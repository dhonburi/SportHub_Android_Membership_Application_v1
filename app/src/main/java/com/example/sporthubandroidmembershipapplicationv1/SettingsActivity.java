package com.example.sporthubandroidmembershipapplicationv1;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends AppCompatActivity {

    Button btnGeneralTab, btnAlertsTab, btnSecurityTab, btnHelpTab;
    TextView txtSettingsContent, btnBackSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        btnGeneralTab = findViewById(R.id.btnGeneralTab);
        btnAlertsTab = findViewById(R.id.btnAlertsTab);
        btnSecurityTab = findViewById(R.id.btnSecurityTab);
        btnHelpTab = findViewById(R.id.btnHelpTab);

        txtSettingsContent = findViewById(R.id.txtSettingsContent);
        btnBackSettings = findViewById(R.id.btnBackSettings);

        updateSelectedTab(btnGeneralTab);
        txtSettingsContent.setText("List of Settings Here");

        btnGeneralTab.setOnClickListener(v -> {
            updateSelectedTab(btnGeneralTab);
            txtSettingsContent.setText("List of Settings Here");
        });

        btnAlertsTab.setOnClickListener(v -> {
            updateSelectedTab(btnAlertsTab);
            txtSettingsContent.setText("Alerts Settings Placeholder");
        });

        btnSecurityTab.setOnClickListener(v -> {
            updateSelectedTab(btnSecurityTab);
            txtSettingsContent.setText("Security Settings Placeholder");
        });

        btnHelpTab.setOnClickListener(v -> {
            updateSelectedTab(btnHelpTab);
            txtSettingsContent.setText("Help Settings Placeholder");
        });

        btnBackSettings.setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void updateSelectedTab(Button selectedButton) {
        btnGeneralTab.setBackgroundColor(Color.parseColor("#E5E5E5"));
        btnAlertsTab.setBackgroundColor(Color.parseColor("#E5E5E5"));
        btnSecurityTab.setBackgroundColor(Color.parseColor("#E5E5E5"));
        btnHelpTab.setBackgroundColor(Color.parseColor("#E5E5E5"));

        btnGeneralTab.setTextColor(Color.BLACK);
        btnAlertsTab.setTextColor(Color.BLACK);
        btnSecurityTab.setTextColor(Color.BLACK);
        btnHelpTab.setTextColor(Color.BLACK);

        selectedButton.setBackgroundColor(Color.parseColor("#111111"));
        selectedButton.setTextColor(Color.WHITE);

        selectedButton.animate()
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setDuration(120)
                .withEndAction(() -> selectedButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120));
    }
}