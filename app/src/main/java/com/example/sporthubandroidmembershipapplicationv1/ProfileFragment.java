package com.example.sporthubandroidmembershipapplicationv1;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    private View btnProfileDetails;

    // This must be View because the XML uses a LinearLayout
    private View btnTopUpBalance;

    private TextView txtMemberName;
    private TextView txtMemberId;
    private TextView txtBalance;
    private TextView txtLevel;

    private LinearLayout layoutTransactions;
    private LinearLayout layoutRewards;
    private LinearLayout layoutMemberships;

    private double currentBalance = 14.67;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        btnProfileDetails =
                view.findViewById(R.id.btnProfileDetails);

        btnTopUpBalance =
                view.findViewById(R.id.btnTopUpBalance);

        txtMemberName =
                view.findViewById(R.id.txtMemberName);

        txtMemberId =
                view.findViewById(R.id.txtMemberId);

        txtBalance =
                view.findViewById(R.id.txtBalance);

        txtLevel =
                view.findViewById(R.id.txtLevel);

        layoutTransactions =
                view.findViewById(R.id.layoutTransactions);

        layoutRewards =
                view.findViewById(R.id.layoutRewards);

        layoutMemberships =
                view.findViewById(R.id.layoutMemberships);

        setTemporaryMemberInformation();
        updateBalanceText();
        setClickListeners();
    }

    private void setTemporaryMemberInformation() {

        txtMemberName.setText("Noah Hayes");
        txtMemberId.setText("ID: 201500067");
        txtLevel.setText("LV 2");
    }

    private void setClickListeners() {

        btnProfileDetails.setOnClickListener(v -> {
            Toast.makeText(
                    requireContext(),
                    "Profile details will be added later.",
                    Toast.LENGTH_SHORT
            ).show();
        });

        btnTopUpBalance.setOnClickListener(v ->
                showTopUpDialog()
        );

        layoutTransactions.setOnClickListener(v -> {
            Toast.makeText(
                    requireContext(),
                    "Transaction history will be added later.",
                    Toast.LENGTH_SHORT
            ).show();
        });

        layoutRewards.setOnClickListener(v -> {
            Toast.makeText(
                    requireContext(),
                    "Rewards details will be added later.",
                    Toast.LENGTH_SHORT
            ).show();
        });

        layoutMemberships.setOnClickListener(v -> {
            Toast.makeText(
                    requireContext(),
                    "Membership management will be added later.",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void updateBalanceText() {

        txtBalance.setText(
                String.format(
                        Locale.getDefault(),
                        "%.2f",
                        currentBalance
                )
        );
    }

    private void showTopUpDialog() {

        EditText amountInput =
                new EditText(requireContext());

        amountInput.setHint("Enter top-up amount");

        amountInput.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        LinearLayout inputContainer =
                new LinearLayout(requireContext());

        inputContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        inputContainer.setPadding(
                dpToPx(24),
                dpToPx(8),
                dpToPx(24),
                0
        );

        inputContainer.addView(amountInput);

        AlertDialog topUpDialog =
                new AlertDialog.Builder(requireContext())
                        .setTitle("Top Up Balance")
                        .setMessage(
                                "Enter the mock amount you would like to add."
                        )
                        .setView(inputContainer)
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Top Up",
                                null
                        )
                        .create();

        topUpDialog.setOnShowListener(dialog -> {

            Button positiveButton =
                    topUpDialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    );

            positiveButton.setOnClickListener(v -> {

                String enteredAmount =
                        amountInput
                                .getText()
                                .toString()
                                .trim();

                if (enteredAmount.isEmpty()) {

                    showMessage(
                            "Please enter an amount."
                    );

                    return;
                }

                try {

                    double topUpAmount =
                            Double.parseDouble(
                                    enteredAmount
                            );

                    if (topUpAmount <= 0) {

                        showMessage(
                                "Enter an amount greater than zero."
                        );

                        return;
                    }

                    currentBalance += topUpAmount;

                    updateBalanceText();

                    showMessage(
                            String.format(
                                    Locale.getDefault(),
                                    "%.2f NZD added successfully.",
                                    topUpAmount
                            )
                    );

                    topUpDialog.dismiss();

                } catch (NumberFormatException exception) {

                    showMessage(
                            "Please enter a valid amount."
                    );
                }
            });
        });

        topUpDialog.show();
    }

    private int dpToPx(int dp) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(dp * density);
    }

    private void showMessage(String message) {

        Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_SHORT
        ).show();
    }
}