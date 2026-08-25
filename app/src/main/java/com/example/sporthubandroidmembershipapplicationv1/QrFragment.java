package com.example.sporthubandroidmembershipapplicationv1;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sporthubandroidmembershipapplicationv1.models.MemberMembershipResponse;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;
import com.example.sporthubandroidmembershipapplicationv1.session.MemberSession;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QrFragment extends Fragment {

    private ImageView imageViewQrCode;
    private TextView textViewTimer;
    private TextView textViewReferenceCode;
    private TextView textViewTitle;

    private View cardCreditTopUp;
    private View cardPurchaseMembership;

    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private long lastWindowGenerated = -1;

    private Call<MemberMembershipResponse> membershipCall;

    public QrFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imageViewQrCode = view.findViewById(R.id.imageViewQrCode);
        textViewTimer = view.findViewById(R.id.textViewTimer);
        textViewReferenceCode = view.findViewById(R.id.textViewReferenceCode);
        textViewTitle = view.findViewById(R.id.textViewTitle);

        cardCreditTopUp = view.findViewById(R.id.cardCreditTopUp);
        cardPurchaseMembership =
                view.findViewById(R.id.cardPurchaseMembership);

        setShortcutClickListeners();

        startSystemClockSync();
        loadMembershipType();
    }

    private void setShortcutClickListeners() {
        cardCreditTopUp.setOnClickListener(view -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity())
                        .openProfileAndShowTopUp();
            }
        });

        cardPurchaseMembership.setOnClickListener(view -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity())
                        .openMembershipFromQr();
            }
        });
    }

    private void loadMembershipType() {

        MemberSession memberSession =
                new MemberSession(requireContext());

        int memberId = memberSession.getMemberId();

        if (memberId <= 0) {
            // No logged-in member found — leave the default
            // "Your Membership QR" label in place.
            return;
        }

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

                            String planName = membership.getPlanName();

                            if (planName != null
                                    && !planName.trim().isEmpty()) {

                                textViewTitle.setText(
                                        String.format(
                                                Locale.getDefault(),
                                                "%s Plan",
                                                planName.trim()
                                        )
                                );
                            }
                        }

                        // On 404 / no membership, keep the default label.
                    }

                    @Override
                    public void onFailure(
                            Call<MemberMembershipResponse> call,
                            Throwable throwable
                    ) {
                        // Network/server error — keep the default label
                        // so the gate code still works while offline.
                    }
                }
        );
    }

    private void startSystemClockSync() {

        refreshRunnable = new Runnable() {
            @Override
            public void run() {

                long currentTimeMillis = System.currentTimeMillis();
                long secondsPassedInMinute = (currentTimeMillis / 1000) % 60;
                long secondsLeft = 60 - secondsPassedInMinute;

                // Sync QR with the current minute
                long currentMinuteWindow = currentTimeMillis / 60000;

                updateQrForWindow(currentMinuteWindow);

                textViewTimer.setText(
                        String.format(
                                Locale.getDefault(),
                                "Updating in %ds...",
                                secondsLeft
                        )
                );

                refreshHandler.postDelayed(this, 1000);
            }
        };

        refreshHandler.post(refreshRunnable);
    }

    private void updateQrForWindow(long windowId) {

        if (windowId != lastWindowGenerated) {

            lastWindowGenerated = windowId;

            String uniqueData = "MEMBER-99821_" + windowId;
            String refCode = "REF: 99821-" + (windowId % 10000);

            textViewReferenceCode.setText(refCode);

            Bitmap bitmap = generateQrCodeBitmap(uniqueData);

            if (bitmap != null) {
                imageViewQrCode.setImageBitmap(bitmap);
            }
        }
    }

    private Bitmap generateQrCodeBitmap(String text) {

        QRCodeWriter writer = new QRCodeWriter();

        try {

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = writer.encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    512,
                    512,
                    hints
            );

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();

            Bitmap bitmap = Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.RGB_565
            );

            for (int x = 0; x < width; x++) {

                for (int y = 0; y < height; y++) {

                    bitmap.setPixel(
                            x,
                            y,
                            bitMatrix.get(x, y)
                                    ? Color.BLACK
                                    : Color.WHITE
                    );
                }
            }

            return bitmap;

        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }

        if (membershipCall != null && !membershipCall.isCanceled()) {
            membershipCall.cancel();
        }
    }
}