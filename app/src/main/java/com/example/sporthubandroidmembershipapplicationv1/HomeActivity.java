package com.example.sporthubandroidmembershipapplicationv1;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

public class HomeActivity extends AppCompatActivity {

    Button btnHomeFragment, btnQrFragment, btnProfileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnHomeFragment = findViewById(R.id.btnHomeFragment);
        btnQrFragment = findViewById(R.id.btnQrFragment);
        btnProfileFragment = findViewById(R.id.btnProfileFragment);

        btnHomeFragment.setOnClickListener(v -> {
            loadFragment(new HomeFragment());
            updateSelectedButton(btnHomeFragment);
        });

        btnQrFragment.setOnClickListener(v -> {
            loadFragment(new QrFragment());
            updateSelectedButton(btnQrFragment);
        });

        btnProfileFragment.setOnClickListener(v -> {
            loadFragment(new ProfileFragment());
            updateSelectedButton(btnProfileFragment);
        });

        String openFragment = getIntent().getStringExtra("OPEN_FRAGMENT");

        if ("QR".equals(openFragment)) {
            loadFragment(new QrFragment());
            updateSelectedButton(btnQrFragment);
        } else {
            loadFragment(new HomeFragment());
            updateSelectedButton(btnHomeFragment);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void updateSelectedButton(Button selectedButton) {
        // Reset all buttons back to the rounded unselected background
        btnHomeFragment.setBackgroundResource(R.drawable.nav_button_unselected_bg);
        btnQrFragment.setBackgroundResource(R.drawable.nav_button_unselected_bg);
        btnProfileFragment.setBackgroundResource(R.drawable.nav_button_unselected_bg);

        btnHomeFragment.setTextColor(getColor(android.R.color.white));
        btnQrFragment.setTextColor(getColor(android.R.color.white));
        btnProfileFragment.setTextColor(getColor(android.R.color.white));

        btnHomeFragment.setAlpha(1f);
        btnQrFragment.setAlpha(1f);
        btnProfileFragment.setAlpha(1f);

        // Apply the rounded selected background
        selectedButton.setBackgroundResource(R.drawable.nav_button_bg);
        selectedButton.setTextColor(getColor(android.R.color.black));

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