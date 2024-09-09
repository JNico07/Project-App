package com.pytorch.project.gazeguard.parentdashboard.childdatafragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.pytorch.demo.objectdetection.R;

import java.util.List;
import java.util.Map;

public class ChildDataFragment extends Fragment {

    private static final String ARG_CHILD_NAME = "child_name";
    private static final String ARG_CHILD_DATA_LIST = "child_data_list";

    public static ChildDataFragment newInstance(String childName, List<Map<String, Object>> childDataList) {
        ChildDataFragment fragment = new ChildDataFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHILD_NAME, childName);
        args.putSerializable(ARG_CHILD_DATA_LIST, (java.io.Serializable) childDataList);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_child_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView childNameTextView = view.findViewById(R.id.childNameTextView);
        LinearLayout recordsContainer = view.findViewById(R.id.recordsContainer);

        if (getArguments() != null) {
            String childName = getArguments().getString(ARG_CHILD_NAME);
            List<Map<String, Object>> childDataList = (List<Map<String, Object>>) getArguments().getSerializable(ARG_CHILD_DATA_LIST);

            childNameTextView.setText(childName);

            if (childDataList != null) {
                // Iterate through each record and add it to the LinearLayout
                for (Map<String, Object> record : childDataList) {
                    View recordView = LayoutInflater.from(getContext()).inflate(R.layout.record_item, recordsContainer, false);

                    TextView dateTextView = recordView.findViewById(R.id.dateTextView);
                    TextView screenTimeTextView = recordView.findViewById(R.id.screenTimeTextView);

                    String date = (String) record.get("date");
                    String screenTime = String.valueOf(record.get("screenTime"));

                    dateTextView.setText("Date: " + date);
                    screenTimeTextView.setText("Screen Time: " + screenTime);

                    recordsContainer.addView(recordView);
                }
            }
        }
    }
}
