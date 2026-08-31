package com.example.sporthubandroidmembershipapplicationv1;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.sporthubandroidmembershipapplicationv1.models.MemberMembershipResponse;
import com.example.sporthubandroidmembershipapplicationv1.models.MembershipQrCodeResponse;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;
import com.example.sporthubandroidmembershipapplicationv1.session.MemberSession;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QrFragment extends Fragment {

    private ViewPager2 viewPagerQr;
    private LinearLayout layoutQrDots;

    private ProgressBar progressQrMembershipsLoading;
    private TextView txtQrMembershipsMessage;
    private AppCompatButton btnRetryQrMemberships;

    private View cardCreditTopUp;
    private View cardPurchaseMembership;

    private QrPagerAdapter qrPagerAdapter;

    private Call<List<MemberMembershipResponse>> membershipsCall;
    private Call<MembershipQrCodeResponse> qrCodeCall;

    private Integer loadingMemberMembershipId;

    private final Handler refreshHandler =
            new Handler(Looper.getMainLooper());

    private Runnable countdownRunnable;
    private int activeQrPosition = -1;

    /*
     * Each membership receives its own cached QR image and expiry time.
     * Swiping between memberships no longer generates another token
     * unless that membership's original token has expired.
     */
    private final Map<Integer, CachedQrCode> cachedQrCodes =
            new HashMap<>();

    private static class CachedQrCode {

        private final Bitmap bitmap;
        private final long expiresAtMillis;

        CachedQrCode(
                Bitmap bitmap,
                long expiresAtMillis
        ) {
            this.bitmap = bitmap;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private final ViewPager2.OnPageChangeCallback pageChangeCallback =
            new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    selectPage(position);
                }
            };

    public QrFragment() {
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_qr,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(
                view,
                savedInstanceState
        );

        bindViews(view);
        configureQrPager();
        setShortcutClickListeners();

        btnRetryQrMemberships.setOnClickListener(
                clickedView -> loadMemberships()
        );

        loadMemberships();
    }

    private void bindViews(View view) {
        viewPagerQr =
                view.findViewById(R.id.viewPagerQr);

        layoutQrDots =
                view.findViewById(R.id.layoutQrDots);

        progressQrMembershipsLoading =
                view.findViewById(
                        R.id.progressQrMembershipsLoading
                );

        txtQrMembershipsMessage =
                view.findViewById(
                        R.id.txtQrMembershipsMessage
                );

        btnRetryQrMemberships =
                view.findViewById(
                        R.id.btnRetryQrMemberships
                );

        cardCreditTopUp =
                view.findViewById(
                        R.id.cardCreditTopUp
                );

        cardPurchaseMembership =
                view.findViewById(
                        R.id.cardPurchaseMembership
                );
    }

    private void configureQrPager() {
        qrPagerAdapter =
                new QrPagerAdapter();

        qrPagerAdapter.setRetryListener(
                position ->
                        loadQrForPosition(position)
        );

        viewPagerQr.setAdapter(
                qrPagerAdapter
        );

        viewPagerQr.setOffscreenPageLimit(1);

        viewPagerQr.registerOnPageChangeCallback(
                pageChangeCallback
        );

        RecyclerView qrRecyclerView =
                (RecyclerView) viewPagerQr.getChildAt(0);

        qrRecyclerView.setItemAnimator(null);
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

    private int getLoggedInMemberId() {
        MemberSession memberSession =
                new MemberSession(requireContext());

        return memberSession.getMemberId();
    }

    private void loadMemberships() {
        int memberId =
                getLoggedInMemberId();

        if (memberId <= 0) {
            showMembershipsMessage(
                    "No logged-in member was found.",
                    false
            );

            return;
        }

        showMembershipsLoading();

        membershipsCall =
                ApiClient.getMemberApiService()
                        .getMemberMemberships(memberId);

        membershipsCall.enqueue(
                new Callback<List<MemberMembershipResponse>>() {
                    @Override
                    public void onResponse(
                            Call<List<MemberMembershipResponse>> call,
                            Response<List<MemberMembershipResponse>> response
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

                        showMembershipsMessage(
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

                        showMembershipsMessage(
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
        progressQrMembershipsLoading.setVisibility(
                View.GONE
        );

        btnRetryQrMemberships.setVisibility(
                View.GONE
        );

        stopCountdown();
        cachedQrCodes.clear();

        if (memberships.isEmpty()) {
            qrPagerAdapter.submitMemberships(
                    memberships
            );

            viewPagerQr.setVisibility(
                    View.INVISIBLE
            );

            layoutQrDots.removeAllViews();

            layoutQrDots.setVisibility(
                    View.INVISIBLE
            );

            txtQrMembershipsMessage.setText(
                    "You haven't purchased a membership yet."
            );

            txtQrMembershipsMessage.setVisibility(
                    View.VISIBLE
            );

            return;
        }

        txtQrMembershipsMessage.setVisibility(
                View.GONE
        );

        qrPagerAdapter.submitMemberships(
                memberships
        );

        viewPagerQr.setVisibility(
                View.VISIBLE
        );

        createDots(memberships.size());

        viewPagerQr.setCurrentItem(
                0,
                false
        );

        selectPage(0);
    }

    private void selectPage(int position) {
        updateDots(position);

        MemberMembershipResponse membership =
                qrPagerAdapter.getMembershipAt(
                        position
                );

        if (membership == null) {
            return;
        }

        stopCountdown();
        activeQrPosition = position;

        int memberMembershipId =
                membership.getMemberMembershipId();

        CachedQrCode cachedQrCode =
                cachedQrCodes.get(
                        memberMembershipId
                );

        /*
         * If this membership already has an unexpired QR,
         * display it and calculate the remaining time from
         * its original backend expiry.
         */
        if (cachedQrCode != null
                && cachedQrCode.expiresAtMillis
                > System.currentTimeMillis()) {

            showCachedQrCode(
                    position,
                    cachedQrCode
            );

            startCountdown();
            return;
        }

        /*
         * Its cached token is missing or has genuinely
         * expired, so a new token may now be requested.
         */
        cachedQrCodes.remove(
                memberMembershipId
        );

        loadQrForPosition(position);
    }

    private void loadQrForPosition(int position) {
        MemberMembershipResponse membership =
                qrPagerAdapter.getMembershipAt(
                        position
                );

        if (membership == null) {
            return;
        }

        int memberMembershipId =
                membership.getMemberMembershipId();

        /*
         * Avoid issuing the same request twice when
         * ViewPager selects the initial page.
         */
        if (loadingMemberMembershipId != null
                && loadingMemberMembershipId
                == memberMembershipId
                && qrCodeCall != null
                && !qrCodeCall.isCanceled()) {

            return;
        }

        stopCountdown();
        activeQrPosition = position;

        if ("Expired".equalsIgnoreCase(
                membership.getStatus()
        )) {
            qrPagerAdapter.updateState(
                    position,
                    QrPagerAdapter.QrPageState.expired()
            );

            return;
        }

        qrPagerAdapter.updateState(
                position,
                QrPagerAdapter.QrPageState.loading()
        );

        if (qrCodeCall != null
                && !qrCodeCall.isCanceled()) {

            qrCodeCall.cancel();
        }

        int memberId =
                getLoggedInMemberId();

        if (memberId <= 0) {
            qrPagerAdapter.updateState(
                    position,
                    QrPagerAdapter.QrPageState.error(
                            "No logged-in member was found."
                    )
            );

            return;
        }

        loadingMemberMembershipId =
                memberMembershipId;

        qrCodeCall =
                ApiClient.getMemberApiService()
                        .getMembershipQrCode(
                                memberId,
                                memberMembershipId
                        );

        qrCodeCall.enqueue(
                new Callback<MembershipQrCodeResponse>() {
                    @Override
                    public void onResponse(
                            Call<MembershipQrCodeResponse> call,
                            Response<MembershipQrCodeResponse> response
                    ) {
                        if (loadingMemberMembershipId != null
                                && loadingMemberMembershipId
                                == memberMembershipId) {

                            loadingMemberMembershipId = null;
                        }

                        if (!isAdded()
                                || getView() == null
                                || viewPagerQr.getCurrentItem()
                                != position) {

                            return;
                        }

                        MembershipQrCodeResponse qr =
                                response.body();

                        if (response.isSuccessful()
                                && qr != null
                                && qr.getQrToken() != null
                                && !qr.getQrToken()
                                .trim()
                                .isEmpty()) {

                            applyQrResult(
                                    position,
                                    qr
                            );

                            return;
                        }

                        if (response.code() == 409) {
                            qrPagerAdapter.updateState(
                                    position,
                                    QrPagerAdapter
                                            .QrPageState
                                            .expired()
                            );

                            return;
                        }

                        qrPagerAdapter.updateState(
                                position,
                                QrPagerAdapter.QrPageState.error(
                                        "Unable to load the QR code."
                                )
                        );
                    }

                    @Override
                    public void onFailure(
                            Call<MembershipQrCodeResponse> call,
                            Throwable throwable
                    ) {
                        if (loadingMemberMembershipId != null
                                && loadingMemberMembershipId
                                == memberMembershipId) {

                            loadingMemberMembershipId = null;
                        }

                        if (call.isCanceled()
                                || !isAdded()
                                || getView() == null
                                || viewPagerQr.getCurrentItem()
                                != position) {

                            return;
                        }

                        qrPagerAdapter.updateState(
                                position,
                                QrPagerAdapter.QrPageState.error(
                                        "Unable to connect to the server."
                                )
                        );
                    }
                }
        );
    }

    private void applyQrResult(
            int position,
            MembershipQrCodeResponse qr
    ) {
        Bitmap bitmap =
                generateQrCodeBitmap(
                        qr.getQrToken()
                );

        if (bitmap == null) {
            qrPagerAdapter.updateState(
                    position,
                    QrPagerAdapter.QrPageState.error(
                            "Unable to generate the QR code."
                    )
            );

            return;
        }

        Long expiresAtMillis =
                parseIsoUtcToMillis(
                        qr.getExpiresAtUtc()
                );

        if (expiresAtMillis == null) {
            int validitySeconds =
                    qr.getValiditySeconds() > 0
                            ? qr.getValiditySeconds()
                            : 60;

            expiresAtMillis =
                    System.currentTimeMillis()
                            + (validitySeconds * 1000L);
        }

        activeQrPosition = position;

        MemberMembershipResponse membership =
                qrPagerAdapter.getMembershipAt(
                        position
                );

        if (membership == null) {
            return;
        }

        CachedQrCode cachedQrCode =
                new CachedQrCode(
                        bitmap,
                        expiresAtMillis
                );

        cachedQrCodes.put(
                membership.getMemberMembershipId(),
                cachedQrCode
        );

        showCachedQrCode(
                position,
                cachedQrCode
        );

        startCountdown();
    }

    private void showCachedQrCode(
            int position,
            CachedQrCode cachedQrCode
    ) {
        long remainingSeconds =
                Math.max(
                        0,
                        (
                                cachedQrCode.expiresAtMillis
                                        - System.currentTimeMillis()
                                        + 999
                        ) / 1000
                );

        qrPagerAdapter.updateState(
                position,
                QrPagerAdapter.QrPageState.ready(
                        cachedQrCode.bitmap,
                        formatRemainingLabel(
                                remainingSeconds
                        )
                )
        );
    }

    private void startCountdown() {
        countdownRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded()
                                || getView() == null
                                || activeQrPosition < 0) {

                            return;
                        }

                        MemberMembershipResponse membership =
                                qrPagerAdapter.getMembershipAt(
                                        activeQrPosition
                                );

                        if (membership == null) {
                            return;
                        }

                        int memberMembershipId =
                                membership
                                        .getMemberMembershipId();

                        CachedQrCode cachedQrCode =
                                cachedQrCodes.get(
                                        memberMembershipId
                                );

                        if (cachedQrCode == null) {
                            loadQrForPosition(
                                    activeQrPosition
                            );

                            return;
                        }

                        long remainingMillis =
                                cachedQrCode.expiresAtMillis
                                        - System.currentTimeMillis();

                        if (remainingMillis <= 0) {
                            cachedQrCodes.remove(
                                    memberMembershipId
                            );

                            loadQrForPosition(
                                    activeQrPosition
                            );

                            return;
                        }

                        showCachedQrCode(
                                activeQrPosition,
                                cachedQrCode
                        );

                        refreshHandler.postDelayed(
                                this,
                                1000
                        );
                    }
                };

        refreshHandler.postDelayed(
                countdownRunnable,
                1000
        );
    }

    private void stopCountdown() {
        if (countdownRunnable != null) {
            refreshHandler.removeCallbacks(
                    countdownRunnable
            );

            countdownRunnable = null;
        }
    }

    private String formatRemainingLabel(
            long remainingSeconds
    ) {
        return String.format(
                Locale.getDefault(),
                "Refreshing in %ds...",
                remainingSeconds
        );
    }

    private Long parseIsoUtcToMillis(
            String value
    ) {
        String cleanValue =
                value == null
                        ? ""
                        : value.trim();

        if (cleanValue.isEmpty()) {
            return null;
        }

        try {
            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss'Z'",
                            Locale.US
                    );

            format.setTimeZone(
                    TimeZone.getTimeZone("UTC")
            );

            format.setLenient(false);

            Date parsed =
                    format.parse(cleanValue);

            return parsed == null
                    ? null
                    : parsed.getTime();

        } catch (ParseException exception) {
            return null;
        }
    }

    private Bitmap generateQrCodeBitmap(
            String text
    ) {
        QRCodeWriter writer =
                new QRCodeWriter();

        try {
            Map<EncodeHintType, Object> hints =
                    new HashMap<>();

            hints.put(
                    EncodeHintType.MARGIN,
                    1
            );

            BitMatrix bitMatrix =
                    writer.encode(
                            text,
                            BarcodeFormat.QR_CODE,
                            512,
                            512,
                            hints
                    );

            int width =
                    bitMatrix.getWidth();

            int height =
                    bitMatrix.getHeight();

            Bitmap bitmap =
                    Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.RGB_565
                    );

            int[] pixels =
                    new int[width * height];

            for (int y = 0; y < height; y++) {
                int rowOffset =
                        y * width;

                for (int x = 0; x < width; x++) {
                    pixels[rowOffset + x] =
                            bitMatrix.get(x, y)
                                    ? Color.BLACK
                                    : Color.WHITE;
                }
            }

            bitmap.setPixels(
                    pixels,
                    0,
                    width,
                    0,
                    0,
                    width,
                    height
            );

            return bitmap;

        } catch (WriterException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private void createDots(
            int membershipCount
    ) {
        layoutQrDots.removeAllViews();

        for (int index = 0;
             index < membershipCount;
             index++) {

            final int pagePosition =
                    index;

            View dot =
                    new View(requireContext());

            int dotSize =
                    dpToPx(10);

            int dotMargin =
                    dpToPx(5);

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

            dot.setLayoutParams(
                    layoutParams
            );

            dot.setContentDescription(
                    String.format(
                            Locale.getDefault(),
                            "Membership %d of %d",
                            index + 1,
                            membershipCount
                    )
            );

            dot.setOnClickListener(
                    clickedView ->
                            viewPagerQr.setCurrentItem(
                                    pagePosition,
                                    true
                            )
            );

            layoutQrDots.addView(dot);
        }

        layoutQrDots.setVisibility(
                View.VISIBLE
        );
    }

    private void updateDots(
            int selectedPosition
    ) {
        if (layoutQrDots == null) {
            return;
        }

        int dotCount =
                layoutQrDots.getChildCount();

        for (int index = 0;
             index < dotCount;
             index++) {

            View dot =
                    layoutQrDots.getChildAt(index);

            dot.setBackgroundResource(
                    index == selectedPosition
                            ? R.drawable
                            .membership_dot_active
                            : R.drawable
                            .membership_dot_inactive
            );
        }
    }

    private void showMembershipsLoading() {
        viewPagerQr.setVisibility(
                View.INVISIBLE
        );

        layoutQrDots.setVisibility(
                View.INVISIBLE
        );

        txtQrMembershipsMessage.setText(
                "Loading your memberships..."
        );

        txtQrMembershipsMessage.setVisibility(
                View.GONE
        );

        btnRetryQrMemberships.setVisibility(
                View.GONE
        );

        progressQrMembershipsLoading.setVisibility(
                View.VISIBLE
        );
    }

    private void showMembershipsMessage(
            String message,
            boolean showRetry
    ) {
        viewPagerQr.setVisibility(
                View.INVISIBLE
        );

        layoutQrDots.setVisibility(
                View.INVISIBLE
        );

        progressQrMembershipsLoading.setVisibility(
                View.GONE
        );

        txtQrMembershipsMessage.setText(
                message
        );

        txtQrMembershipsMessage.setVisibility(
                View.VISIBLE
        );

        btnRetryQrMemberships.setVisibility(
                showRetry
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private int dpToPx(int dp) {
        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                dp * density
        );
    }

    @Override
    public void onDestroyView() {
        stopCountdown();

        viewPagerQr.unregisterOnPageChangeCallback(
                pageChangeCallback
        );

        viewPagerQr.setAdapter(null);

        if (membershipsCall != null
                && !membershipsCall.isCanceled()) {

            membershipsCall.cancel();
        }

        if (qrCodeCall != null
                && !qrCodeCall.isCanceled()) {

            qrCodeCall.cancel();
        }

        qrPagerAdapter = null;
        activeQrPosition = -1;
        loadingMemberMembershipId = null;

        cachedQrCodes.clear();

        super.onDestroyView();
    }
}