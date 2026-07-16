package com.example.sporthubandroidmembershipapplicationv1;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BookingsFragment extends Fragment {

    private Button btnBookCourt;

    private LinearLayout cardPortage44;
    private LinearLayout cardPortage38;
    private LinearLayout cardPortage36;

    public BookingsFragment() {
        super(R.layout.fragment_bookings);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        btnBookCourt = view.findViewById(R.id.btnBookCourt);

        cardPortage44 = view.findViewById(R.id.cardPortage44);
        cardPortage38 = view.findViewById(R.id.cardPortage38);
        cardPortage36 = view.findViewById(R.id.cardPortage36);

        btnBookCourt.setOnClickListener(v -> {
            Toast.makeText(
                    requireContext(),
                    "Court booking will be added later.",
                    Toast.LENGTH_SHORT
            ).show();
        });

        cardPortage44.setOnClickListener(v ->
                showVenueMessage("44 Portage Road")
        );

        cardPortage38.setOnClickListener(v ->
                showVenueMessage("38 Portage Road")
        );

        cardPortage36.setOnClickListener(v ->
                showVenueMessage("36 Portage Road")
        );
    }

    private void showVenueMessage(String venueName) {
        Toast.makeText(
                requireContext(),
                venueName + " booking details will be added later.",
                Toast.LENGTH_SHORT
        ).show();
    }
}