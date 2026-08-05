package com.example.sporthubandroidmembershipapplicationv1;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sporthubandroidmembershipapplicationv1.models.MemberMembershipResponse;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;
import com.example.sporthubandroidmembershipapplicationv1.session.MemberSession;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MembershipFragment extends Fragment {

    private ProgressBar progressLoading;
    private ProgressBar progressDuration;

    private View layoutMembershipContent;

    private TextView txtMessage;
    private TextView txtPlanName;
    private TextView txtStatus;
    private TextView txtMemberNumber;
    private TextView txtDescription;
    private TextView txtStartDate;
    private TextView txtExpiryDate;
    private TextView txtDuration;
    private TextView txtRemainingEntries;

    private Button btnRetry;

    private Call<MemberMembershipResponse> membershipCall;

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

        view.findViewById(R.id.btnMembershipBack)
                .setOnClickListener(clickedView ->
                        getParentFragmentManager()
                                .popBackStack()
                );

        btnRetry.setOnClickListener(clickedView ->
                loadMembership()
        );

        loadMembership();
    }

    private void bindViews(View view) {
        progressLoading =
                view.findViewById(
                        R.id.progressMembershipLoading
                );

        progressDuration =
                view.findViewById(
                        R.id.progressMembershipDuration
                );

        layoutMembershipContent =
                view.findViewById(
                        R.id.layoutMembershipContent
                );

        txtMessage =
                view.findViewById(
                        R.id.txtMembershipMessage
                );

        txtPlanName =
                view.findViewById(
                        R.id.txtMembershipPlanName
                );

        txtStatus =
                view.findViewById(
                        R.id.txtMembershipStatus
                );

        txtMemberNumber =
                view.findViewById(
                        R.id.txtMembershipNumber
                );

        txtDescription =
                view.findViewById(
                        R.id.txtMembershipDescription
                );

        txtStartDate =
                view.findViewById(
                        R.id.txtMembershipStartDate
                );

        txtExpiryDate =
                view.findViewById(
                        R.id.txtMembershipExpiryDate
                );

        txtDuration =
                view.findViewById(
                        R.id.txtMembershipDuration
                );

        txtRemainingEntries =
                view.findViewById(
                        R.id.txtRemainingEntries
                );

        btnRetry =
                view.findViewById(
                        R.id.btnRetryMembership
                );
    }

    private void loadMembership() {
        MemberSession memberSession =
                new MemberSession(requireContext());

        int memberId =
                memberSession.getMemberId();

        if (memberId <= 0) {
            showError(
                    "No logged-in member was found.",
                    false
            );
            return;
        }

        showLoading();

        membershipCall = ApiClient
                .getMemberApiService()
                .getMemberMembership(memberId);

        membershipCall.enqueue(
                new Callback<MemberMembershipResponse>() {
                    @Override
                    public void onResponse(
                            Call<MemberMembershipResponse> call,
                            Response<MemberMembershipResponse> response
                    ) {
                        if (!isAdded() || getView() == null) {
                            return;
                        }

                        MemberMembershipResponse membership =
                                response.body();

                        if (response.isSuccessful()
                                && membership != null) {

                            displayMembership(membership);
                            return;
                        }

                        if (response.code() == 404) {
                            showError(
                                    "No membership has been assigned "
                                            + "to this account yet.",
                                    false
                            );
                            return;
                        }

                        showError(
                                "Unable to load your membership.",
                                true
                        );
                    }

                    @Override
                    public void onFailure(
                            Call<MemberMembershipResponse> call,
                            Throwable throwable
                    ) {
                        if (call.isCanceled()
                                || !isAdded()
                                || getView() == null) {
                            return;
                        }

                        showError(
                                "Unable to connect to the server.",
                                true
                        );
                    }
                }
        );
    }

    private void displayMembership(
            MemberMembershipResponse membership
    ) {
        progressLoading.setVisibility(View.GONE);
        txtMessage.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);

        layoutMembershipContent.setVisibility(
                View.VISIBLE
        );

        txtPlanName.setText(
                valueOrFallback(
                        membership.getPlanName(),
                        "Membership plan"
                )
        );

        String status =
                valueOrFallback(
                        membership.getStatus(),
                        "Unknown"
                );

        txtStatus.setText(status);
        applyStatusStyle(status);

        txtMemberNumber.setText(
                String.format(
                        Locale.getDefault(),
                        "Member %s",
                        valueOrFallback(
                                membership.getMemberNumber(),
                                "—"
                        )
                )
        );

        String description =
                clean(membership.getDescription());

        if (description.isEmpty()) {
            txtDescription.setVisibility(View.GONE);
        } else {
            txtDescription.setText(description);
            txtDescription.setVisibility(View.VISIBLE);
        }

        Date startDate =
                parseApiDate(
                        membership.getStartDate()
                );

        Date expiryDate =
                parseApiDate(
                        membership.getExpiryDate()
                );

        txtStartDate.setText(
                formatDisplayDate(startDate)
        );

        txtExpiryDate.setText(
                expiryDate == null
                        ? "No expiry"
                        : formatDisplayDate(expiryDate)
        );

        configureDurationBar(
                startDate,
                expiryDate,
                status
        );

        Integer remainingEntries =
                membership.getRemainingEntries();

        if (remainingEntries == null) {
            txtRemainingEntries.setVisibility(
                    View.GONE
            );
        } else {
            txtRemainingEntries.setText(
                    String.format(
                            Locale.getDefault(),
                            "%d entries remaining",
                            remainingEntries
                    )
            );

            txtRemainingEntries.setVisibility(
                    View.VISIBLE
            );
        }
    }

    private void configureDurationBar(
            Date startDate,
            Date expiryDate,
            String status
    ) {
        if (startDate == null || expiryDate == null) {
            progressDuration.setVisibility(View.GONE);

            txtDuration.setText(
                    "No expiry date has been provided."
            );

            txtDuration.setTextColor(
                    Color.parseColor("#666666")
            );

            return;
        }

        progressDuration.setVisibility(View.VISIBLE);

        Date today = startOfToday();

        long totalDays = Math.max(
                1,
                daysBetween(startDate, expiryDate)
        );

        long elapsedDays =
                daysBetween(startDate, today);

        int progress =
                (int) Math.round(
                        (elapsedDays * 100.0)
                                / totalDays
                );

        progress = Math.max(
                0,
                Math.min(100, progress)
        );

        long remainingDays =
                daysBetween(today, expiryDate);

        boolean statusExpired =
                "Expired".equalsIgnoreCase(status);

        boolean dateExpired =
                today.after(expiryDate);

        int barColour;
        String durationMessage;

        if (statusExpired || dateExpired) {
            progress = 100;
            barColour = Color.parseColor("#D64545");

            durationMessage =
                    "Membership expired on "
                            + formatDisplayDate(expiryDate);

        } else if (today.before(startDate)) {
            barColour = Color.parseColor("#F2C94C");

            durationMessage = String.format(
                    Locale.getDefault(),
                    "Starts in %d days",
                    Math.max(
                            0,
                            daysBetween(today, startDate)
                    )
            );

        } else if (remainingDays == 0) {
            barColour = Color.parseColor("#D64545");
            durationMessage = "Membership ends today";

        } else if (remainingDays <= 30) {
            barColour = Color.parseColor("#D64545");

            durationMessage = String.format(
                    Locale.getDefault(),
                    "%d days remaining — ending soon",
                    remainingDays
            );

        } else if (remainingDays <= 60) {
            barColour = Color.parseColor("#F2994A");

            durationMessage = String.format(
                    Locale.getDefault(),
                    "%d days remaining",
                    remainingDays
            );

        } else {
            barColour = Color.parseColor("#F2C94C");

            durationMessage = String.format(
                    Locale.getDefault(),
                    "%d days remaining",
                    remainingDays
            );
        }

        progressDuration.setProgress(
                progress,
                true
        );

        progressDuration.setProgressTintList(
                ColorStateList.valueOf(barColour)
        );

        progressDuration.setProgressBackgroundTintList(
                ColorStateList.valueOf(
                        Color.parseColor("#D9D9D9")
                )
        );

        txtDuration.setText(durationMessage);
        txtDuration.setTextColor(barColour);
    }

    private void applyStatusStyle(String status) {
        if ("Active".equalsIgnoreCase(status)) {
            txtStatus.setBackgroundResource(
                    R.drawable.membership_status_active_bg
            );

            txtStatus.setTextColor(
                    Color.parseColor("#176B39")
            );

            return;
        }

        if ("Expired".equalsIgnoreCase(status)) {
            txtStatus.setBackgroundResource(
                    R.drawable.membership_status_expired_bg
            );

            txtStatus.setTextColor(
                    Color.parseColor("#A52A2A")
            );

            return;
        }

        txtStatus.setBackgroundResource(
                R.drawable.membership_status_other_bg
        );

        txtStatus.setTextColor(
                Color.parseColor("#111111")
        );
    }

    private Date parseApiDate(String value) {
        String cleanValue = clean(value);

        if (cleanValue.length() < 10) {
            return null;
        }

        SimpleDateFormat apiDateFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        apiDateFormat.setLenient(false);

        try {
            return apiDateFormat.parse(
                    cleanValue.substring(0, 10)
            );

        } catch (ParseException exception) {
            return null;
        }
    }

    private String formatDisplayDate(Date date) {
        if (date == null) {
            return "Not provided";
        }

        SimpleDateFormat displayDateFormat =
                new SimpleDateFormat(
                        "d MMM yyyy",
                        Locale.getDefault()
                );

        return displayDateFormat.format(date);
    }

    private Date startOfToday() {
        Calendar calendar =
                Calendar.getInstance();

        calendar.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    private long daysBetween(
            Date first,
            Date second
    ) {
        double millisecondsPerDay =
                24.0 * 60.0 * 60.0 * 1000.0;

        return Math.round(
                (second.getTime() - first.getTime())
                        / millisecondsPerDay
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

    private void showLoading() {
        layoutMembershipContent.setVisibility(
                View.GONE
        );

        txtMessage.setText(
                "Loading your membership..."
        );

        txtMessage.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.GONE);
        progressLoading.setVisibility(View.VISIBLE);
    }

    private void showError(
            String message,
            boolean showRetry
    ) {
        layoutMembershipContent.setVisibility(
                View.GONE
        );

        progressLoading.setVisibility(View.GONE);

        txtMessage.setText(message);
        txtMessage.setVisibility(View.VISIBLE);

        btnRetry.setVisibility(
                showRetry
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    @Override
    public void onDestroyView() {
        if (membershipCall != null
                && !membershipCall.isCanceled()) {
            membershipCall.cancel();
        }

        super.onDestroyView();
    }
}