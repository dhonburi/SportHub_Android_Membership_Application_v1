package com.example.sporthubandroidmembershipapplicationv1;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.sporthubandroidmembershipapplicationv1.session.MemberSession;

public class SettingsActivity extends AppCompatActivity {

    private Button btnGeneralTab;
    private Button btnAlertsTab;
    private Button btnSecurityTab;
    private Button btnHelpTab;
    private Button btnOpenStaffQrScanner;

    private TextView txtSettingsContent;
    private TextView txtStaffToolsTitle;
    private TextView txtStaffScannerDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        configureSystemBars();
        configureWindowInsets();
        bindViews();
        configureSettingsHeader();
        configureTabs();
        configureStaffToolsAccess();

        findViewById(R.id.btnBackSettings)
                .setOnClickListener(view -> finish());
    }

    private void bindViews() {
        btnGeneralTab = findViewById(R.id.btnGeneralTab);
        btnAlertsTab = findViewById(R.id.btnAlertsTab);
        btnSecurityTab = findViewById(R.id.btnSecurityTab);
        btnHelpTab = findViewById(R.id.btnHelpTab);

        btnOpenStaffQrScanner = findViewById(
                R.id.btnOpenStaffQrScanner
        );

        txtStaffToolsTitle = findViewById(
                R.id.txtStaffToolsTitle
        );

        txtStaffScannerDescription = findViewById(
                R.id.txtStaffScannerDescription
        );

        txtSettingsContent = findViewById(
                R.id.txtSettingsContent
        );
    }

    private void configureSettingsHeader() {
        View settingsButton = findViewById(R.id.btnSettings);

        if (settingsButton != null) {
            settingsButton.setVisibility(View.GONE);
        }
    }

    private void configureTabs() {
        updateSelectedTab(btnGeneralTab);
        txtSettingsContent.setText("List of Settings Here");

        btnGeneralTab.setOnClickListener(view -> {
            updateSelectedTab(btnGeneralTab);
            txtSettingsContent.setText("List of Settings Here");
        });

        btnAlertsTab.setOnClickListener(view -> {
            updateSelectedTab(btnAlertsTab);
            txtSettingsContent.setText(
                    "Alerts Settings Placeholder"
            );
        });

        btnSecurityTab.setOnClickListener(view -> {
            updateSelectedTab(btnSecurityTab);
            txtSettingsContent.setText(
                    "Security Settings Placeholder"
            );
        });

        btnHelpTab.setOnClickListener(view -> {
            updateSelectedTab(btnHelpTab);
            txtSettingsContent.setText(
                    "Help Settings Placeholder"
            );
        });
    }

    private void configureStaffToolsAccess() {
        MemberSession memberSession = new MemberSession(this);
        boolean isAdmin = memberSession.isAdmin();

        int staffToolsVisibility =
                isAdmin ? View.VISIBLE : View.GONE;

        txtStaffToolsTitle.setVisibility(
                staffToolsVisibility
        );

        txtStaffScannerDescription.setVisibility(
                staffToolsVisibility
        );

        btnOpenStaffQrScanner.setVisibility(
                staffToolsVisibility
        );

        if (!isAdmin) {
            return;
        }

        btnOpenStaffQrScanner.setOnClickListener(view -> {
            Intent intent = new Intent(
                    SettingsActivity.this,
                    StaffQrScannerActivity.class
            );

            startActivity(intent);
        });
    }

    private void updateSelectedTab(Button selectedButton) {
        Button[] tabButtons = {
                btnGeneralTab,
                btnAlertsTab,
                btnSecurityTab,
                btnHelpTab
        };

        int black = ContextCompat.getColor(
                this,
                android.R.color.black
        );

        int white = ContextCompat.getColor(
                this,
                android.R.color.white
        );

        for (Button tabButton : tabButtons) {
            tabButton.setBackgroundResource(
                    R.drawable.home_tab_unselected_bg
            );

            tabButton.setTextColor(black);
        }

        selectedButton.setBackgroundResource(
                R.drawable.home_tab_selected_bg
        );

        selectedButton.setTextColor(white);

        selectedButton.animate()
                .scaleX(1.03f)
                .scaleY(1.03f)
                .setDuration(100)
                .withEndAction(() ->
                        selectedButton.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                );
    }

    private void configureWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.settingsMain),
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

        window.setStatusBarColor(Color.WHITE);
        window.setNavigationBarColor(Color.WHITE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.setNavigationBarDividerColor(Color.TRANSPARENT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        window,
                        window.getDecorView()
                );

        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        window.getDecorView().setBackgroundColor(Color.WHITE);
    }
}