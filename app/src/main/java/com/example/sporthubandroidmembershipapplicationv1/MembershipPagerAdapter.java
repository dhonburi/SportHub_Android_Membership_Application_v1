package com.example.sporthubandroidmembershipapplicationv1;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sporthubandroidmembershipapplicationv1.models.MemberMembershipResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MembershipPagerAdapter
        extends RecyclerView.Adapter<
        MembershipPagerAdapter.MembershipViewHolder> {

    private final List<MemberMembershipResponse>
            memberships = new ArrayList<>();

    public void submitList(
            List<MemberMembershipResponse> newMemberships
    ) {
        memberships.clear();

        if (newMemberships != null) {
            memberships.addAll(newMemberships);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MembershipViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_owned_membership,
                                parent,
                                false
                        );

        return new MembershipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MembershipViewHolder holder,
            int position
    ) {
        MemberMembershipResponse membership =
                memberships.get(position);

        holder.bind(membership);
    }

    @Override
    public int getItemCount() {
        return memberships.size();
    }

    static class MembershipViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView txtPlanName;
        private final TextView txtStatus;
        private final TextView txtMemberNumber;
        private final TextView txtPrice;
        private final TextView txtDescription;
        private final TextView txtStartDate;
        private final TextView txtExpiryDate;
        private final TextView txtDuration;
        private final TextView txtRemainingEntries;

        private final ProgressBar progressDuration;

        MembershipViewHolder(@NonNull View itemView) {
            super(itemView);

            txtPlanName =
                    itemView.findViewById(
                            R.id.txtPagerPlanName
                    );

            txtStatus =
                    itemView.findViewById(
                            R.id.txtPagerStatus
                    );

            txtMemberNumber =
                    itemView.findViewById(
                            R.id.txtPagerMemberNumber
                    );

            txtPrice =
                    itemView.findViewById(
                            R.id.txtPagerPrice
                    );

            txtDescription =
                    itemView.findViewById(
                            R.id.txtPagerDescription
                    );

            txtStartDate =
                    itemView.findViewById(
                            R.id.txtPagerStartDate
                    );

            txtExpiryDate =
                    itemView.findViewById(
                            R.id.txtPagerExpiryDate
                    );

            txtDuration =
                    itemView.findViewById(
                            R.id.txtPagerDuration
                    );

            txtRemainingEntries =
                    itemView.findViewById(
                            R.id.txtPagerRemainingEntries
                    );

            progressDuration =
                    itemView.findViewById(
                            R.id.progressPagerDuration
                    );
        }

        void bind(MemberMembershipResponse membership) {
            txtPlanName.setText(
                    valueOrFallback(
                            membership.getPlanName(),
                            "Membership Plan"
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

            txtPrice.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.2f NZD",
                            membership.getPrice()
                    )
            );

            txtDescription.setText(
                    valueOrFallback(
                            membership.getDescription(),
                            "No membership description is available."
                    )
            );

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

            configureDuration(
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

        private void configureDuration(
                Date startDate,
                Date expiryDate,
                String status
        ) {
            if (startDate == null || expiryDate == null) {
                progressDuration.setVisibility(View.GONE);

                txtDuration.setText(
                        "No expiry date"
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

            boolean expired =
                    "Expired".equalsIgnoreCase(status)
                            || today.after(expiryDate);

            int colour;
            String message;

            if (expired) {
                progress = 100;
                colour = Color.parseColor("#D64545");

                message =
                        "Expired on "
                                + formatDisplayDate(expiryDate);

            } else if (today.before(startDate)) {
                colour = Color.parseColor("#F2C94C");

                message = String.format(
                        Locale.getDefault(),
                        "Starts in %d days",
                        Math.max(
                                0,
                                daysBetween(today, startDate)
                        )
                );

            } else if (remainingDays == 0) {
                colour = Color.parseColor("#D64545");
                message = "Membership ends today";

            } else if (remainingDays <= 30) {
                colour = Color.parseColor("#D64545");

                message = String.format(
                        Locale.getDefault(),
                        "%d days remaining — ending soon",
                        remainingDays
                );

            } else if (remainingDays <= 60) {
                colour = Color.parseColor("#F2994A");

                message = String.format(
                        Locale.getDefault(),
                        "%d days remaining",
                        remainingDays
                );

            } else {
                colour = Color.parseColor("#F2C94C");

                message = String.format(
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
                    ColorStateList.valueOf(colour)
            );

            progressDuration
                    .setProgressBackgroundTintList(
                            ColorStateList.valueOf(
                                    Color.parseColor("#D9D9D9")
                            )
                    );

            txtDuration.setText(message);
            txtDuration.setTextColor(colour);
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
    }
}