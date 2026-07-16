package com.example.sporthubandroidmembershipapplicationv1;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TrainingDetailFragment extends Fragment {

    public TrainingDetailFragment() {
        super(R.layout.fragment_training_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView btnBackTraining = view.findViewById(R.id.btnBackTraining);
        TextView txtTrainingTitle = view.findViewById(R.id.txtTrainingTitle);

        TextView txtHeader1 = view.findViewById(R.id.txtHeader1);
        TextView txtDesc1 = view.findViewById(R.id.txtDesc1);

        TextView txtHeader2 = view.findViewById(R.id.txtHeader2);
        TextView txtDesc2 = view.findViewById(R.id.txtDesc2);

        TextView txtHeader3 = view.findViewById(R.id.txtHeader3);
        TextView txtDesc3 = view.findViewById(R.id.txtDesc3);

        Bundle args = getArguments();
        if (args != null) {
            txtTrainingTitle.setText(args.getString("title", "Training"));

            String detail1 = args.getString("detail1", "");
            String detail2 = args.getString("detail2", "");
            String detail3 = args.getString("detail3", "");

            String[] parts1 = splitDetail(detail1);
            String[] parts2 = splitDetail(detail2);
            String[] parts3 = splitDetail(detail3);

            txtHeader1.setText(parts1[0]);
            txtDesc1.setText(parts1[1]);

            txtHeader2.setText(parts2[0]);
            txtDesc2.setText(parts2[1]);

            txtHeader3.setText(parts3[0]);
            txtDesc3.setText(parts3[1]);
        }

        btnBackTraining.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });
    }

    private String[] splitDetail(String detail) {
        String[] result = new String[2];
        if (detail != null && detail.contains(":\n")) {
            String[] parts = detail.split(":\\n", 2);
            result[0] = parts[0] + ":";
            result[1] = parts[1];
        } else {
            result[0] = detail;
            result[1] = "";
        }
        return result;
    }
}