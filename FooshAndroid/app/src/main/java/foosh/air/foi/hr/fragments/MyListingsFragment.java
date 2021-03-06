package foosh.air.foi.hr.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import foosh.air.foi.hr.LoadCompletedListener;
import foosh.air.foi.hr.LoadMoreListener;
import foosh.air.foi.hr.R;
import foosh.air.foi.hr.adapters.MyListingsEndlessRecyclerViewAdapter;
import foosh.air.foi.hr.model.Listing;

public class MyListingsFragment extends Fragment{

    public interface onFragmentInteractionListener{
        void onFragmentInteraction(Fragment fragment);
    }

    private static final String KEY_PREFIX = "foosh.air.foi.hr.MyListingsFragment.";
    private static final String ARG_TYPE_KEY = KEY_PREFIX + "type-key";

    private MyListingsEndlessRecyclerViewAdapter myListingsEndlessRecyclerViewAdapter;
    private onFragmentInteractionListener mListener;
    private String mType;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LoadMoreListener loadMoreListener = new LoadMoreListener() {
        @Override
        public void loadMore(Listing id, int mPostsPerPage, final LoadCompletedListener loadCompletedListener) {
            Query query;
            final String key = id != null ? id.getId() : null;
            if (key == null)
                query = FirebaseDatabase.getInstance().getReference()
                        .child("listings")
                        .orderByKey()
                        .limitToLast(mPostsPerPage);
            else
                query = FirebaseDatabase.getInstance().getReference()
                        .child("listings")
                        .orderByKey()
                        .endAt(key)
                        .limitToLast(mPostsPerPage);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ArrayList<Listing> listings = new ArrayList<>();
                    if (dataSnapshot.exists()){
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            Log.d("key-id", userSnapshot.getValue(Listing.class).getId());
                            if (!userSnapshot.getValue(Listing.class).getId().equals(key))
                                listings.add(0, userSnapshot.getValue(Listing.class));
                        }
                        loadCompletedListener.onLoadCompleted(listings);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    loadCompletedListener.onLoadCompleted(new ArrayList<foosh.air.foi.hr.model.Listing>());
                }
            });
        }
    };

    public MyListingsFragment() {
        // Required empty public constructor
    }

    public static MyListingsFragment getInstance(String type) {
        MyListingsFragment fragment = new MyListingsFragment();

        Bundle bundle = new Bundle();
        bundle.putString(ARG_TYPE_KEY, type);
        fragment.setArguments(bundle);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mType = getArguments().getString(ARG_TYPE_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.fragment_my_listings, container, false);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeColors(Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN);
        recyclerView = view.findViewById(R.id.id_recycle_view);
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) new LinearLayoutManager(getContext());
        linearLayoutManager.setSmoothScrollbarEnabled(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        if (mType.equals("OBJAVLJENI")){
            myListingsEndlessRecyclerViewAdapter = new MyListingsEndlessRecyclerViewAdapter(getContext(), recyclerView,
                    swipeRefreshLayout, 10, loadMoreListener);
        }
        else if (mType.equals("PRIJAVLJENI")){
            myListingsEndlessRecyclerViewAdapter = new MyListingsEndlessRecyclerViewAdapter(getContext(), recyclerView,
                    swipeRefreshLayout, 10, loadMoreListener);
        }
        else {
            myListingsEndlessRecyclerViewAdapter = new MyListingsEndlessRecyclerViewAdapter(getContext(), recyclerView,
                    swipeRefreshLayout, 10, loadMoreListener);
        }
        recyclerView.setAdapter(myListingsEndlessRecyclerViewAdapter);
        return swipeRefreshLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof onFragmentInteractionListener) {
            mListener = (onFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public String getType() {
        return mType;
    }
}
