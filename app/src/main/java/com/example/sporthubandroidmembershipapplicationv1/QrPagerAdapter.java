package com.example.sporthubandroidmembershipapplicationv1;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sporthubandroidmembershipapplicationv1.models.MemberMembershipResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QrPagerAdapter
        extends RecyclerView.Adapter<QrPagerAdapter.QrViewHolder> {

    public static final int STATE_LOADING = 0;
    public static final int STATE_READY = 1;
    public static final int STATE_ERROR = 2;
    public static final int STATE_EXPIRED = 3;

    public static class QrPageState {

        public int displayState = STATE_LOADING;
        public Bitmap qrBitmap;
        public String remainingLabel = "";
        public String errorMessage = "";

        public static QrPageState loading() {
            QrPageState state = new QrPageState();
            state.displayState = STATE_LOADING;
            return state;
        }

        public static QrPageState expired() {
            QrPageState state = new QrPageState();
            state.displayState = STATE_EXPIRED;
            return state;
        }

        public static QrPageState error(String message) {
            QrPageState state = new QrPageState();
            state.displayState = STATE_ERROR;
            state.errorMessage = message;
            return state;
        }

        public static QrPageState ready(Bitmap bitmap, String remainingLabel) {
            QrPageState state = new QrPageState();
            state.displayState = STATE_READY;
            state.qrBitmap = bitmap;
            state.remainingLabel = remainingLabel;
            return state;
        }
    }

    public interface RetryListener {
        void onRetryQrCode(int position);
    }

    private final List<MemberMembershipResponse> memberships = new ArrayList<>();
    private final Map<Integer, QrPageState> stateByPosition = new HashMap<>();
    private RetryListener retryListener;

    public void setRetryListener(RetryListener listener) {
        this.retryListener = listener;
    }

    public void submitMemberships(List<MemberMembershipResponse> newMemberships) {
        memberships.clear();
        stateByPosition.clear();

        if (newMemberships != null) {
            memberships.addAll(newMemberships);
        }

        notifyDataSetChanged();
    }

    public MemberMembershipResponse getMembershipAt(int position) {
        if (position < 0 || position >= memberships.size()) {
            return null;
        }

        return memberships.get(position);
    }

    public void updateState(int position, QrPageState state) {
        stateByPosition.put(position, state);
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public QrViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_qr_membership, parent, false);

        return new QrViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull QrViewHolder holder,
            int position
    ) {
        MemberMembershipResponse membership = memberships.get(position);
        QrPageState state = stateByPosition.get(position);

        holder.bind(membership, state, position, retryListener);
    }

    @Override
    public int getItemCount() {
        return memberships.size();
    }

    static class QrViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtQrPlanName;
        private final TextView txtQrMemberNumber;
        private final ImageView imageViewQrCode;
        private final ProgressBar progressQrLoading;
        private final TextView txtQrError;
        private final TextView txtQrRemaining;
        private final View btnQrRetry;

        QrViewHolder(@NonNull View itemView) {
            super(itemView);

            txtQrPlanName = itemView.findViewById(R.id.txtQrPlanName);
            txtQrMemberNumber = itemView.findViewById(R.id.txtQrMemberNumber);
            imageViewQrCode = itemView.findViewById(R.id.imageViewQrCode);
            progressQrLoading = itemView.findViewById(R.id.progressQrLoading);
            txtQrError = itemView.findViewById(R.id.txtQrError);
            txtQrRemaining = itemView.findViewById(R.id.txtQrRemaining);
            btnQrRetry = itemView.findViewById(R.id.btnQrRetry);
        }

        void bind(
                MemberMembershipResponse membership,
                QrPageState state,
                int position,
                RetryListener retryListener
        ) {
            // Plan name and member number come from the memberships
            // list already fetched for the carousel - the QR endpoint
            // deliberately does not repeat memberNumber.
            txtQrPlanName.setText(
                    valueOrFallback(membership.getPlanName(), "Membership Plan")
            );

            txtQrMemberNumber.setText(
                    String.format(
                            Locale.getDefault(),
                            "Member %s",
                            valueOrFallback(membership.getMemberNumber(), "—")
                    )
            );

            int displayState = state == null
                    ? QrPagerAdapter.STATE_LOADING
                    : state.displayState;

            imageViewQrCode.setVisibility(
                    displayState == QrPagerAdapter.STATE_READY
                            ? View.VISIBLE
                            : View.INVISIBLE
            );

            progressQrLoading.setVisibility(
                    displayState == QrPagerAdapter.STATE_LOADING
                            ? View.VISIBLE
                            : View.GONE
            );

            txtQrError.setVisibility(
                    (displayState == QrPagerAdapter.STATE_ERROR
                            || displayState == QrPagerAdapter.STATE_EXPIRED)
                            ? View.VISIBLE
                            : View.GONE
            );

            btnQrRetry.setVisibility(
                    displayState == QrPagerAdapter.STATE_ERROR
                            ? View.VISIBLE
                            : View.GONE
            );

            txtQrRemaining.setVisibility(
                    displayState == QrPagerAdapter.STATE_READY
                            ? View.VISIBLE
                            : View.GONE
            );

            if (displayState == QrPagerAdapter.STATE_READY
                    && state.qrBitmap != null) {

                imageViewQrCode.setImageBitmap(state.qrBitmap);
                txtQrRemaining.setText(state.remainingLabel);

            } else if (displayState == QrPagerAdapter.STATE_EXPIRED) {

                txtQrError.setText(
                        "This membership has expired and can't generate a QR code."
                );

            } else if (displayState == QrPagerAdapter.STATE_ERROR) {

                String message = state.errorMessage;

                txtQrError.setText(
                        (message == null || message.trim().isEmpty())
                                ? "Unable to load the QR code."
                                : message
                );
            }

            btnQrRetry.setOnClickListener(view -> {
                if (retryListener != null) {
                    retryListener.onRetryQrCode(position);
                }
            });
        }

        private String valueOrFallback(String value, String fallback) {
            String cleanValue = value == null ? "" : value.trim();
            return cleanValue.isEmpty() ? fallback : cleanValue;
        }
    }
}