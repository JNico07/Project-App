package com.pytorch.project.gazeguard.parentdashboard;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pytorch.project.gazeguard.common.FirebaseManager;
import com.pytorch.project.gazeguard.parentdashboard.childdatafragment.ChildDataFragment;
import com.pytorch.project.gazeguard.parentdashboard.optionfragments.HomeFragment;


import org.pytorch.demo.objectdetection.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ParentAdapter extends FirebaseRecyclerAdapter<ParentModel, ParentAdapter.myViewHolder> {

    FirebaseUser currentUser = FirebaseManager.getCurrentUser();
    FirebaseFirestore firestore = FirebaseManager.getFirestore();

    public interface OnItemClickListener {
        void onShowProgressBar();
        void onHideProgressBar();
    }

    private final OnItemClickListener listener;

    public ParentAdapter(@NonNull FirebaseRecyclerOptions<ParentModel> options, OnItemClickListener listener) {
        super(options);
        this.listener = listener;
    }

    @Override
    protected void onBindViewHolder(@NonNull myViewHolder myViewHolder, int i, @NonNull ParentModel parentModel) {
        myViewHolder.name.setText(parentModel.getName());
        myViewHolder.SreenTime.setText(parentModel.getScreenTime());

        Glide.with(myViewHolder.img.getContext())
                .load(parentModel.getSurl())
                .placeholder(R.drawable.person)
                .circleCrop()
                .error(R.drawable.person)
                .into(myViewHolder.img);

        myViewHolder.itemView.setOnClickListener(v -> {

            listener.onShowProgressBar();

            if (currentUser != null) {
                String uid = currentUser.getUid();
                String childUserName = parentModel.getName();

                DatabaseReference childDatabase = FirebaseDatabase.getInstance().getReference()
                        .child("Registered Users").child(uid).child("Child");

                childDatabase.orderByChild("name").equalTo(childUserName).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            String dateCreated = childSnapshot.child("dateCreated").getValue(String.class);

                            assert dateCreated != null;
                            firestore.collection("ScreenTimeRecords")
                                    .document(uid)
                                    .collection(childUserName)
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {

                                        List<Map<String, Object>> childDataList = new ArrayList<>();
                                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                                            childDataList.add(document.getData());
                                        }

                                        // Open Fragment and Pass the child data list
                                        ChildDataFragment fragment = ChildDataFragment.newInstance(childUserName, childDataList);
                                        ((ParentDashboardActivity) myViewHolder.itemView.getContext())
                                                .getSupportFragmentManager()
                                                .beginTransaction()
                                                .replace(R.id.fragment_container, fragment)
                                                .addToBackStack(null)
                                                .commit();

                                        listener.onHideProgressBar();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w("Firestore", "Error querying records", e);
                                        listener.onHideProgressBar();
                                    });
                            break; // Exit loop after finding the data
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w("Firebase", "Error retrieving child data", error.toException());
                        listener.onHideProgressBar();
                    }
                });
            } else {
                Log.w("Firestore", "User is not authenticated");
                listener.onHideProgressBar();
            }
        });
    }







    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.parent_db_item,parent,false);
        return new myViewHolder(view);
    }

    static class myViewHolder extends RecyclerView.ViewHolder {
        CircleImageView img;
        TextView name, SreenTime;

        public myViewHolder(@NonNull View itemView) {
            super(itemView);

            img = (CircleImageView) itemView.findViewById(R.id.img1);
            name = (TextView) itemView.findViewById(R.id.userName);
            SreenTime = (TextView) itemView.findViewById(R.id.timerTextPDash);
        }
    }

}

