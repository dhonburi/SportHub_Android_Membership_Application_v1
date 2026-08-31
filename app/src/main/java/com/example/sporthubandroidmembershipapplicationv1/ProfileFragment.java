package com.example.sporthubandroidmembershipapplicationv1;

import android.content.Intent;
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

import com.example.sporthubandroidmembershipapplicationv1.models.MemberProfileResponse;
import com.example.sporthubandroidmembershipapplicationv1.models.TopUpBalanceRequest;
import com.example.sporthubandroidmembershipapplicationv1.models.TopUpBalanceResponse;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;
import com.example.sporthubandroidmembershipapplicationv1.session.MemberSession;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private View btnProfileDetails;
    private View btnTopUpBalance;

    private TextView txtMemberName;
    private TextView txtMemberId;
    private TextView txtBalance;
    private TextView txtLevel;

    private LinearLayout layoutTransactions;
    private LinearLayout layoutRewards;
    private LinearLayout layoutMemberships;

    private double currentBalance = 0.00;

    private Call<MemberProfileResponse> memberProfileCall;
    private Call<TopUpBalanceResponse> topUpBalanceCall;

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

        updateBalanceText();
        setClickListeners();
        loadMemberProfile();
    }

    private void loadMemberProfile() {
        MemberSession memberSession =
                new MemberSession(requireContext());

        int memberId =
                memberSession.getMemberId();

        String savedMemberNumber =
                memberSession.getMemberNumber();

        if (memberId <= 0) {
            showProfileUnavailable(
                    "No logged-in member was found.",
                    savedMemberNumber
            );
            return;
        }

        txtMemberName.setText("Loading profile...");
        setMemberNumberText(savedMemberNumber);

        memberProfileCall =
                ApiClient
                        .getMemberApiService()
                        .getMemberProfile(memberId);

        memberProfileCall.enqueue(
                new Callback<MemberProfileResponse>() {

                    @Override
                    public void onResponse(
                            Call<MemberProfileResponse> call,
                            Response<MemberProfileResponse> response
                    ) {
                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        MemberProfileResponse profile =
                                response.body();

                        if (response.isSuccessful()
                                && profile != null) {

                            displayMemberProfile(profile);
                            return;
                        }

                        showProfileUnavailable(
                                "Member profile unavailable",
                                savedMemberNumber
                        );
                    }

                    @Override
                    public void onFailure(
                            Call<MemberProfileResponse> call,
                            Throwable throwable
                    ) {
                        if (call.isCanceled()
                                || !isAdded()
                                || getView() == null) {

                            return;
                        }

                        showProfileUnavailable(
                                "Unable to load profile",
                                savedMemberNumber
                        );
                    }
                }
        );
    }

    private void displayMemberProfile(
            MemberProfileResponse profile
    ) {
        String firstName =
                cleanProfileValue(profile.getFirstName());

        String lastName =
                cleanProfileValue(profile.getLastName());

        String fullName =
                (firstName + " " + lastName).trim();

        if (fullName.isEmpty()) {
            fullName = "SportHub Member";
        }

        txtMemberName.setText(fullName);
        setMemberNumberText(profile.getMemberNumber());

        currentBalance = profile.getBalance();
        updateBalanceText();
    }

    private String cleanProfileValue(String value) {
        return value == null
                ? ""
                : value.trim();
    }

    private void setMemberNumberText(
            String memberNumber
    ) {
        String cleanMemberNumber =
                cleanProfileValue(memberNumber);

        if (cleanMemberNumber.isEmpty()) {
            txtMemberId.setText("ID: —");
            return;
        }

        txtMemberId.setText(
                String.format(
                        Locale.getDefault(),
                        "ID: %s",
                        cleanMemberNumber
                )
        );
    }

    private void showProfileUnavailable(
            String message,
            String memberNumber
    ) {
        txtMemberName.setText(message);
        setMemberNumberText(memberNumber);
    }

    private void setClickListeners() {
        btnProfileDetails.setOnClickListener(view -> {
            Intent intent = new Intent(
                    requireContext(),
                    ProfileDetailsActivity.class
            );

            startActivity(intent);
        });

        btnTopUpBalance.setOnClickListener(view ->
                showTopUpDialog()
        );

        layoutTransactions.setOnClickListener(view ->
                showMessage(
                        "Transaction history will be added later."
                )
        );

        layoutRewards.setOnClickListener(view ->
                showMessage(
                        "Rewards details will be added later."
                )
        );

        layoutMemberships.setOnClickListener(view -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                    )
                    .replace(
                            R.id.fragmentContainer,
                            new MembershipFragment()
                    )
                    .addToBackStack("membership")
                    .commit();
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

            positiveButton.setOnClickListener(view -> {
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

                    double roundedAmount =
                            Math.round(
                                    topUpAmount * 100.0
                            ) / 100.0;

                    if (roundedAmount != topUpAmount) {
                        showMessage(
                                "Use no more than two decimal places."
                        );
                        return;
                    }

                    submitBalanceTopUp(
                            roundedAmount,
                            topUpDialog,
                            positiveButton
                    );

                } catch (NumberFormatException exception) {
                    showMessage(
                            "Please enter a valid amount."
                    );
                }
            });
        });

        topUpDialog.show();
    }

    private void submitBalanceTopUp(
            double amount,
            AlertDialog topUpDialog,
            Button positiveButton
    ) {
        MemberSession memberSession =
                new MemberSession(requireContext());

        int memberId =
                memberSession.getMemberId();

        if (memberId <= 0) {
            showMessage(
                    "No logged-in member was found."
            );
            return;
        }

        positiveButton.setEnabled(false);
        positiveButton.setText("Adding...");

        TopUpBalanceRequest request =
                new TopUpBalanceRequest(amount);

        topUpBalanceCall =
                ApiClient
                        .getMemberApiService()
                        .topUpMemberBalance(
                                memberId,
                                request
                        );

        topUpBalanceCall.enqueue(
                new Callback<TopUpBalanceResponse>() {

                    @Override
                    public void onResponse(
                            Call<TopUpBalanceResponse> call,
                            Response<TopUpBalanceResponse> response
                    ) {
                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        TopUpBalanceResponse topUpResponse =
                                response.body();

                        if (response.isSuccessful()
                                && topUpResponse != null) {

                            currentBalance =
                                    topUpResponse.getBalance();

                            updateBalanceText();

                            showMessage(
                                    String.format(
                                            Locale.getDefault(),
                                            "%.2f NZD added successfully.",
                                            topUpResponse.getAmountAdded()
                                    )
                            );

                            topUpDialog.dismiss();
                            return;
                        }

                        positiveButton.setEnabled(true);
                        positiveButton.setText("Top Up");

                        if (response.code() == 400) {
                            showMessage(
                                    "The top-up amount is invalid."
                            );
                            return;
                        }

                        if (response.code() == 404) {
                            showMessage(
                                    "Member profile was not found."
                            );
                            return;
                        }

                        showMessage(
                                "Unable to add balance."
                        );
                    }

                    @Override
                    public void onFailure(
                            Call<TopUpBalanceResponse> call,
                            Throwable throwable
                    ) {
                        if (call.isCanceled()
                                || !isAdded()
                                || getView() == null) {

                            return;
                        }

                        positiveButton.setEnabled(true);
                        positiveButton.setText("Top Up");

                        showMessage(
                                "Unable to connect to the API."
                        );
                    }
                }
        );
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

    @Override
    public void onDestroyView() {
        if (memberProfileCall != null
                && !memberProfileCall.isCanceled()) {

            memberProfileCall.cancel();
        }

        if (topUpBalanceCall != null
                && !topUpBalanceCall.isCanceled()) {

            topUpBalanceCall.cancel();
        }

        super.onDestroyView();
    }
}