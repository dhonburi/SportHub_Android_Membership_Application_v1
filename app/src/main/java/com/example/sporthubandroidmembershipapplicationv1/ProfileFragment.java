package com.example.sporthubandroidmembershipapplicationv1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private Button btnSettings;
    private Button btnTopUp;

    private TextView txtMemberName;
    private TextView txtMemberId;
    private TextView txtBalance;
    private TextView txtLevel;

    private LinearLayout layoutTransactions;
    private LinearLayout layoutRewards;
    private LinearLayout layoutMemberships;

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

        txtMemberName = view.findViewById(R.id.txtMemberName);
        txtMemberId = view.findViewById(R.id.txtMemberId);
        txtBalance = view.findViewById(R.id.txtBalance);
        txtLevel = view.findViewById(R.id.txtLevel);

        layoutTransactions = view.findViewById(R.id.layoutTransactions);
        layoutRewards = view.findViewById(R.id.layoutRewards);
        layoutMemberships = view.findViewById(R.id.layoutMemberships);

        if (txtMemberName != null)
            txtMemberName.setText("John Smith");

        if (txtMemberId != null)
            txtMemberId.setText("Member ID: 4213477");

        if (txtBalance != null)
            txtBalance.setText("$102.34");

        if (txtLevel != null)
            txtLevel.setText("LV2");

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(
                        requireActivity(),
                        SettingsActivity.class
                );
                startActivity(intent);
            });
        }

        if (btnTopUp != null) {
            btnTopUp.setOnClickListener(v -> {
                // TODO
            });
        }

        if (layoutTransactions != null) {
            layoutTransactions.setOnClickListener(v -> {
                // TODO
            });
        }

        if (layoutRewards != null) {
            layoutRewards.setOnClickListener(v -> {
                // TODO
            });
        }

        if (layoutMemberships != null) {
            layoutMemberships.setOnClickListener(v -> {
                // TODO
            });
        }
    }
}