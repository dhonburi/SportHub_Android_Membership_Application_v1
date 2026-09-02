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

import com.example.sporthubandroidmembershipapplicationv1.models.MembershipQrValidationRequest;
import com.example.sporthubandroidmembershipapplicationv1.models.MembershipQrValidationResponse;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

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

    private Call<MembershipQrValidationResponse> validationCall;
    private String lastScannedToken;
    private boolean validationInProgress;

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
                view -> retryLastValidation()
        );

        btnScanAnother.setOnClickListener(view -> {
            clearPendingValidation();
            showReadyState();
            startScannerFlow();
        });

        btnOpenPermissionSettings.setOnClickListener(
                view -> openApplicationSettings()
        );
    }

    private void startScannerFlow() {
        if (validationInProgress) {
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
                "Camera permission is required to scan a membership QR code."
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
                "Place the SportHub membership QR inside the frame"
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
                    "No QR code was submitted for validation."
            );

            return;
        }

        String token = contents.trim();

        if (token.isEmpty()) {
            showScannerError(
                    "Invalid QR code",
                    "The scanned QR code did not contain validation information."
            );

            return;
        }

        lastScannedToken = token;
        validateToken(token);
    }

    private void validateToken(String token) {
        if (validationInProgress) {
            return;
        }

        validationInProgress = true;
        showValidationLoading();

        MembershipQrValidationRequest request =
                new MembershipQrValidationRequest(token);

        validationCall = ApiClient
                .getMembershipQrApiService()
                .validateMembershipQr(request);

        validationCall.enqueue(
                new Callback<MembershipQrValidationResponse>() {
                    @Override
                    public void onResponse(
                            Call<MembershipQrValidationResponse> call,
                            Response<MembershipQrValidationResponse> response
                    ) {
                        validationInProgress = false;

                        MembershipQrValidationResponse result =
                                response.body();

                        if (!response.isSuccessful()
                                || result == null) {
                            showNetworkError(
                                    "The validation API returned HTTP "
                                            + response.code()
                                            + "."
                            );

                            return;
                        }

                        showValidationResult(result);
                    }

                    @Override
                    public void onFailure(
                            Call<MembershipQrValidationResponse> call,
                            Throwable throwable
                    ) {
                        validationInProgress = false;

                        if (call.isCanceled()) {
                            return;
                        }

                        showNetworkError(
                                "Unable to connect to the SportHub API. Check the connection and retry."
                        );
                    }
                }
        );
    }

    private void showReadyState() {
        txtScannerResultTitle.setText("Ready to scan");

        txtScannerResultTitle.setTextColor(
                Color.parseColor("#111111")
        );

        txtScannerResultCode.setVisibility(View.GONE);

        txtScannerResultMessage.setText(
                "Open the camera and scan a member's current SportHub membership QR code."
        );

        txtScannerResultDetails.setVisibility(View.GONE);
        progressScannerValidation.setVisibility(View.GONE);
        btnStartScanner.setVisibility(View.VISIBLE);
        btnRetryValidation.setVisibility(View.GONE);
        btnScanAnother.setVisibility(View.GONE);
        btnOpenPermissionSettings.setVisibility(View.GONE);
    }

    private void showValidationLoading() {
        txtScannerResultTitle.setText(
                "Validating QR code"
        );

        txtScannerResultTitle.setTextColor(
                Color.parseColor("#111111")
        );

        txtScannerResultCode.setVisibility(View.GONE);

        txtScannerResultMessage.setText(
                "Checking the token and current membership record..."
        );

        txtScannerResultDetails.setVisibility(View.GONE);
        progressScannerValidation.setVisibility(View.VISIBLE);
        btnStartScanner.setVisibility(View.GONE);
        btnRetryValidation.setVisibility(View.GONE);
        btnScanAnother.setVisibility(View.GONE);
        btnOpenPermissionSettings.setVisibility(View.GONE);
    }

    private void showValidationResult(
            MembershipQrValidationResponse result
    ) {
        progressScannerValidation.setVisibility(View.GONE);

        boolean isApproved = result.isApproved();

        txtScannerResultTitle.setText(
                isApproved
                        ? "Access approved"
                        : "Access denied"
        );

        txtScannerResultTitle.setTextColor(
                Color.parseColor(
                        isApproved
                                ? "#1B7F3A"
                                : "#A52A2A"
                )
        );

        String resultCode = safeText(
                result.getResultCode(),
                isApproved ? "VALID" : "DENIED"
        );

        txtScannerResultCode.setText(resultCode);
        txtScannerResultCode.setVisibility(View.VISIBLE);

        txtScannerResultMessage.setText(
                safeText(
                        result.getMessage(),
                        isApproved
                                ? "Membership validation succeeded."
                                : "Membership validation was denied."
                )
        );

        String details = isApproved
                ? buildApprovedDetails(result)
                : "";

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

    private String buildApprovedDetails(
            MembershipQrValidationResponse result
    ) {
        StringBuilder details = new StringBuilder();

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

        if (details.length() == 0
                && "BALANCE".equalsIgnoreCase(
                result.getAccessType()
        )) {
            appendDetail(
                    details,
                    "Access type",
                    "Balance Access"
            );
        }

        return details.toString();
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

    private void retryLastValidation() {
        if (lastScannedToken == null
                || lastScannedToken.trim().isEmpty()) {
            startScannerFlow();

            return;
        }

        validateToken(lastScannedToken);
    }

    private void clearPendingValidation() {
        if (validationCall != null
                && !validationCall.isCanceled()) {
            validationCall.cancel();
        }

        validationCall = null;
        validationInProgress = false;
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
        clearPendingValidation();
        super.onDestroy();
    }
}