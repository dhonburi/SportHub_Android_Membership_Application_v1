package com.example.sporthubandroidmembershipapplicationv1;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

    private static final String STATE_SELECTED_TAB = "settings_selected_tab";
    private static final int TAB_GENERAL = 0;
    private static final int TAB_ALERTS = 1;
    private static final int TAB_SECURITY = 2;
    private static final int TAB_HELP = 3;

    private Button btnGeneralTab;
    private Button btnAlertsTab;
    private Button btnSecurityTab;
    private Button btnHelpTab;
    private Button btnOpenStaffQrScanner;
    private TextView txtSettingsContent;
    private View layoutStaffTools;
    private View layoutSettingsHelp;
    private boolean isAdmin;
    private int selectedTab = TAB_GENERAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        configureSystemBars();
        configureWindowInsets();
        bindViews();
        configureSettingsHeader();
        configureStaffToolsAccess();
        configureHelpActions();
        configureTabs();

        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(
                    STATE_SELECTED_TAB, TAB_GENERAL
            );
        }
        showTab(selectedTab);

        findViewById(R.id.btnBackSettings)
                .setOnClickListener(view -> finish());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_TAB, selectedTab);
        super.onSaveInstanceState(outState);
    }

    private void bindViews() {
        btnGeneralTab = findViewById(R.id.btnGeneralTab);
        btnAlertsTab = findViewById(R.id.btnAlertsTab);
        btnSecurityTab = findViewById(R.id.btnSecurityTab);
        btnHelpTab = findViewById(R.id.btnHelpTab);
        btnOpenStaffQrScanner = findViewById(R.id.btnOpenStaffQrScanner);
        txtSettingsContent = findViewById(R.id.txtSettingsContent);
        layoutStaffTools = findViewById(R.id.layoutStaffTools);
        layoutSettingsHelp = findViewById(R.id.layoutSettingsHelp);
    }

    private void configureSettingsHeader() {
        View settingsButton = findViewById(R.id.btnSettings);
        if (settingsButton != null) {
            settingsButton.setVisibility(View.GONE);
        }
    }

    private void configureTabs() {
        btnGeneralTab.setOnClickListener(view -> showTab(TAB_GENERAL));
        btnAlertsTab.setOnClickListener(view -> showTab(TAB_ALERTS));
        btnSecurityTab.setOnClickListener(view -> showTab(TAB_SECURITY));
        btnHelpTab.setOnClickListener(view -> showTab(TAB_HELP));
    }

    private void showTab(int tab) {
        if (tab < TAB_GENERAL || tab > TAB_HELP) {
            tab = TAB_GENERAL;
        }
        selectedTab = tab;

        Button[] tabs = {
                btnGeneralTab, btnAlertsTab, btnSecurityTab, btnHelpTab
        };
        updateSelectedTab(tabs[tab]);

        // Help is available to every account, regardless of role.
        boolean showHelp = tab == TAB_HELP;
        layoutSettingsHelp.setVisibility(showHelp ? View.VISIBLE : View.GONE);
        txtSettingsContent.setVisibility(showHelp ? View.GONE : View.VISIBLE);

        // Only the staff scanner requires an admin account.
        layoutStaffTools.setVisibility(
                isAdmin && tab == TAB_GENERAL ? View.VISIBLE : View.GONE
        );

        if (tab == TAB_GENERAL) {
            txtSettingsContent.setText(R.string.settings_general_placeholder);
        } else if (tab == TAB_ALERTS) {
            txtSettingsContent.setText(R.string.settings_alerts_placeholder);
        } else if (tab == TAB_SECURITY) {
            txtSettingsContent.setText(R.string.settings_security_placeholder);
        }
    }

    private void configureStaffToolsAccess() {
        isAdmin = new MemberSession(this).isAdmin();
        if (isAdmin) {
            btnOpenStaffQrScanner.setOnClickListener(view ->
                    startActivity(new Intent(
                            SettingsActivity.this, StaffQrScannerActivity.class
                    ))
            );
        }
    }

    private void configureHelpActions() {
        bindPhoneAction(R.id.btnSportHubLandline, R.string.help_landline_number);
        bindPhoneAction(R.id.btnSportHubMobile, R.string.help_mobile_number);
        bindPhoneAction(R.id.btnSynergyLandline, R.string.help_landline_number);
        bindPhoneAction(R.id.btnSynergyMobile, R.string.help_mobile_number);

        bindEmailAction(R.id.btnSportHubEmail, R.string.help_sporthub_email);
        bindEmailAction(R.id.btnSynergyEmail, R.string.help_synergy_email);

        bindMapAction(R.id.btnSportHubAddress, R.string.help_sporthub_address);
        bindMapAction(R.id.btnSynergyAddress, R.string.help_synergy_address);
    }

    private void bindPhoneAction(int viewId, int phoneResource) {
        findViewById(viewId).setOnClickListener(view -> {
            Uri phoneUri = Uri.fromParts("tel", getString(phoneResource), null);
            openContactApp(
                    new Intent(Intent.ACTION_DIAL, phoneUri),
                    R.string.help_no_phone_app
            );
        });
    }

    private void bindEmailAction(int viewId, int emailResource) {
        findViewById(viewId).setOnClickListener(view -> {
            Uri emailUri = Uri.fromParts("mailto", getString(emailResource), null);
            openContactApp(
                    new Intent(Intent.ACTION_SENDTO, emailUri),
                    R.string.help_no_email_app
            );
        });
    }

    private void bindMapAction(int viewId, int addressResource) {
        findViewById(viewId).setOnClickListener(view -> {
            String address = getString(addressResource) + ", New Zealand";
            Uri mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
            openContactApp(
                    new Intent(Intent.ACTION_VIEW, mapUri),
                    R.string.help_no_maps_app
            );
        });
    }

    private void openContactApp(Intent intent, int unavailableMessage) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, unavailableMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void updateSelectedTab(Button selectedButton) {
        Button[] tabs = {
                btnGeneralTab, btnAlertsTab, btnSecurityTab, btnHelpTab
        };
        int black = ContextCompat.getColor(this, android.R.color.black);
        int white = ContextCompat.getColor(this, android.R.color.white);

        for (Button tab : tabs) {
            tab.setBackgroundResource(R.drawable.home_tab_unselected_bg);
            tab.setTextColor(black);
            tab.setSelected(tab == selectedButton);
        }

        selectedButton.setBackgroundResource(R.drawable.home_tab_selected_bg);
        selectedButton.setTextColor(white);
        selectedButton.animate()
                .scaleX(1.03f).scaleY(1.03f).setDuration(100)
                .withEndAction(() -> selectedButton.animate()
                        .scaleX(1f).scaleY(1f).setDuration(100));
    }

    private void configureWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.settingsMain),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );
                    view.setPadding(systemBars.left, systemBars.top,
                            systemBars.right, systemBars.bottom);
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

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                window, window.getDecorView()
        );
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
        window.getDecorView().setBackgroundColor(Color.WHITE);
    }
}