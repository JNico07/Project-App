package com.pytorch.project.gazeguard.parentdashboard.optionfragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.widget.TooltipCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.pytorch.project.gazeguard.auth.LoginActivity;
import com.pytorch.project.gazeguard.parentdashboard.ParentAdapter;
import com.pytorch.project.gazeguard.parentdashboard.ParentModel;
import com.pytorch.project.gazeguard.parentdashboard.SetLimitAdapter;

import org.pytorch.demo.objectdetection.R;

public class SetLimitFragment extends Fragment {

    private TextView parentNameTextView;
    private RecyclerView recyclerView;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private SetLimitAdapter setLimitAdapter;
    private String uid;

    HomeFragment homeFragment = new HomeFragment();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentLimitAlertsView = inflater.inflate(R.layout.fragment_set_limit, container, false);

        parentNameTextView = fragmentLimitAlertsView.findViewById(R.id.parentName);
        recyclerView = fragmentLimitAlertsView.findViewById(R.id.rvSetLimit);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(getContext(), LoginActivity.class);
            startActivity(intent);
        } else {
            homeFragment.getParentName(new HomeFragment.ParentNameCallback() {
                @Override
                public void onCallback(String parentName) {
                    parentNameTextView.setText(parentName);
                }
            });
        }

        uid = currentUser.getUid();
        FirebaseRecyclerOptions<ParentModel> options =
                new FirebaseRecyclerOptions.Builder<ParentModel>()
                        .setQuery(FirebaseDatabase.getInstance()
                                        .getReference("Registered Users")
                                        .child(uid)
                                        .child("Child")
                                , ParentModel.class)
                        .build();
        setLimitAdapter = new SetLimitAdapter(options, uid);
        recyclerView.setAdapter(setLimitAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        return fragmentLimitAlertsView;
    }


    @Override
    public void onStart() {
        super.onStart();
        setLimitAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        setLimitAdapter.stopListening();
    }

}