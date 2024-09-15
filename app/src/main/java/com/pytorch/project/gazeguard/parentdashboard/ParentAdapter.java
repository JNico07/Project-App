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

    public ParentAdapter(@NonNull FirebaseRecyclerOptions<ParentModel> options) {
        super(options);
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
            // Handle item click event here
            Toast.makeText(myViewHolder.itemView.getContext(), parentModel.getName(), Toast.LENGTH_SHORT).show();

            if (currentUser != null) {
                String uid = currentUser.getUid();
                String childUserName = parentModel.getName();

                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                DatabaseReference childDatabase = FirebaseDatabase.getInstance().getReference()
                        .child("Registered Users").child(uid).child("Child");

                // Query the child nodes to find the specific child node by name without explicitly specifying the child number
                childDatabase.orderByChild("name").equalTo(childUserName).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            String dateCreated = childSnapshot.child("dateCreated").getValue(String.class);

                            assert dateCreated != null;
                            firestore.collection("ScreenTimeRecords")
                                    .document(uid)
                                    .collection(childUserName)
                                    .whereGreaterThanOrEqualTo("date", dateCreated) // read data from Date Created
                                    .whereLessThanOrEqualTo("date", "2025-01-01")   // to Current Date
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {

                                        List<Map<String, Object>> childDataList = new ArrayList<>();

                                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                                            Log.d("Firestore", "Record: " + document.getData());
                                            Log.d("Firestore", "Date Started: " + dateCreated);

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
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w("Firestore", "Error querying records", e);
                                    });

                            break; // Assuming childUserName is unique, exit loop after finding the child
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w("Firebase", "Error retrieving child data", error.toException());
                    }
                });
            } else {
                Log.w("Firestore", "User is not authenticated");
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

