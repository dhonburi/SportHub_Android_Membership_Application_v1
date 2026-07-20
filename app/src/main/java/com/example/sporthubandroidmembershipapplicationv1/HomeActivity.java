package com.example.sporthubandroidmembershipapplicationv1;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout navHome;
    private LinearLayout navQr;
    private LinearLayout navProfile;

    private ImageView iconHome;
    private ImageView iconQr;
    private ImageView iconProfile;

    private TextView txtHome;
    private TextView txtQr;
    private TextView txtProfile;

    private Button btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        configureSystemBars();

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
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

        navHome = findViewById(R.id.navHome);
        navQr = findViewById(R.id.navQr);
        navProfile = findViewById(R.id.navProfile);

        iconHome = findViewById(R.id.iconHome);
        iconQr = findViewById(R.id.iconQr);
        iconProfile = findViewById(R.id.iconProfile);

        txtHome = findViewById(R.id.txtHome);
        txtQr = findViewById(R.id.txtQr);
        txtProfile = findViewById(R.id.txtProfile);

        btnSettings = findViewById(R.id.btnSettings);

        btnSettings.setOnClickListener(view -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    SettingsActivity.class
            );
            startActivity(intent);
        });

        navHome.setOnClickListener(view -> {
            loadFragment(new HomeFragment());
            updateSelectedNavigation(
                    navHome,
                    iconHome,
                    txtHome
            );
        });

        navQr.setOnClickListener(view -> {
            loadFragment(new QrFragment());
            updateSelectedNavigation(
                    navQr,
                    iconQr,
                    txtQr
            );
        });

        navProfile.setOnClickListener(view -> {
            loadFragment(new ProfileFragment());
            updateSelectedNavigation(
                    navProfile,
                    iconProfile,
                    txtProfile
            );
        });

        String openFragment =
                getIntent().getStringExtra("OPEN_FRAGMENT");

        if ("QR".equals(openFragment)) {
            loadFragment(new QrFragment());

            updateSelectedNavigation(
                    navQr,
                    iconQr,
                    txtQr
            );
        } else {
            loadFragment(new HomeFragment());

            updateSelectedNavigation(
                    navHome,
                    iconHome,
                    txtHome
            );
        }
    }

    private void configureSystemBars() {

        Window window = getWindow();

        window.setStatusBarColor(Color.WHITE);
        window.setNavigationBarColor(Color.WHITE);

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

    private void loadFragment(Fragment fragment) {

        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void updateSelectedNavigation(
            LinearLayout selectedNavigation,
            ImageView selectedIcon,
            TextView selectedText
    ) {

        int white = ContextCompat.getColor(
                this,
                android.R.color.white
        );

        int black = ContextCompat.getColor(
                this,
                android.R.color.black
        );

        // Reset all bottom navigation backgrounds
        navHome.setBackgroundResource(
                R.drawable.bottom_nav_unselected_bg
        );

        navQr.setBackgroundResource(
                R.drawable.bottom_nav_unselected_bg
        );

        navProfile.setBackgroundResource(
                R.drawable.bottom_nav_unselected_bg
        );

        // Hide all navigation text
        txtHome.setVisibility(View.GONE);
        txtQr.setVisibility(View.GONE);
        txtProfile.setVisibility(View.GONE);

        // Make all icons white
        iconHome.setColorFilter(white);
        iconQr.setColorFilter(white);
        iconProfile.setColorFilter(white);

        // Highlight selected navigation button
        selectedNavigation.setBackgroundResource(
                R.drawable.bottom_nav_selected_bg
        );

        // Make selected icon black
        selectedIcon.setColorFilter(black);

        // Show selected navigation text
        selectedText.setTextColor(black);
        selectedText.setVisibility(View.VISIBLE);

        selectedNavigation.animate()
                .scaleX(1.04f)
                .scaleY(1.04f)
                .setDuration(120)
                .withEndAction(() ->
                        selectedNavigation.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                );
    }
}