package com.example.sporthubandroidmembershipapplicationv1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private Button btnNewsTab;
    private Button btnTrainingTab;
    private Button btnBookingsTab;
    private Button btnSettings;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        btnNewsTab = view.findViewById(R.id.btnNewsTab);
        btnTrainingTab = view.findViewById(R.id.btnTrainingTab);
        btnBookingsTab = view.findViewById(R.id.btnBookingsTab);
        btnSettings = view.findViewById(R.id.btnSettings);

        // News opens by default
        loadInnerFragment(new NewsFragment());
        updateSelectedTab(btnNewsTab);

        btnNewsTab.setOnClickListener(v -> {
            loadInnerFragment(new NewsFragment());
            updateSelectedTab(btnNewsTab);
        });

        btnTrainingTab.setOnClickListener(v -> {
            loadInnerFragment(new TrainingSessionsFragment());
            updateSelectedTab(btnTrainingTab);
        });

        btnBookingsTab.setOnClickListener(v -> {
            loadInnerFragment(new BookingsFragment());
            updateSelectedTab(btnBookingsTab);
        });

        // Prevent crash if the Settings button isn't present
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(
                        requireActivity(),
                        SettingsActivity.class
                );
                startActivity(intent);
            });
        }
    }

    private void loadInnerFragment(Fragment fragment) {

        getChildFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.homeInnerFragmentContainer,
                        fragment
                )
                .commit();
    }

    private void updateSelectedTab(Button selectedButton) {

        int black = ContextCompat.getColor(
                requireContext(),
                android.R.color.black
        );

        int white = ContextCompat.getColor(
                requireContext(),
                android.R.color.white
        );

        btnNewsTab.setBackgroundResource(
                R.drawable.home_tab_unselected_bg
        );

        btnTrainingTab.setBackgroundResource(
                R.drawable.home_tab_unselected_bg
        );

        btnBookingsTab.setBackgroundResource(
                R.drawable.home_tab_unselected_bg
        );

        btnNewsTab.setTextColor(black);
        btnTrainingTab.setTextColor(black);
        btnBookingsTab.setTextColor(black);

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
}