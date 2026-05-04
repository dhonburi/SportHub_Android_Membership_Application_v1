package com.example.sporthubandroidmembershipapplicationv1;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

public class HomeActivity extends AppCompatActivity {

    Button btnHomeFragment, btnQrFragment, btnProfileFragment;
    ConstraintLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        mainLayout = findViewById(R.id.main);

        btnHomeFragment = findViewById(R.id.btnHomeFragment);
        btnQrFragment = findViewById(R.id.btnQrFragment);
        btnProfileFragment = findViewById(R.id.btnProfileFragment);

        loadFragment(new HomeFragment());
        updateSelectedButton(btnHomeFragment);
        mainLayout.setBackgroundColor(Color.parseColor("#E9E9E9"));

        btnHomeFragment.setOnClickListener(v -> {
            loadFragment(new HomeFragment());
            updateSelectedButton(btnHomeFragment);
            mainLayout.setBackgroundColor(Color.parseColor("#E9E9E9"));
        });

        btnQrFragment.setOnClickListener(v -> {
            loadFragment(new QrFragment());
            updateSelectedButton(btnQrFragment);
            mainLayout.setBackgroundColor(Color.parseColor("#111111"));
        });

        btnProfileFragment.setOnClickListener(v -> {
            loadFragment(new ProfileFragment());
            updateSelectedButton(btnProfileFragment);
            mainLayout.setBackgroundColor(Color.parseColor("#E9E9E9"));
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void updateSelectedButton(Button selectedButton) {
        btnHomeFragment.setBackgroundResource(R.drawable.nav_button_unselected_bg);
        btnQrFragment.setBackgroundResource(R.drawable.nav_button_unselected_bg);
        btnProfileFragment.setBackgroundResource(R.drawable.nav_button_unselected_bg);

        btnHomeFragment.setTextColor(Color.WHITE);
        btnQrFragment.setTextColor(Color.WHITE);
        btnProfileFragment.setTextColor(Color.WHITE);

        selectedButton.setBackgroundResource(R.drawable.nav_button_bg);
        selectedButton.setTextColor(Color.BLACK);

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