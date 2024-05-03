package org.tensorflow.lite.examples.detection.parentDB;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import org.tensorflow.lite.examples.detection.Login;
import org.tensorflow.lite.examples.detection.R;
import org.tensorflow.lite.examples.detection.Welcome;

public class HomeFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private TextView textView;
    private RecyclerView recyclerView;
    ParentAdapter parentAdapter;
    private String uid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        textView = view.findViewById(R.id.user_details);
        recyclerView = view.findViewById(R.id.rv);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(getContext(), Login.class);
            startActivity(intent);
        } else {
            textView.setText(currentUser.getEmail());
        }

        uid = currentUser.getUid();
        FirebaseRecyclerOptions<ParentModel> options =
                new FirebaseRecyclerOptions.Builder<ParentModel>()
                        .setQuery(FirebaseDatabase.getInstance().getReference("Registered Users").child(uid).child("Child")
                                , ParentModel.class)
                        .build();
        parentAdapter = new ParentAdapter(options);
        recyclerView.setAdapter(parentAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        return view;
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
