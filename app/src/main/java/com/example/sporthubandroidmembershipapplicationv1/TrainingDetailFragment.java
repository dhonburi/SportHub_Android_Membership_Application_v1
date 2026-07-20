package com.example.sporthubandroidmembershipapplicationv1;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TrainingDetailFragment extends Fragment {

    private static final String ARG_SPORT_NAME = "sport_name";

    private String sportName = "Basketball";

    public TrainingDetailFragment() {
        super(R.layout.fragment_training_detail);
    }

    public static TrainingDetailFragment newInstance(String sportName) {

        TrainingDetailFragment fragment =
                new TrainingDetailFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_SPORT_NAME, sportName);

        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {

            String receivedSportName =
                    getArguments().getString(ARG_SPORT_NAME);

            if (receivedSportName != null
                    && !receivedSportName.isEmpty()) {

                sportName = receivedSportName;
            }
        }
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        TextView btnBackTraining =
                view.findViewById(R.id.btnBackTraining);

        TextView txtTrainingTitle =
                view.findViewById(R.id.txtTrainingTitle);

        TextView txtBeginnerTitle =
                view.findViewById(R.id.txtBeginnerTitle);

        TextView txtIntermediateTitle =
                view.findViewById(R.id.txtIntermediateTitle);

        TextView txtSeniorTitle =
                view.findViewById(R.id.txtSeniorTitle);

        Button btnRegisterNow =
                view.findViewById(R.id.btnRegisterNow);

        // Changes the page title based on the selected sport
        txtTrainingTitle.setText(
                sportName + " Trainings"
        );

        txtBeginnerTitle.setText(
                "Beginner / Upper Beginner:"
        );

        txtIntermediateTitle.setText(
                "Intermediate " + sportName + ":"
        );

        txtSeniorTitle.setText(
                "Senior " + sportName + ":"
        );

        // Returns to the training list.
        // The reverse slide animation comes from
        // TrainingSessionsFragment's fragment transaction.
        btnBackTraining.setOnClickListener(v -> {

            getParentFragmentManager()
                    .popBackStack();
        });

        btnRegisterNow.setOnClickListener(v -> {

            Toast.makeText(
                    requireContext(),
                    sportName
                            + " registration will be added later.",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }
}