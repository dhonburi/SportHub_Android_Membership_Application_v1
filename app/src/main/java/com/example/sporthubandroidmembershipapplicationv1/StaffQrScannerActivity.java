package com.example.sporthubandroidmembershipapplicationv1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.sporthubandroidmembershipapplicationv1.models.GateEntryRequest;
import com.example.sporthubandroidmembershipapplicationv1.models.GateEntryResponse;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;
import com.example.sporthubandroidmembershipapplicationv1.session.MemberSession;
import com.google.android.material.button.MaterialButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StaffQrScannerActivity extends AppCompatActivity {

    private TextView btnBackScanner;
    private TextView txtScannerResultTitle;
    private TextView txtScannerResultCode;
    private TextView txtScannerResultMessage;
    private TextView txtScannerResultDetails;

    private ProgressBar progressScannerValidation;

    private MaterialButton btnStartScanner;
    private MaterialButton btnRetryValidation;
    private MaterialButton btnScanAnother;
    private MaterialButton btnOpenPermissionSettings;

    private Call<GateEntryResponse> gateEntryCall;
    private String lastScannedToken;
    private boolean gateProcessingInProgress;

    private final ActivityResultLauncher<ScanOptions>
            scannerLauncher = registerForActivityResult(
            new ScanContract(),
            this::handleScanResult
    );

    private final ActivityResultLauncher<String>
            cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            this::handleCameraPermissionResult
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MemberSession memberSession =
                new MemberSession(this);

        if (!memberSession.isAdmin()) {
            Toast.makeText(
                    this,
                    "Administrator access is required.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        if (memberSession.getStaffAccessToken() == null
                || memberSession.getStaffAccessToken().trim().isEmpty()) {
            redirectToStaffLogin("Sign in again to use the gate scanner.");
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_staff_qr_scanner);

        configureSystemBars();
        bindViews();
        applySystemBarInsets();
        configureClickListeners();
        showReadyState();
    }

    private void bindViews() {
        btnBackScanner = findViewById(R.id.btnBackScanner);

        txtScannerResultTitle = findViewById(
                R.id.txtScannerResultTitle
        );

        txtScannerResultCode = findViewById(
                R.id.txtScannerResultCode
        );

        txtScannerResultMessage = findViewById(
                R.id.txtScannerResultMessage
        );

        txtScannerResultDetails = findViewById(
                R.id.txtScannerResultDetails
        );

        progressScannerValidation = findViewById(
                R.id.progressScannerValidation
        );

        btnStartScanner = findViewById(
                R.id.btnStartScanner
        );

        btnRetryValidation = findViewById(
                R.id.btnRetryValidation
        );

        btnScanAnother = findViewById(
                R.id.btnScanAnother
        );

        btnOpenPermissionSettings = findViewById(
                R.id.btnOpenPermissionSettings
        );
    }

    private void configureClickListeners() {
        btnBackScanner.setOnClickListener(view -> finish());

        btnStartScanner.setOnClickListener(
                view -> startScannerFlow()
        );

        btnRetryValidation.setOnClickListener(
                view -> retryLastGateEntry()
        );

        btnScanAnother.setOnClickListener(view -> {
            clearPendingGateEntry();
            showReadyState();
            startScannerFlow();
        });

        btnOpenPermissionSettings.setOnClickListener(
                view -> openApplicationSettings()
        );
    }

    private void startScannerFlow() {
        if (gateProcessingInProgress) {
            return;
        }

        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_ANY
        )) {
            showScannerError(
                    "Camera unavailable",
                    "This device does not have an available camera."
            );

            return;
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {
            launchScanner();

            return;
        }

        cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
        );
    }

    private void handleCameraPermissionResult(
            boolean isGranted
    ) {
        if (isGranted) {
            launchScanner();

            return;
        }

        txtScannerResultTitle.setText(
                "Camera permission denied"
        );

        txtScannerResultTitle.setTextColor(
                Color.parseColor("#A52A2A")
        );

        txtScannerResultCode.setText(
                "PERMISSION_REQUIRED"
        );

        txtScannerResultCode.setVisibility(View.VISIBLE);

        txtScannerResultMessage.setText(
                "Camera permission is required to scan a SportHub QR code."
        );

        txtScannerResultDetails.setVisibility(View.GONE);
        progressScannerValidation.setVisibility(View.GONE);
        btnStartScanner.setVisibility(View.VISIBLE);
        btnRetryValidation.setVisibility(View.GONE);
        btnScanAnother.setVisibility(View.GONE);

        boolean canAskAgain =
                shouldShowRequestPermissionRationale(
                        Manifest.permission.CAMERA
                );

        btnOpenPermissionSettings.setVisibility(
                canAskAgain ? View.GONE : View.VISIBLE
        );
    }

    private void launchScanner() {
        ScanOptions options = new ScanOptions();

        options.setDesiredBarcodeFormats(
                ScanOptions.QR_CODE
        );

        options.setPrompt(
                "Place the current SportHub QR inside the frame"
        );

        options.setBeepEnabled(false);
        options.setOrientationLocked(false);
        options.setBarcodeImageEnabled(false);

        scannerLauncher.launch(options);
    }

    private void handleScanResult(
            ScanIntentResult scanResult
    ) {
        String contents = scanResult.getContents();

        if (contents == null) {
            showScannerError(
                    "Scan cancelled",
                    "No QR code was submitted for gate processing."
            );

            return;
        }

        String token = contents.trim();

        if (token.isEmpty()) {
            showScannerError(
                    "Invalid QR code",
                    "The scanned QR code did not contain gate-entry information."
            );

            return;
        }

        lastScannedToken = token;
        processGateEntry(token);
    }

    private void processGateEntry(String token) {
        if (gateProcessingInProgress) {
            return;
        }

        String staffAccessToken =
                new MemberSession(this).getStaffAccessToken();

        if (staffAccessToken == null
                || staffAccessToken.trim().isEmpty()) {
            redirectToStaffLogin("Sign in again to use the gate scanner.");
            return;
        }

        gateProcessingInProgress = true;
        showGateProcessingLoading();

        GateEntryRequest request =
                new GateEntryRequest(token);

        gateEntryCall = ApiClient
                .getGateEntryApiService()
                .processGateEntry("Bearer " + staffAccessToken, request);

        gateEntryCall.enqueue(
                new Callback<GateEntryResponse>() {
                    @Override
                    public void onResponse(
                            Call<GateEntryResponse> call,
                            Response<GateEntryResponse> response
                    ) {
                        gateProcessingInProgress = false;

                        if (response.code() == 401
                                || response.code() == 403) {
                            redirectToStaffLogin(
                                    "Staff access expired or was removed. Sign in again."
                            );
                            return;
                        }

                        GateEntryResponse result =
                                response.body();

                        if (!response.isSuccessful()
                                || result == null) {
                            showNetworkError(
                                    "The gate-processing API returned HTTP "
                                            + response.code()
                                            + "."
                            );

                            return;
                        }

                        showGateEntryResult(result);
                    }

                    @Override
                    public void onFailure(
                            Call<GateEntryResponse> call,
                            Throwable throwable
                    ) {
                        gateProcessingInProgress = false;

                        if (call.isCanceled()) {
                            return;
                        }

                        showNetworkError(
                                "The entry outcome could not be confirmed. Check the connection and safely retry the same QR token."
                        );
                    }
                }
        );
    }

    private void redirectToStaffLogin(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showReadyState() {
        txtScannerResultTitle.setText("Ready to scan");

        txtScannerResultTitle.setTextColor(
                Color.parseColor("#111111")
        );

        txtScannerResultCode.setVisibility(View.GONE);

        txtScannerResultMessage.setText(
                "Open the camera and scan a member's current SportHub QR code to simulate gate entry."
        );

        txtScannerResultDetails.setVisibility(View.GONE);
        progressScannerValidation.setVisibility(View.GONE);
        btnStartScanner.setVisibility(View.VISIBLE);
        btnRetryValidation.setVisibility(View.GONE);
        btnScanAnother.setVisibility(View.GONE);
        btnOpenPermissionSettings.setVisibility(View.GONE);
    }

    private void showGateProcessingLoading() {
        txtScannerResultTitle.setText(
                "Processing gate entry"
        );

        txtScannerResultTitle.setTextColor(
                Color.parseColor("#111111")
        );

        txtScannerResultCode.setVisibility(View.GONE);

        txtScannerResultMessage.setText(
                "Checking the current account record and applying the correct entry rule..."
        );

        txtScannerResultDetails.setVisibility(View.GONE);
        progressScannerValidation.setVisibility(View.VISIBLE);
        btnStartScanner.setVisibility(View.GONE);
        btnRetryValidation.setVisibility(View.GONE);
        btnScanAnother.setVisibility(View.GONE);
        btnOpenPermissionSettings.setVisibility(View.GONE);
    }

    private void showGateEntryResult(
            GateEntryResponse result
    ) {
        progressScannerValidation.setVisibility(View.GONE);

        boolean isApproved = result.isApproved();
        boolean isDuplicate = result.isDuplicate();

        txtScannerResultTitle.setText(
                isApproved
                        ? "Access approved"
                        : isDuplicate
                        ? "Entry already processed"
                        : "Access denied"
        );

        txtScannerResultTitle.setTextColor(
                Color.parseColor(
                        isApproved
                                ? "#1B7F3A"
                                : isDuplicate
                                ? "#A66A00"
                                : "#A52A2A"
                )
        );

        String resultCode = safeText(
                result.getResultCode(),
                isApproved
                        ? "ENTRY_PROCESSED"
                        : isDuplicate
                        ? "ALREADY_PROCESSED"
                        : "DENIED"
        );

        txtScannerResultCode.setText(resultCode);
        txtScannerResultCode.setVisibility(View.VISIBLE);

        txtScannerResultMessage.setText(
                safeText(
                        result.getMessage(),
                        isApproved
                                ? "The simulated gate entry was completed."
                                : isDuplicate
                                ? "No additional deduction was made."
                                : "The simulated gate denied entry."
                )
        );

        String details = buildGateEntryDetails(result);

        if (details.isEmpty()) {
            txtScannerResultDetails.setVisibility(View.GONE);
        } else {
            txtScannerResultDetails.setText(details);
            txtScannerResultDetails.setVisibility(View.VISIBLE);
        }

        btnStartScanner.setVisibility(View.GONE);
        btnRetryValidation.setVisibility(View.GONE);
        btnScanAnother.setVisibility(View.VISIBLE);
        btnOpenPermissionSettings.setVisibility(View.GONE);
    }

    private String buildGateEntryDetails(
            GateEntryResponse result
    ) {
        StringBuilder details = new StringBuilder();

        appendDetail(
                details,
                "Access method",
                formatAccessType(result.getAccessType())
        );

        appendDetail(
                details,
                "Member number",
                result.getMemberNumber()
        );

        appendDetail(
                details,
                "Membership plan",
                result.getPlanName()
        );

        appendDetail(
                details,
                "Membership status",
                result.getMembershipStatus()
        );

        if (result.getRemainingEntries() != null) {
            appendDetail(
                    details,
                    "Remaining entries",
                    String.valueOf(
                            result.getRemainingEntries()
                    )
            );
        }

        if (result.getAmountCharged() != null
                && result.getAmountCharged() > 0) {
            appendDetail(
                    details,
                    "Amount charged",
                    String.format(
                            Locale.US,
                            "%.2f %s",
                            result.getAmountCharged(),
                            safeText(result.getCurrency(), "NZD")
                    )
            );
        }

        if (result.getBalance() != null
                && "BALANCE".equalsIgnoreCase(
                result.getAccessType()
        )) {
            appendDetail(
                    details,
                    "Updated balance",
                    String.format(
                            Locale.US,
                            "%.2f %s",
                            result.getBalance(),
                            safeText(result.getCurrency(), "NZD")
                    )
            );
        }

        appendDetail(
                details,
                "Processing ID",
                result.getProcessingId()
        );

        return details.toString();
    }

    private String formatAccessType(String accessType) {
        if ("STANDARD_MEMBERSHIP".equalsIgnoreCase(accessType)) {
            return "Standard membership";
        }

        if ("SPORTS_PASSCARD".equalsIgnoreCase(accessType)) {
            return "Sports Passcard";
        }

        if ("BALANCE".equalsIgnoreCase(accessType)) {
            return "Balance access";
        }

        return accessType;
    }

    private void appendDetail(
            StringBuilder details,
            String label,
            String value
    ) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        if (details.length() > 0) {
            details.append('\n');
        }

        details.append(label)
                .append(": ")
                .append(value.trim());
    }

    private void showScannerError(
            String title,
            String message
    ) {
        txtScannerResultTitle.setText(title);

        txtScannerResultTitle.setTextColor(
                Color.parseColor("#A52A2A")
        );

        txtScannerResultCode.setText("NOT_SUBMITTED");
        txtScannerResultCode.setVisibility(View.VISIBLE);
        txtScannerResultMessage.setText(message);
        txtScannerResultDetails.setVisibility(View.GONE);
        progressScannerValidation.setVisibility(View.GONE);
        btnStartScanner.setVisibility(View.VISIBLE);
        btnRetryValidation.setVisibility(View.GONE);
        btnScanAnother.setVisibility(View.GONE);
        btnOpenPermissionSettings.setVisibility(View.GONE);
    }

    private void showNetworkError(String message) {
        txtScannerResultTitle.setText(
                "Connection error"
        );

        txtScannerResultTitle.setTextColor(
                Color.parseColor("#A52A2A")
        );

        txtScannerResultCode.setText(
                "API_CONNECTION_ERROR"
        );

        txtScannerResultCode.setVisibility(View.VISIBLE);
        txtScannerResultMessage.setText(message);
        txtScannerResultDetails.setVisibility(View.GONE);
        progressScannerValidation.setVisibility(View.GONE);
        btnStartScanner.setVisibility(View.GONE);
        btnRetryValidation.setVisibility(View.VISIBLE);
        btnScanAnother.setVisibility(View.VISIBLE);
        btnOpenPermissionSettings.setVisibility(View.GONE);
    }

    private void retryLastGateEntry() {
        if (lastScannedToken == null
                || lastScannedToken.trim().isEmpty()) {
            startScannerFlow();

            return;
        }

        processGateEntry(lastScannedToken);
    }

    private void clearPendingGateEntry() {
        if (gateEntryCall != null
                && !gateEntryCall.isCanceled()) {
            gateEntryCall.cancel();
        }

        gateEntryCall = null;
        gateProcessingInProgress = false;
        lastScannedToken = null;
    }

    private void openApplicationSettings() {
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        );

        intent.setData(
                Uri.fromParts(
                        "package",
                        getPackageName(),
                        null
                )
        );

        startActivity(intent);
    }

    private String safeText(
            String value,
            String fallback
    ) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    private void applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.staffQrScannerMain),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }

    private void configureSystemBars() {
        Window window = getWindow();

        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        window,
                        window.getDecorView()
                );

        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    @Override
    protected void onDestroy() {
        clearPendingGateEntry();
        super.onDestroy();
    }
}
