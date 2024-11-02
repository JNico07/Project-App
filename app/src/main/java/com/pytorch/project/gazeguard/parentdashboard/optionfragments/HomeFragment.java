package com.pytorch.project.gazeguard.parentdashboard.optionfragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pytorch.project.gazeguard.auth.LoginActivity;
import com.pytorch.project.gazeguard.parentdashboard.ParentAdapter;
import com.pytorch.project.gazeguard.parentdashboard.ParentModel;

import org.pytorch.demo.objectdetection.R;

public class HomeFragment extends Fragment implements ParentAdapter.OnItemClickListener {

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private TextView parentNameTextView;
    private RecyclerView recyclerView;
    private ParentAdapter parentAdapter;
    private String uid;

    private TextView textView;
    private FirebaseUser user;
    private DatabaseReference parentNameRef;
    private String parentName;
    public ProgressBar loadingProgressBarHome;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentHomeView = inflater.inflate(R.layout.fragment_home, container, false);

        parentNameTextView = fragmentHomeView.findViewById(R.id.parentName);
        recyclerView = fragmentHomeView.findViewById(R.id.rvHome);
        loadingProgressBarHome = fragmentHomeView.findViewById(R.id.progressBar_home);

        loadingProgressBarHome.setVisibility(View.GONE);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(getContext(), LoginActivity.class);
            startActivity(intent);
        } else {
            getParentName(new ParentNameCallback() {
                @Override
                public void onCallback(String parentName) {
                    parentNameTextView.setText(parentName);
                }
            });
        }

        uid = currentUser.getUid();
        FirebaseRecyclerOptions<ParentModel> options =
                new FirebaseRecyclerOptions.Builder<ParentModel>()
                        .setQuery(FirebaseDatabase.getInstance().getReference("Registered Users").child(uid).child("Child")
                                , ParentModel.class)
                        .build();
        parentAdapter = new ParentAdapter(options, this);
        recyclerView.setAdapter(parentAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return fragmentHomeView;
    }

    public void getParentName(final ParentNameCallback callback) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        parentNameRef = FirebaseDatabase.getInstance().getReference()
                .child("Registered Users").child(uid).child("Parent").child("full_name");

        parentNameRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String parentName = dataSnapshot.getValue(String.class);
                    callback.onCallback(parentName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible errors.
            }
        });
    }

    @Override
    public void onShowProgressBar() {
        loadingProgressBarHome.setVisibility(View.VISIBLE);
    }
    @Override
    public void onHideProgressBar() {
        loadingProgressBarHome.setVisibility(View.GONE);
    }

    public interface ParentNameCallback {
        void onCallback(String parentName);
    }

    @Override
    public void onStart() {
        super.onStart();
        parentAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        parentAdapter.stopListening();
    }

}