package com.example.sporthubandroidmembershipapplicationv1;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TrainingSessionsFragment extends Fragment {

    private LinearLayout cardBasketball;
    private LinearLayout cardVolleyball;
    private LinearLayout cardBadminton;
    private LinearLayout cardTableTennis;
    private LinearLayout cardFitness;
    private LinearLayout cardFootball;

    public TrainingSessionsFragment() {
        super(R.layout.fragment_training_sessions);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        cardBasketball = view.findViewById(R.id.cardBasketball);
        cardVolleyball = view.findViewById(R.id.cardVolleyball);
        cardBadminton = view.findViewById(R.id.cardBadminton);
        cardTableTennis = view.findViewById(R.id.cardTableTennis);
        cardFitness = view.findViewById(R.id.cardFitness);
        cardFootball = view.findViewById(R.id.cardFootball);

        cardBasketball.setOnClickListener(v ->
                openTrainingDetails("Basketball")
        );

        cardVolleyball.setOnClickListener(v ->
                openTrainingDetails("Volleyball")
        );

        cardBadminton.setOnClickListener(v ->
                openTrainingDetails("Badminton")
        );

        cardTableTennis.setOnClickListener(v ->
                openTrainingDetails("Table Tennis")
        );

        cardFitness.setOnClickListener(v ->
                openTrainingDetails("Fitness")
        );

        cardFootball.setOnClickListener(v ->
                openTrainingDetails("Football")
        );
    }

    private void openTrainingDetails(String sportName) {

        TrainingDetailFragment detailFragment =
                TrainingDetailFragment.newInstance(sportName);

        getParentFragmentManager()
                .beginTransaction()

                // Opening animation:
                // Detail page slides in from the right
                // Training list slides out to the left
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )

                .setReorderingAllowed(true)
                .replace(
                        R.id.homeInnerFragmentContainer,
                        detailFragment
                )
                .addToBackStack("training_details")
                .commit();
    }
}