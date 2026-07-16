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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import com.google.zxing.EncodeHintType;
import java.util.HashMap;
import java.util.Map;

import java.util.Locale;

public class QrFragment extends Fragment {

    private ImageView imageViewQrCode;
    private TextView textViewTimer;
    private TextView textViewReferenceCode;
    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private long lastWindowGenerated = -1;

    public QrFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI CONSISTENCY FIX: Force activity background to white/light gray
        if (getActivity() != null) {
            View mainContainer = getActivity().findViewById(R.id.main);
            if (mainContainer != null) {
                mainContainer.setBackgroundColor(Color.parseColor("#E9E9E9"));
            }
        }

        imageViewQrCode = view.findViewById(R.id.imageViewQrCode);
        textViewTimer = view.findViewById(R.id.textViewTimer);
        textViewReferenceCode = view.findViewById(R.id.textViewReferenceCode);

        startSystemClockSync();
    }

    private void startSystemClockSync() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                long currentTimeMillis = System.currentTimeMillis();
                long secondsPassedInMinute = (currentTimeMillis / 1000) % 60;
                long secondsLeft = 60 - secondsPassedInMinute;

                // Sync QR window with the actual current minute
                long currentMinuteWindow = currentTimeMillis / 60000;
                updateQrForWindow(currentMinuteWindow);

                textViewTimer.setText(String.format(Locale.getDefault(), "Updating in %ds...", secondsLeft));
                refreshHandler.postDelayed(this, 1000);
            }
        };
        refreshHandler.post(refreshRunnable);
    }

    private void updateQrForWindow(long windowId) {
        if (windowId != lastWindowGenerated) {
            lastWindowGenerated = windowId;

            // Generate data (e.g., MEMBER-ID + TIME-WINDOW)
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
            hints.put(EncodeHintType.MARGIN, 1); // removes the quiet zone

            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512, hints);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
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
    }
}