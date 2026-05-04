package com.example.sporthubandroidmembershipapplicationv1;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TrainingSessionsFragment extends Fragment {

    public TrainingSessionsFragment() {
        super(R.layout.fragment_training_sessions);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.cardBasketball).setOnClickListener(v ->
                openTrainingDetail(
                        "Basketball Trainings",
                        "Beginner / Upper Beginner:\nDesigned for beginner and upper beginner players aged 6 to 12 years to learn the fundamentals of basketball in a structured environment.",
                        "Intermediate Basketball:\nFor players who already know the basics and want to improve confidence, teamwork, and match-play performance.",
                        "Senior Basketball:\nAn instructional programme for players with stronger core skills who want a more competitive and structured basketball training experience."
                )
        );

        view.findViewById(R.id.cardVolleyball).setOnClickListener(v ->
                openTrainingDetail(
                        "Volleyball Trainings",
                        "Beginner Volleyball:\nIntroduces basic passing, serving, and teamwork in a supportive training environment.",
                        "Intermediate Volleyball:\nHelps players improve game awareness, coordination, and ball control.",
                        "Advanced Volleyball:\nFocuses on match play, team communication, and stronger technical ability."
                )
        );

        view.findViewById(R.id.cardBadminton).setOnClickListener(v ->
                openTrainingDetail(
                        "Badminton Trainings",
                        "Junior Badminton:\nBuilds early movement, coordination, and racket skills for younger players.",
                        "Intermediate Badminton:\nSupports players who want to strengthen consistency, footwork, and tactical play.",
                        "Senior Badminton:\nDesigned for players aiming to improve competitiveness and advanced gameplay."
                )
        );

        view.findViewById(R.id.cardTableTennis).setOnClickListener(v ->
                openTrainingDetail(
                        "Table Tennis Trainings",
                        "Beginner Table Tennis:\nCovers the fundamentals of grip, control, and rally practice.",
                        "Intermediate Table Tennis:\nFocuses on improving reactions, spin control, and structured drills.",
                        "Advanced Table Tennis:\nSupports more competitive players through tactical and technical development."
                )
        );

        view.findViewById(R.id.cardFootball).setOnClickListener(v ->
                openTrainingDetail(
                        "Football Trainings",
                        "Junior Football:\nIntroduces movement, passing, teamwork, and coordination for newer players.",
                        "Intermediate Football:\nHelps players improve positioning, decision-making, and match readiness.",
                        "Senior Football:\nDesigned for players seeking a more competitive and intensive training experience."
                )
        );

        view.findViewById(R.id.cardMoreTrainings).setOnClickListener(v ->
                openTrainingDetail(
                        "More Trainings",
                        "Multi-sport Introduction:\nA flexible programme introducing different sports and movement-based activities.",
                        "Community Activity Sessions:\nCasual sessions that encourage participation, fitness, and social involvement.",
                        "Future Programmes:\nPlaceholder for upcoming training categories and new programme additions."
                )
        );
    }

    private void openTrainingDetail(String title, String detail1, String detail2, String detail3) {
        TrainingDetailFragment fragment = new TrainingDetailFragment();

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("detail1", detail1);
        args.putString("detail2", detail2);
        args.putString("detail3", detail3);
        fragment.setArguments(args);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.homeInnerFragmentContainer, fragment)
                .commit();
    }
}