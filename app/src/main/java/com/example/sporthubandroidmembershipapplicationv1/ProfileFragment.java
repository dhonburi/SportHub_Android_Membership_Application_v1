package com.example.sporthubandroidmembershipapplicationv1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private Button btnSettings;
    private Button btnTopUp;
    private Button btnTransactions;
    private Button btnRewards;
    private Button btnMembership;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        btnSettings = view.findViewById(R.id.btnSettings);
        btnTopUp = view.findViewById(R.id.btnTopUp);
        btnTransactions = view.findViewById(R.id.btnTransactions);
        btnRewards = view.findViewById(R.id.btnRewards);
        btnMembership = view.findViewById(R.id.btnMembership);

        btnSettings.setOnClickListener(v -> {

            Intent intent = new Intent(
                    requireActivity(),
                    SettingsActivity.class
            );

            startActivity(intent);

        });

        btnTopUp.setOnClickListener(v -> {

            // TODO
            // Open Top Up page

        });

        btnTransactions.setOnClickListener(v -> {

            // TODO
            // Open Transactions page

        });

        btnRewards.setOnClickListener(v -> {

            // TODO
            // Open Rewards page

        });

        btnMembership.setOnClickListener(v -> {

            // TODO
            // Open Membership page

        });

    }

}