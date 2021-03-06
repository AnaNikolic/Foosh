package foosh.air.foi.hr;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import foosh.air.foi.hr.adapters.ImagesRecyclerViewAdapter;
import foosh.air.foi.hr.helper.RecyclerItemTouchHelper;
import foosh.air.foi.hr.model.Listing;

public class NewListingActivity extends NavigationDrawerBaseActivity implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    private ConstraintLayout contentLayout;

    //used in the NavigationDrawerBaseActivity for the menu item id
    public static final int id = 2;
    private final int PICK_IMAGES_FOR_LISTING = 1500;
    private final int REQUEST_IMAGE_CAPTURE = 1501;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private final int NUMBER_OF_IMAGES = 10;
    private final int MenuItem_FilterAds = 0, MenuItem_ExpandOpt = 1;
    private RecyclerView recyclerView;
    private Toolbar toolbar;
    private ScrollView scrollView;

    private TextInputEditText listingTitle;
    private TextInputEditText listingDescription;
    private TextInputEditText listingPrice;
    private AutoCompleteTextView autoCompleteTextView;

    private Button buttonAddNewListing;
    private Button buttonPayingForService;
    private Button buttonIWantToEarn;
    private Spinner categoriesSpinner;

    private AppCompatImageView appCompatImageViewLibrary;
    private AppCompatImageView appCompatImageViewCamera;

    private TextView textViewUploadImages;
    private LinearLayout linearLayout;
    private List<ProgressBar> progressBars;

    private DatabaseReference mDatabaseListings;
    private DatabaseReference mDatabaseCategorys;
    private DatabaseReference mDatabaseCities;
    private FirebaseAuth mAuth;
    private String mUserId;

    private ImagesRecyclerViewAdapter imagesRecyclerViewAdapter;

    private Listing listing;
    private HashMap<String, Long> categories;
    private List<String> cities;

    private List<UploadTask> uploadTask;
    private int finished;

    {
        listing = new Listing();
        mDatabaseListings = FirebaseDatabase.getInstance().getReference().child("listings");
        mDatabaseCategorys = FirebaseDatabase.getInstance().getReference().child("categorys");
        mDatabaseCities = FirebaseDatabase.getInstance().getReference().child("cities");
        uploadTask = new ArrayList<>();
        progressBars = new ArrayList<>();
        categories = new HashMap<>();
        cities = new ArrayList<>();
    }

    public static String getMenuTitle(){
        return "Dodaj oglas";
    }

    private void fillListingPartial(){
        listing.setTitle(listingTitle.getText().toString());
        listing.setDescription(listingDescription.getText().toString());
        listing.setPrice(Integer.parseInt(listingPrice.getText().toString()));

        listing.setOwnerId(FirebaseAuth.getInstance().getUid());
        listing.setStatus("OBJAVLJEN");
        listing.setActive(true);

        String key = mDatabaseListings.push().getKey();
        listing.setId(key);
    }

    private void setUpProgress(int progressBarNumber){
        linearLayout.setVisibility(View.VISIBLE);
        linearLayout.setWeightSum((float)progressBarNumber);

        for (int i = 0; i < progressBarNumber; i++){
            progressBars.get(i).setVisibility(View.VISIBLE);
            progressBars.get(i).setProgress(0);
            progressBars.get(i).setProgressTintList(ColorStateList.valueOf(Color.GREEN));
        }

        textViewUploadImages.setText("Uploading images...");
        textViewUploadImages.setVisibility(View.VISIBLE);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contentLayout = findViewById(R.id.main_layout);
        getLayoutInflater().inflate(R.layout.activity_listing_new, contentLayout);

        init();

        buttonAddNewListing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listingTitle.getText().length()==0
                        || listingDescription.getText().length()==0
                        || listingPrice.getText().length()==0
                        || autoCompleteTextView.getText().length()==0) {
                    Toast.makeText(NewListingActivity.this, "Not all fileds have been populated!", Toast.LENGTH_LONG).show();
                }
                else {
                    fillListingPartial();
                    setUpProgress(imagesRecyclerViewAdapter.getmDataset().size());
                    finished = 0;
                    for (int i = 0; i < imagesRecyclerViewAdapter.getmDataset().size(); i++){
                        String imageName = listing.getId() + "_image_" + (i + 1);
                        final StorageReference listingImageRef = FirebaseStorage.getInstance().getReference()
                            .child("listings/" + listing.getOwnerId() + "/" + listing.getId() + "/" + imageName);
                        StorageMetadata metadata = new StorageMetadata.Builder()
                                .setContentType("image/jpeg")
                                .build();
                        if (imagesRecyclerViewAdapter.getmDataset().get(i) instanceof Uri){
                            Uri imageUri = (Uri) imagesRecyclerViewAdapter.getmDataset().get(i);
                            uploadTask.add(listingImageRef.putFile(imageUri, metadata));
                            final int j = i;
                            uploadTask.get(uploadTask.size() - 1).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                                }
                            }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                                    System.out.println("Upload is paused");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    progressBars.get(j).setProgressTintList(ColorStateList.valueOf(Color.RED));
                                    progressBars.get(j).setProgress(100);
                                }
                            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    progressBars.get(j).setProgress(100);
                                    listingImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            listing.getImages().add(uri.toString());
                                            finished++;
                                            checkNewListing();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                        }
                                    });
                                }
                            });
                        }
                        else if (imagesRecyclerViewAdapter.getmDataset().get(i) instanceof Bitmap){
                            Bitmap imageBitmap = (Bitmap) imagesRecyclerViewAdapter.getmDataset().get(i);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                            byte[] data = baos.toByteArray();
                            final int j = i;
                            uploadTask.add(listingImageRef.putBytes(data, metadata));
                            uploadTask.get(uploadTask.size() - 1).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                                }
                            }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                                    System.out.println("Upload is paused");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    progressBars.get(j).setIndeterminateTintList(ColorStateList.valueOf(Color.RED));
                                    progressBars.get(j).setProgress(100);
                                }
                            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    progressBars.get(j).setProgress(100);
                                    listingImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            listing.getImages().add(uri.toString());
                                            finished++;
                                            checkNewListing();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                        }
                                    });
                                }
                            });
                        }
                    }
                }
            }
        });

       buttonPayingForService.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                buttonPayingForService.setBackgroundColor(Color.rgb(114, 79, 175));
                buttonIWantToEarn.setBackgroundColor(Color.rgb(132, 146, 166));
                listing.setHiring(false);
                listing.setStatus("KREIRAN");
            }
        });

        buttonIWantToEarn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                buttonIWantToEarn.setBackgroundColor(Color.rgb(114, 79, 175));
                buttonPayingForService.setBackgroundColor(Color.rgb(132, 146, 166));
                listing.setHiring(true);
                listing.setStatus("OBJAVLJEN");
            }
        });

        appCompatImageViewLibrary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!canAddListingImageBefore()){
                    Toast.makeText(NewListingActivity.this, "No more than " + NUMBER_OF_IMAGES +
                            " images can be added!", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Images"), PICK_IMAGES_FOR_LISTING);
            }
        });
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            appCompatImageViewCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!canAddListingImageBefore()){
                        Toast.makeText(NewListingActivity.this, "No more than " + NUMBER_OF_IMAGES +
                                " images can be added!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
            });
            if (checkSelfPermission(android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA},
                        MY_CAMERA_REQUEST_CODE);
            }
        }
        else{
            appCompatImageViewCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(NewListingActivity.this, "Camera not found!", Toast.LENGTH_LONG).show();
                }
            });
        }

        mDatabaseCategorys.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot item: dataSnapshot.getChildren()) {
                    categories.put(item.getKey().toString(), (long)item.getValue());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(NewListingActivity.this,
                        android.R.layout.simple_spinner_item, new ArrayList<>(categories.keySet()));
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categoriesSpinner.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mDatabaseCities.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot item: dataSnapshot.getChildren()){
                    cities.add(item.getKey().toString());
                }
                ArrayAdapter<String> adapter= new ArrayAdapter<>(NewListingActivity.this,
                        android.R.layout.simple_list_item_1, cities);
                autoCompleteTextView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        categoriesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String itemValue = adapterView.getItemAtPosition(i).toString();
                listing.setCategory(itemValue);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void checkNewListing(){
        if (uploadTask.size() != imagesRecyclerViewAdapter.getmDataset().size()){
            return;
        }
        for (UploadTask task: uploadTask) {
            if (task.isInProgress() || task.isCanceled()){
                return;
            }
        }
        if (uploadTask.size() != finished){
            return;
        }

        createFirebaseListing(listing);
        Toast.makeText(NewListingActivity.this, "New listing has been successfully added!", Toast.LENGTH_LONG).show();
        finish();
    }

    private void updateCategorys() {
        mDatabaseCategorys.child(listing.getCategory()).setValue(categories.get(listing.getCategory()) + 1);
    }

    private void init() {
        toolbar = findViewById(R.id.id_toolbar_main);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);

        scrollView = findViewById(R.id.fragment_listing_add);
        listingTitle = findViewById(R.id.listingTitle);
        listingDescription = findViewById(R.id.ListingDescription);
        listingPrice = findViewById(R.id.ListingPrice);
        autoCompleteTextView = findViewById(R.id.country_list);

        buttonAddNewListing = contentLayout.findViewById(R.id.buttonAddListing);
        buttonPayingForService = contentLayout.findViewById(R.id.buttonPaying);
        buttonIWantToEarn = contentLayout.findViewById(R.id.buttonEarning);

        categoriesSpinner = contentLayout.findViewById(R.id.spinner_categories);

        appCompatImageViewLibrary = contentLayout.findViewById(R.id.appCompatImageViewLibrary);
        appCompatImageViewCamera = contentLayout.findViewById(R.id.appCompatImageViewCamera);

        textViewUploadImages = contentLayout.findViewById(R.id.textUploadImage);
        linearLayout = contentLayout.findViewById(R.id.imageProgresBarLinearLayout);
        for (int i = 0; i < NUMBER_OF_IMAGES; i++){
            progressBars.add((ProgressBar) contentLayout.findViewById(getResources()
                    .getIdentifier("imageUploadProgressBar" + (i + 1), "id", getPackageName())));
        }

        imagesRecyclerViewAdapter = new ImagesRecyclerViewAdapter(this);

        recyclerView = contentLayout.findViewById(R.id.id_recycle_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL));
        recyclerView.setAdapter(imagesRecyclerViewAdapter);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.DOWN, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);

        recyclerView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        scrollView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                recyclerView.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
    }

    public void createFirebaseListing(Listing listing){
        mDatabaseListings.child(listing.getId()).setValue(listing).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                updateCategorys();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;

            default:
                return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_FOR_LISTING && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                if (data.getData() != null){
                    if (!canAddListingImageAfter(1)){
                        Toast.makeText(NewListingActivity.this, "No more than " + NUMBER_OF_IMAGES +
                                " images can be added!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    imagesRecyclerViewAdapter.addImageToDataset(data.getData());
                }
                else{
                    List<Object> imagesList = new ArrayList<>();
                    ClipData mClipData = data.getClipData();
                    if (!canAddListingImageAfter(mClipData.getItemCount())){
                        Toast.makeText(NewListingActivity.this, "No more than " + NUMBER_OF_IMAGES +
                                " images can be added!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    for (int i=0;i<mClipData.getItemCount();i++){
                        imagesList.add(mClipData.getItemAt(i).getUri());
                    }
                    imagesRecyclerViewAdapter.addImagesToDataset(imagesList);
                }
            }
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK){
            if (data != null && data.getExtras() != null) {
                if (!canAddListingImageAfter(1)){
                    Toast.makeText(NewListingActivity.this, "No more than " + NUMBER_OF_IMAGES +
                            " images can be added!", Toast.LENGTH_LONG).show();
                    return;
                }
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                imagesRecyclerViewAdapter.addImageToDataset(imageBitmap);
            }
        }
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof ImagesRecyclerViewAdapter.MyViewHolder) {
            String name = "Undo removed image";

            final Object deletedItem = imagesRecyclerViewAdapter.getmDataset().get(viewHolder.getAdapterPosition());
            final int deletedIndex = viewHolder.getAdapterPosition();

            imagesRecyclerViewAdapter.removeItem(viewHolder.getAdapterPosition());

            Snackbar snackbar = Snackbar
                    .make(contentLayout, name, Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    imagesRecyclerViewAdapter.restoreItem(deletedItem, deletedIndex);
                    recyclerView.getLayoutManager().scrollToPosition(deletedIndex);
                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Camera permission denied!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean canAddListingImageBefore(){
        return imagesRecyclerViewAdapter.getmDataset().size() < NUMBER_OF_IMAGES;
    }

    private boolean canAddListingImageAfter(int plusNumberOfImages){
        return imagesRecyclerViewAdapter.getmDataset().size() + plusNumberOfImages <= NUMBER_OF_IMAGES;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (uploadTask.size() > 0){
            for (UploadTask task: uploadTask) {
                if (task.isInProgress()){
                    task.pause();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (uploadTask.size() > 0){
            for (UploadTask task: uploadTask) {
                if (task.isPaused()){
                    task.resume();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (uploadTask.size() > 0){
            Toast.makeText(this, "Uploading images... Please wait!", Toast.LENGTH_LONG).show();
        }
        else{
            super.onBackPressed();
        }
    }
}