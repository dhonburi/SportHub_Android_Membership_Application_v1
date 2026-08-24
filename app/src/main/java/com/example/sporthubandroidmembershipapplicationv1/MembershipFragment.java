package com.example.sporthubandroidmembershipapplicationv1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.sporthubandroidmembershipapplicationv1.models.MemberMembershipResponse;
import com.example.sporthubandroidmembershipapplicationv1.models.MembershipPlanResponse;
import com.example.sporthubandroidmembershipapplicationv1.models.PurchaseMembershipRequest;
import com.example.sporthubandroidmembershipapplicationv1.models.PurchaseMembershipResponse;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;
import com.example.sporthubandroidmembershipapplicationv1.session.MemberSession;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MembershipFragment extends Fragment {

    private ProgressBar progressMembershipsLoading;
    private ProgressBar progressPlansLoading;

    private TextView txtMembershipsMessage;
    private TextView txtPlansMessage;

    private ViewPager2 viewPagerMemberships;

    private LinearLayout layoutMembershipDots;
    private LinearLayout layoutMembershipPlans;

    private Button btnRetryMemberships;
    private Button btnRetryPlans;

    private MembershipPagerAdapter membershipPagerAdapter;

    private Call<List<MemberMembershipResponse>>
            membershipsCall;

    private Call<List<MembershipPlanResponse>>
            membershipPlansCall;

    private Call<PurchaseMembershipResponse>
            purchaseMembershipCall;

    private final ViewPager2.OnPageChangeCallback
            pageChangeCallback =
            new ViewPager2.OnPageChangeCallback() {

                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    updateDots(position);
                }
            };

    public MembershipFragment() {
        super(R.layout.fragment_membership);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        configureMembershipPager();

        view.findViewById(R.id.btnMembershipBack)
                .setOnClickListener(clickedView ->
                        getParentFragmentManager()
                                .popBackStack()
                );

        btnRetryMemberships.setOnClickListener(
                clickedView -> loadMemberships()
        );

        btnRetryPlans.setOnClickListener(
                clickedView -> loadMembershipPlans()
        );

        loadMemberships();
        loadMembershipPlans();
    }

    private void bindViews(View view) {
        progressMembershipsLoading =
                view.findViewById(
                        R.id.progressMembershipsLoading
                );

        progressPlansLoading =
                view.findViewById(
                        R.id.progressMembershipPlansLoading
                );

        txtMembershipsMessage =
                view.findViewById(
                        R.id.txtMembershipsMessage
                );

        txtPlansMessage =
                view.findViewById(
                        R.id.txtMembershipPlansMessage
                );

        viewPagerMemberships =
                view.findViewById(
                        R.id.viewPagerMemberships
                );

        layoutMembershipDots =
                view.findViewById(
                        R.id.layoutMembershipDots
                );

        layoutMembershipPlans =
                view.findViewById(
                        R.id.layoutMembershipPlans
                );

        btnRetryMemberships =
                view.findViewById(
                        R.id.btnRetryMemberships
                );

        btnRetryPlans =
                view.findViewById(
                        R.id.btnRetryMembershipPlans
                );
    }

    private void configureMembershipPager() {
        membershipPagerAdapter =
                new MembershipPagerAdapter();

        viewPagerMemberships.setAdapter(
                membershipPagerAdapter
        );

        viewPagerMemberships.setOffscreenPageLimit(1);

        viewPagerMemberships.registerOnPageChangeCallback(
                pageChangeCallback
        );
    }

    private int getLoggedInMemberId() {
        MemberSession memberSession =
                new MemberSession(requireContext());

        return memberSession.getMemberId();
    }

    private void loadMemberships() {
        int memberId = getLoggedInMemberId();

        if (memberId <= 0) {
            showMembershipsError(
                    "No logged-in member was found.",
                    false
            );

            return;
        }

        showMembershipsLoading();

        membershipsCall =
                ApiClient
                        .getMemberApiService()
                        .getMemberMemberships(memberId);

        membershipsCall.enqueue(
                new Callback<List<MemberMembershipResponse>>() {

                    @Override
                    public void onResponse(
                            Call<List<MemberMembershipResponse>> call,
                            Response<List<MemberMembershipResponse>>
                                    response
                    ) {
                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        List<MemberMembershipResponse> memberships =
                                response.body();

                        if (response.isSuccessful()
                                && memberships != null) {

                            displayMemberships(memberships);
                            return;
                        }

                        showMembershipsError(
                                "Unable to load your memberships.",
                                true
                        );
                    }

                    @Override
                    public void onFailure(
                            Call<List<MemberMembershipResponse>> call,
                            Throwable throwable
                    ) {
                        if (call.isCanceled()
                                || !isAdded()
                                || getView() == null) {
                            return;
                        }

                        showMembershipsError(
                                "Unable to connect to the server.",
                                true
                        );
                    }
                }
        );
    }

    private void displayMemberships(
            List<MemberMembershipResponse> memberships
    ) {
        progressMembershipsLoading.setVisibility(
                View.GONE
        );

        btnRetryMemberships.setVisibility(
                View.GONE
        );

        if (memberships.isEmpty()) {
            membershipPagerAdapter.submitList(
                    memberships
            );

            viewPagerMemberships.setVisibility(
                    View.GONE
            );

            layoutMembershipDots.removeAllViews();
            layoutMembershipDots.setVisibility(
                    View.GONE
            );

            txtMembershipsMessage.setText(
                    "No memberships have been purchased yet."
            );

            txtMembershipsMessage.setVisibility(
                    View.VISIBLE
            );

            return;
        }

        txtMembershipsMessage.setVisibility(
                View.GONE
        );

        membershipPagerAdapter.submitList(
                memberships
        );

        viewPagerMemberships.setVisibility(
                View.VISIBLE
        );

        createDots(memberships.size());

        viewPagerMemberships.setCurrentItem(
                0,
                false
        );

        updateDots(0);
    }

    private void createDots(int membershipCount) {
        layoutMembershipDots.removeAllViews();

        for (int index = 0;
             index < membershipCount;
             index++) {

            final int pagePosition = index;

            View dot = new View(requireContext());

            int dotSize = dpToPx(10);
            int dotMargin = dpToPx(5);

            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(
                            dotSize,
                            dotSize
                    );

            layoutParams.setMargins(
                    dotMargin,
                    0,
                    dotMargin,
                    0
            );

            dot.setLayoutParams(layoutParams);

            dot.setContentDescription(
                    String.format(
                            Locale.getDefault(),
                            "Membership %d of %d",
                            index + 1,
                            membershipCount
                    )
            );

            dot.setOnClickListener(clickedView ->
                    viewPagerMemberships.setCurrentItem(
                            pagePosition,
                            true
                    )
            );

            layoutMembershipDots.addView(dot);
        }

        layoutMembershipDots.setVisibility(
                View.VISIBLE
        );
    }

    private void updateDots(int selectedPosition) {
        if (layoutMembershipDots == null) {
            return;
        }

        int dotCount =
                layoutMembershipDots.getChildCount();

        for (int index = 0;
             index < dotCount;
             index++) {

            View dot =
                    layoutMembershipDots.getChildAt(index);

            if (index == selectedPosition) {
                dot.setBackgroundResource(
                        R.drawable.membership_dot_active
                );
            } else {
                dot.setBackgroundResource(
                        R.drawable.membership_dot_inactive
                );
            }
        }
    }

    private void loadMembershipPlans() {
        int memberId = getLoggedInMemberId();

        if (memberId <= 0) {
            showPlansError(
                    "No logged-in member was found.",
                    false
            );

            return;
        }

        showPlansLoading();

        membershipPlansCall =
                ApiClient
                        .getMemberApiService()
                        .getMembershipPlans(memberId);

        membershipPlansCall.enqueue(
                new Callback<List<MembershipPlanResponse>>() {

                    @Override
                    public void onResponse(
                            Call<List<MembershipPlanResponse>> call,
                            Response<List<MembershipPlanResponse>>
                                    response
                    ) {
                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        List<MembershipPlanResponse> plans =
                                response.body();

                        if (response.isSuccessful()
                                && plans != null) {

                            displayMembershipPlans(plans);
                            return;
                        }

                        showPlansError(
                                "Unable to load membership plans.",
                                true
                        );
                    }

                    @Override
                    public void onFailure(
                            Call<List<MembershipPlanResponse>> call,
                            Throwable throwable
                    ) {
                        if (call.isCanceled()
                                || !isAdded()
                                || getView() == null) {
                            return;
                        }

                        showPlansError(
                                "Unable to connect to the server.",
                                true
                        );
                    }
                }
        );
    }

    private void displayMembershipPlans(
            List<MembershipPlanResponse> plans
    ) {
        progressPlansLoading.setVisibility(
                View.GONE
        );

        btnRetryPlans.setVisibility(
                View.GONE
        );

        layoutMembershipPlans.removeAllViews();
        layoutMembershipPlans.setVisibility(
                View.VISIBLE
        );

        if (plans.isEmpty()) {
            txtPlansMessage.setText(
                    "No membership plans are currently available."
            );

            txtPlansMessage.setVisibility(
                    View.VISIBLE
            );

            return;
        }

        txtPlansMessage.setVisibility(
                View.GONE
        );

        LayoutInflater inflater =
                LayoutInflater.from(requireContext());

        for (MembershipPlanResponse plan : plans) {
            View planView =
                    inflater.inflate(
                            R.layout.item_membership_plan,
                            layoutMembershipPlans,
                            false
                    );

            TextView planName =
                    planView.findViewById(
                            R.id.txtAvailablePlanName
                    );

            TextView planPrice =
                    planView.findViewById(
                            R.id.txtAvailablePlanPrice
                    );

            TextView planDescription =
                    planView.findViewById(
                            R.id.txtAvailablePlanDescription
                    );

            Button purchaseButton =
                    planView.findViewById(
                            R.id.btnPurchaseMembershipPlan
                    );

            planName.setText(
                    valueOrFallback(
                            plan.getPlanName(),
                            "Membership Plan"
                    )
            );

            planPrice.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.2f NZD",
                            plan.getPrice()
                    )
            );

            String description =
                    clean(plan.getDescription());

            if (description.isEmpty()) {
                planDescription.setText(
                        "No plan description is available."
                );
            } else {
                planDescription.setText(description);
            }

            if (plan.isAlreadyActive()) {
                purchaseButton.setText(
                        "Already Active"
                );

                purchaseButton.setEnabled(false);
                purchaseButton.setAlpha(0.55f);

            } else {
                purchaseButton.setText(
                        "Purchase Plan"
                );

                purchaseButton.setEnabled(true);
                purchaseButton.setAlpha(1.0f);

                purchaseButton.setOnClickListener(
                        clickedView ->
                                showPurchaseConfirmation(
                                        plan,
                                        purchaseButton
                                )
                );
            }

            layoutMembershipPlans.addView(planView);
        }
    }

    private void showPurchaseConfirmation(
            MembershipPlanResponse plan,
            Button purchaseButton
    ) {
        String planName =
                valueOrFallback(
                        plan.getPlanName(),
                        "Membership Plan"
                );

        String confirmationMessage =
                String.format(
                        Locale.getDefault(),
                        "Purchase %s for %.2f NZD? "
                                + "The price will be deducted "
                                + "from your SportHub balance.",
                        planName,
                        plan.getPrice()
                );

        new AlertDialog.Builder(requireContext())
                .setTitle(
                        "Confirm Membership Purchase"
                )
                .setMessage(confirmationMessage)
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Purchase",
                        (dialog, which) ->
                                purchaseMembership(
                                        plan,
                                        purchaseButton
                                )
                )
                .show();
    }

    private void purchaseMembership(
            MembershipPlanResponse plan,
            Button purchaseButton
    ) {
        int memberId = getLoggedInMemberId();

        if (memberId <= 0) {
            showMessage(
                    "No logged-in member was found."
            );

            return;
        }

        purchaseButton.setEnabled(false);
        purchaseButton.setText("Purchasing...");

        PurchaseMembershipRequest request =
                new PurchaseMembershipRequest(
                        plan.getMembershipPlanId()
                );

        purchaseMembershipCall =
                ApiClient
                        .getMemberApiService()
                        .purchaseMembership(
                                memberId,
                                request
                        );

        purchaseMembershipCall.enqueue(
                new Callback<PurchaseMembershipResponse>() {

                    @Override
                    public void onResponse(
                            Call<PurchaseMembershipResponse> call,
                            Response<PurchaseMembershipResponse>
                                    response
                    ) {
                        if (!isAdded()
                                || getView() == null) {
                            return;
                        }

                        PurchaseMembershipResponse purchase =
                                response.body();

                        if (response.isSuccessful()
                                && purchase != null) {

                            showMessage(
                                    String.format(
                                            Locale.getDefault(),
                                            "%s purchased. "
                                                    + "Balance: %.2f NZD",
                                            purchase.getPlanName(),
                                            purchase.getBalance()
                                    )
                            );

                            loadMemberships();
                            loadMembershipPlans();
                            return;
                        }

                        purchaseButton.setEnabled(true);
                        purchaseButton.setText(
                                "Purchase Plan"
                        );

                        if (response.code() == 400) {
                            showMessage(
                                    "You do not have enough balance "
                                            + "to purchase this plan."
                            );

                            return;
                        }

                        if (response.code() == 409) {
                            showMessage(
                                    "This membership plan "
                                            + "is already active."
                            );

                            loadMembershipPlans();
                            return;
                        }

                        if (response.code() == 404) {
                            showMessage(
                                    "The member or membership plan "
                                            + "could not be found."
                            );

                            return;
                        }

                        showMessage(
                                "Unable to purchase this membership."
                        );
                    }

                    @Override
                    public void onFailure(
                            Call<PurchaseMembershipResponse> call,
                            Throwable throwable
                    ) {
                        if (call.isCanceled()
                                || !isAdded()
                                || getView() == null) {
                            return;
                        }

                        purchaseButton.setEnabled(true);

                        purchaseButton.setText(
                                "Purchase Plan"
                        );

                        showMessage(
                                "Unable to connect to the server."
                        );
                    }
                }
        );
    }

    private void showMembershipsLoading() {
        viewPagerMemberships.setVisibility(
                View.GONE
        );

        layoutMembershipDots.setVisibility(
                View.GONE
        );

        txtMembershipsMessage.setText(
                "Loading your memberships..."
        );

        txtMembershipsMessage.setVisibility(
                View.VISIBLE
        );

        btnRetryMemberships.setVisibility(
                View.GONE
        );

        progressMembershipsLoading.setVisibility(
                View.VISIBLE
        );
    }

    private void showMembershipsError(
            String message,
            boolean showRetry
    ) {
        viewPagerMemberships.setVisibility(
                View.GONE
        );

        layoutMembershipDots.setVisibility(
                View.GONE
        );

        progressMembershipsLoading.setVisibility(
                View.GONE
        );

        txtMembershipsMessage.setText(message);
        txtMembershipsMessage.setVisibility(
                View.VISIBLE
        );

        btnRetryMemberships.setVisibility(
                showRetry
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void showPlansLoading() {
        layoutMembershipPlans.removeAllViews();

        txtPlansMessage.setText(
                "Loading available memberships..."
        );

        txtPlansMessage.setVisibility(
                View.VISIBLE
        );

        btnRetryPlans.setVisibility(
                View.GONE
        );

        progressPlansLoading.setVisibility(
                View.VISIBLE
        );
    }

    private void showPlansError(
            String message,
            boolean showRetry
    ) {
        layoutMembershipPlans.removeAllViews();

        progressPlansLoading.setVisibility(
                View.GONE
        );

        txtPlansMessage.setText(message);
        txtPlansMessage.setVisibility(
                View.VISIBLE
        );

        btnRetryPlans.setVisibility(
                showRetry
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private String valueOrFallback(
            String value,
            String fallback
    ) {
        String cleanValue = clean(value);

        return cleanValue.isEmpty()
                ? fallback
                : cleanValue;
    }

    private String clean(String value) {
        return value == null
                ? ""
                : value.trim();
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
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onDestroyView() {
        viewPagerMemberships
                .unregisterOnPageChangeCallback(
                        pageChangeCallback
                );

        viewPagerMemberships.setAdapter(null);

        if (membershipsCall != null
                && !membershipsCall.isCanceled()) {

            membershipsCall.cancel();
        }

        if (membershipPlansCall != null
                && !membershipPlansCall.isCanceled()) {

            membershipPlansCall.cancel();
        }

        if (purchaseMembershipCall != null
                && !purchaseMembershipCall.isCanceled()) {

            purchaseMembershipCall.cancel();
        }

        membershipPagerAdapter = null;

        super.onDestroyView();
    }
}