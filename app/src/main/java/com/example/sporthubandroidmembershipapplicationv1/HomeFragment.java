package com.example.sporthubandroidmembershipapplicationv1;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    Button btnBookingsTab, btnTrainingTab, btnNewsTab;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnBookingsTab = view.findViewById(R.id.btnBookingsTab);
        btnTrainingTab = view.findViewById(R.id.btnTrainingTab);
        btnNewsTab = view.findViewById(R.id.btnNewsTab);

        loadInnerFragment(new BookingsFragment());
        updateSelectedTab(btnBookingsTab);

        btnBookingsTab.setOnClickListener(v -> {
            loadInnerFragment(new BookingsFragment());
            updateSelectedTab(btnBookingsTab);
        });

        btnTrainingTab.setOnClickListener(v -> {
            loadInnerFragment(new TrainingSessionsFragment());
            updateSelectedTab(btnTrainingTab);
        });

        btnNewsTab.setOnClickListener(v -> {
            loadInnerFragment(new NewsFragment());
            updateSelectedTab(btnNewsTab);
        });
    }

    private void loadInnerFragment(Fragment fragment) {
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.homeInnerFragmentContainer, fragment)
                .commit();
    }

    private void updateSelectedTab(Button selectedButton) {
        btnBookingsTab.setBackgroundColor(Color.parseColor("#E5E5E5"));
        btnTrainingTab.setBackgroundColor(Color.parseColor("#E5E5E5"));
        btnNewsTab.setBackgroundColor(Color.parseColor("#E5E5E5"));

        btnBookingsTab.setTextColor(Color.BLACK);
        btnTrainingTab.setTextColor(Color.BLACK);
        btnNewsTab.setTextColor(Color.BLACK);

        selectedButton.setBackgroundColor(Color.parseColor("#111111"));
        selectedButton.setTextColor(Color.WHITE);
    }
}