package com.example.sporthubandroidmembershipapplicationv1;

import android.os.Bundle;
import android.view.View;

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

        View btnBackTraining = view.findViewById(R.id.btnBackTraining);

        btnBackTraining.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.homeInnerFragmentContainer, new TrainingSessionsFragment())
                    .commit();
        });
    }
}