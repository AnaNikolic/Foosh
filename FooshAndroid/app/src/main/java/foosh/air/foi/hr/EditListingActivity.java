package foosh.air.foi.hr;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.FileProvider;
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

import java.util.UUID;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import foosh.air.foi.hr.adapters.ImagesRecyclerViewAdapter;
import foosh.air.foi.hr.helper.ImagesRecyclerViewDatasetItem;
import foosh.air.foi.hr.helper.RecyclerItemTouchHelper;
import foosh.air.foi.hr.model.Listing;

public class EditListingActivity extends NavigationDrawerBaseActivity implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    private ConstraintLayout contentLayout;
    private String mListingId;
    private ImagesRecyclerViewDatasetItem imagesRecyclerViewDatasetItem;

    private RecyclerView recyclerView;
    private Toolbar toolbar;
    private ScrollView scrollView;

    private TextInputEditText listingTitle;
    private TextInputEditText listingDescription;
    private TextInputEditText listingPrice;
    private AutoCompleteTextView listingLocation;

    private Button buttonAddNewListing;
    private Button buttonPayingForService;
    private Button buttonIWantToEarn;
    private Spinner categoriesSpinner;

    public static final int id = 2;
    private final int PICK_IMAGES_FOR_LISTING = 1500;
    private final int REQUEST_IMAGE_CAPTURE = 1501;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private final int NUMBER_OF_IMAGES = 10;

    private AppCompatImageView appCompatImageViewLibrary;
    private AppCompatImageView appCompatImageViewCamera;

    private TextView textViewUploadImages;
    private LinearLayout linearLayout;
    private List<ProgressBar> progressBars;

    private DatabaseReference mDatabase;
    private DatabaseReference mDatabaseCategorys;
    private DatabaseReference mDatabaseCities;
    private DatabaseReference mListingReference;

    private ImagesRecyclerViewAdapter imagesRecyclerViewAdapter;

    private Listing listing;

    private HashMap<String, Long> categories;
    private List<String> cities;
    private List<UploadTask> uploadTask;

    private String mCurrentPhotoPath;
    private int finished;
    private int uploadNum;

    {
        listing = new Listing();
        mDatabaseCategorys = FirebaseDatabase.getInstance().getReference().child("categorys");
        mDatabaseCities = FirebaseDatabase.getInstance().getReference().child("cities");
        uploadTask = new ArrayList<>();
        progressBars = new ArrayList<>();
        categories = new HashMap<>();
        cities = new ArrayList<>();
    }

    private void setUpProgress(int progressBarNumber) {
        linearLayout.setVisibility(View.VISIBLE);
        linearLayout.setWeightSum((float) progressBarNumber);

        for (int i = 0; i < progressBarNumber; i++) {
            progressBars.get(i).setVisibility(View.VISIBLE);
            progressBars.get(i).setProgress(0);
            progressBars.get(i).setProgressTintList(ColorStateList.valueOf(Color.GREEN));
        }

        textViewUploadImages.setText(R.string.toast_uploading);
        textViewUploadImages.setVisibility(View.VISIBLE);
    }

    public static String getMenuTitle(){
        return "Uredi oglas";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contentLayout = findViewById(R.id.main_layout);
        getLayoutInflater().inflate(R.layout.activity_listing_new, contentLayout);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mListingId = getIntent().getExtras().getString("listingId");
        cities = new ArrayList<>();

        init();

        buttonPayingForService.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                buttonPayingForService.setBackgroundColor(Color.rgb(114, 79, 175));
                buttonIWantToEarn.setBackgroundColor(Color.rgb(132, 146, 166));
                listing.setHiring(true);
                listing.setStatus("OBJAVLJEN");
            }
        });

        buttonIWantToEarn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                buttonIWantToEarn.setBackgroundColor(Color.rgb(114, 79, 175));
                buttonPayingForService.setBackgroundColor(Color.rgb(132, 146, 166));
                listing.setHiring(false);
                listing.setStatus("OBJAVLJEN");
            }
        });

        mDatabaseCategorys.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    categories.put(item.getKey(), (long) item.getValue());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(EditListingActivity.this,
                        android.R.layout.simple_spinner_item, new ArrayList<>(categories.keySet()));
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categoriesSpinner.setAdapter(adapter);
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

        mDatabaseCities.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    cities.add(item.getKey().toString());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(EditListingActivity.this,
                        android.R.layout.simple_list_item_1, cities);
                listingLocation.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        appCompatImageViewLibrary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!canAddListingImageBefore()) {
                    Toast.makeText(EditListingActivity.this, "No more than " + NUMBER_OF_IMAGES +
                            " images can be added!", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_FOR_LISTING);
            }
        });
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            appCompatImageViewCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!canAddListingImageBefore()) {
                        Toast.makeText(EditListingActivity.this, "No more than " + NUMBER_OF_IMAGES +
                                " images can be added!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                        } catch (IOException ex) {
                            Toast.makeText(EditListingActivity.this, R.string.toast_cant_save_img, Toast.LENGTH_LONG).show();
                        }
                        if (photoFile != null) {
                            Uri photoURI = FileProvider.getUriForFile(EditListingActivity.this,
                                    "foosh.air.foi.hr.fileprovider",
                                    photoFile);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                        }
                    }
                }
            });
            if (checkSelfPermission(android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA},
                        MY_CAMERA_REQUEST_CODE);
            }
        } else {
            appCompatImageViewCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(EditListingActivity.this, R.string.toast_camera_not_found, Toast.LENGTH_LONG).show();
                }
            });

        }

        mListingReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                listing = dataSnapshot.getValue(Listing.class);
                showListingDetailData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }

        });

        buttonAddNewListing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listingTitle.getText().length() == 0
                        || listingDescription.getText().length() == 0
                        || listingPrice.getText().length() == 0
                        || listingLocation.getText().length() == 0
                        || imagesRecyclerViewAdapter.getmDataset().size() == 0) {
                    Toast.makeText(EditListingActivity.this, R.string.toast_not_all_fields_populated, Toast.LENGTH_LONG).show();
                } else {
                    fillListingPartial();
                    while (imagesRecyclerViewAdapter.getmDeleted().size() != 0) {
                        ImagesRecyclerViewDatasetItem item = imagesRecyclerViewAdapter.getmDeleted().pop();
                        if (item.isInDatabase()) {
                            listing.getImages().remove(item.getImageUri().toString());
                            StorageReference photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(item.getImageUri().toString());
                            photoRef.delete();
                        }
                    }
                    countUpload();
                    if (uploadNum > 0) {
                        setUpProgress(uploadNum);
                        finished = 0;
                        for (int i = 0; i < imagesRecyclerViewAdapter.getmDataset().size(); i++) {
                            imagesRecyclerViewDatasetItem = imagesRecyclerViewAdapter.getmDataset().get(i);
                            if (!imagesRecyclerViewDatasetItem.isInDatabase()) {
                                String  uniqueID = UUID.randomUUID().toString().replace("-", "");;
                                String imageName = mListingId + "_image_" + uniqueID;
                                final StorageReference listingImageRef = FirebaseStorage.getInstance().getReference()
                                        .child("listings/" + listing.getOwnerId() + "/" + mListingId + "/" + imageName);
                                StorageMetadata metadata = new StorageMetadata.Builder()
                                        .setContentType("image/jpeg")
                                        .build();
                                Uri imageUri = imagesRecyclerViewAdapter.getmDataset().get(i).getImageUri();
                                uploadTask.add(listingImageRef.putFile(imageUri, metadata));
                                final int j = i;
                                uploadTask.get(uploadTask.size() - 1).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                    }
                                }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                                        System.out.println(R.string.toast_upload_paused);
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
                        }
                    }
                    else { createFirebaseListing(); }
                }
            }
        });


    }

    private void checkNewListing() {
        if (uploadTask.size() != uploadNum) {
            return;
        }
        for (UploadTask task : uploadTask) {
            if (task.isInProgress() || task.isCanceled()) {
                return;
            }
        }
        if (uploadTask.size() != finished) {
            return;
        }
        createFirebaseListing();
    }

    public void createFirebaseListing() {
        mDatabase.child("listings").child(mListingId).setValue(listing).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(EditListingActivity.this, R.string.toast_listing_succesfully_updated, Toast.LENGTH_LONG).show();
            }
        });
        finish();
    }

    private void countUpload() {
        int count = 0;
        for (ImagesRecyclerViewDatasetItem item : imagesRecyclerViewAdapter.getmDataset()) {
            if (!item.isInDatabase()) {
                count++;
            }
        }
        uploadNum = count;
    }

    private void init() {
        toolbar = findViewById(R.id.id_toolbar_main);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);

        categoriesSpinner = contentLayout.findViewById(R.id.spinner_categories);


        mListingReference = FirebaseDatabase.getInstance().getReference().child("listings").child(mListingId);

        scrollView = findViewById(R.id.fragment_listing_add);
        listingTitle = findViewById(R.id.listingTitle);
        listingDescription = findViewById(R.id.ListingDescription);
        listingPrice = findViewById(R.id.ListingPrice);
        listingLocation = findViewById(R.id.country_list);

        buttonAddNewListing = contentLayout.findViewById(R.id.buttonAddListing);
        buttonPayingForService = contentLayout.findViewById(R.id.buttonPaying);
        buttonIWantToEarn = contentLayout.findViewById(R.id.buttonEarning);

        appCompatImageViewLibrary = contentLayout.findViewById(R.id.appCompatImageViewLibrary);
        appCompatImageViewCamera = contentLayout.findViewById(R.id.appCompatImageViewCamera);

        textViewUploadImages = contentLayout.findViewById(R.id.textUploadImage);
        linearLayout = contentLayout.findViewById(R.id.imageProgresBarLinearLayout);
        for (int i = 0; i < NUMBER_OF_IMAGES; i++) {
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

    private void showListingDetailData() {
        listingTitle.setText(listing.getTitle());
        listingDescription.setText(listing.getDescription());
        listingPrice.setText(String.valueOf(listing.getPrice()));
        listingLocation.setText(listing.getLocation());
        if (listing.isHiring()) {
            buttonPayingForService.setBackgroundColor(Color.rgb(114, 79, 175));
            buttonIWantToEarn.setBackgroundColor(Color.rgb(132, 146, 166));
        } else {
            buttonIWantToEarn.setBackgroundColor(Color.rgb(114, 79, 175));
            buttonPayingForService.setBackgroundColor(Color.rgb(132, 146, 166));
        }
        categoriesSpinner.setSelection(((ArrayAdapter<String>) categoriesSpinner.getAdapter()).getPosition(listing.getCategory()));
        buttonAddNewListing.setText(R.string.save_changes);
        ArrayList<String> listOfCurrentImages = listing.getImages();
        List<ImagesRecyclerViewDatasetItem> imagesList = new ArrayList<>();
        if (listOfCurrentImages != null) {
            for (String imagePath : listOfCurrentImages) {
                imagesRecyclerViewDatasetItem = new ImagesRecyclerViewDatasetItem(Uri.parse(imagePath));
                imagesRecyclerViewDatasetItem.setInDatabase(true);
                imagesList.add(imagesRecyclerViewDatasetItem);
            }
            imagesRecyclerViewAdapter.addImagesToDataset(imagesList);
        }
    }

    private boolean canAddListingImageBefore() {
        return uploadNum < NUMBER_OF_IMAGES;
    }

    private boolean canAddListingImageAfter(int plusNumberOfImages) {
        return uploadNum + plusNumberOfImages <= NUMBER_OF_IMAGES;
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_FOR_LISTING && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                if (data.getData() != null) {
                    if (!canAddListingImageAfter(1)) {
                        Toast.makeText(EditListingActivity.this, "No more than " + NUMBER_OF_IMAGES +
                                " images can be added!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    imagesRecyclerViewDatasetItem = new ImagesRecyclerViewDatasetItem(data.getData());
                    imagesRecyclerViewAdapter.addImageToDataset(imagesRecyclerViewDatasetItem);
                } else {
                    List<ImagesRecyclerViewDatasetItem> imagesList = new ArrayList<>();
                    ClipData mClipData = data.getClipData();
                    if (!canAddListingImageAfter(mClipData.getItemCount())) {
                        Toast.makeText(EditListingActivity.this, "No more than " + NUMBER_OF_IMAGES +
                                " images can be added!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    for (int i = 0; i < mClipData.getItemCount(); i++) {
                        imagesRecyclerViewDatasetItem = new ImagesRecyclerViewDatasetItem(mClipData.getItemAt(i).getUri());
                        imagesList.add(imagesRecyclerViewDatasetItem);
                    }
                    imagesRecyclerViewAdapter.addImagesToDataset(imagesList);
                }
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            if (!canAddListingImageAfter(1)) {
                Toast.makeText(EditListingActivity.this, "No more than " + NUMBER_OF_IMAGES +
                        " images can be added!", Toast.LENGTH_LONG).show();
                return;
            }
            imagesRecyclerViewDatasetItem = new ImagesRecyclerViewDatasetItem(Uri.fromFile(new File(mCurrentPhotoPath)));
            imagesRecyclerViewAdapter.addImageToDataset(imagesRecyclerViewDatasetItem);
        }
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof ImagesRecyclerViewAdapter.MyViewHolder) {
            String name = "Undo removed image";
            final Object deletedItem = imagesRecyclerViewAdapter.getmDataset().get(viewHolder.getAdapterPosition());
            final int deletedIndex = viewHolder.getAdapterPosition();

            imagesRecyclerViewAdapter.removeItem(deletedIndex);

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
                Toast.makeText(this, R.string.toast_camera_granted, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.toast_camera_denied, Toast.LENGTH_LONG).show();
            }
        }
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
            Toast.makeText(this, R.string.toast_uploading, Toast.LENGTH_LONG).show();
        }
        else{
            super.onBackPressed();
        }
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

    private void fillListingPartial() {
        listing.setTitle(listingTitle.getText().toString());
        listing.setDescription(listingDescription.getText().toString());
        listing.setPrice(Integer.parseInt(listingPrice.getText().toString()));
        listing.setLocation(listingLocation.getText().toString());
        listing.setActive(true);
        listing.setId(mListingId);
    }

}