package com.example.sporthubandroidmembershipapplicationv1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

/**
 * US-14: a local visual preview only.
 * These examples are not member balances, earning rules or redeemable rewards.
 */
public class RewardsFragment extends Fragment {

    private static final RewardPreview[] PREVIEW_REWARDS = {
            new RewardPreview(R.string.reward_preview_court_name,
                    R.string.reward_preview_court_visual, 1,
                    R.drawable.training_blue_placeholder),
            new RewardPreview(R.string.reward_preview_booking_name,
                    R.string.reward_preview_booking_visual, 1,
                    R.drawable.training_green_placeholder),
            new RewardPreview(R.string.reward_preview_drink_name,
                    R.string.reward_preview_drink_visual, 1,
                    R.drawable.training_pink_placeholder),
            new RewardPreview(R.string.reward_preview_membership_name,
                    R.string.reward_preview_membership_visual, 3,
                    R.drawable.training_orange_placeholder),
            // Sample names for the two cards cut off in the supplied screenshot.
            new RewardPreview(R.string.reward_preview_friend_name,
                    R.string.reward_preview_friend_visual, 3,
                    R.drawable.training_grey_placeholder),
            new RewardPreview(R.string.reward_preview_session_name,
                    R.string.reward_preview_session_visual, 5,
                    R.drawable.training_dark_grey_placeholder)
    };

    private AlertDialog rewardDialog;
    private View bottomNavigation;
    private View.OnLayoutChangeListener navigationLayoutListener;

    public RewardsFragment() {
        super(R.layout.fragment_rewards);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnRewardsBack).setOnClickListener(button -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        LinearLayout cards = view.findViewById(R.id.layoutRewardCards);
        renderPreviewCards(cards);
        reserveBottomNavigationSpace(view);
    }

    private void renderPreviewCards(LinearLayout container) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        container.removeAllViews();

        for (int index = 0; index < PREVIEW_REWARDS.length; index += 2) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBaselineAligned(false);
            row.setWeightSum(2f);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.topMargin = index == 0 ? 0 : dpToPx(14);
            container.addView(row, rowParams);

            for (int column = 0; column < 2; column++) {
                int rewardIndex = index + column;
                if (rewardIndex >= PREVIEW_REWARDS.length) {
                    break;
                }

                RewardPreview reward = PREVIEW_REWARDS[rewardIndex];
                View card = inflater.inflate(
                        R.layout.layout_reward_preview_card, row, false);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                if (column == 0) {
                    cardParams.setMarginEnd(dpToPx(6));
                } else {
                    cardParams.setMarginStart(dpToPx(6));
                }

                TextView visual = card.findViewById(R.id.txtRewardVisual);
                TextView title = card.findViewById(R.id.txtRewardName);
                TextView cost = card.findViewById(R.id.txtRewardCost);

                visual.setText(reward.visualRes);
                visual.setBackgroundResource(reward.backgroundRes);
                title.setText(reward.nameRes);

                String exampleCost = getResources().getQuantityString(
                        R.plurals.reward_preview_points, reward.points, reward.points);
                cost.setText(getString(R.string.reward_preview_cost_badge, exampleCost));
                card.setContentDescription(getString(
                        R.string.reward_preview_card_accessibility,
                        getString(reward.nameRes), exampleCost));
                card.setOnClickListener(button -> showRewardPreview(reward));

                row.addView(card, cardParams);
            }
        }
    }

    private void showRewardPreview(RewardPreview reward) {
        if (!isAdded() || (rewardDialog != null && rewardDialog.isShowing())) {
            return;
        }

        String exampleCost = getResources().getQuantityString(
                R.plurals.reward_preview_points, reward.points, reward.points);

        rewardDialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.rewards_preview_dialog_title)
                .setMessage(getString(R.string.rewards_preview_dialog_message,
                        getString(reward.nameRes), exampleCost))
                .setPositiveButton(android.R.string.ok, null)
                .create();
        rewardDialog.setOnDismissListener(dialog -> rewardDialog = null);
        rewardDialog.show();
    }

    private void reserveBottomNavigationSpace(View screen) {
        bottomNavigation = requireActivity().findViewById(R.id.bottomNavLayout);
        if (bottomNavigation == null) {
            return;
        }

        navigationLayoutListener = (navigation, left, top, right, bottom,
                                    oldLeft, oldTop, oldRight, oldBottom) ->
                updateBottomPadding(screen, navigation);
        bottomNavigation.addOnLayoutChangeListener(navigationLayoutListener);

        screen.post(() -> {
            if (getView() == screen && bottomNavigation != null) {
                updateBottomPadding(screen, bottomNavigation);
            }
        });
    }

    private void updateBottomPadding(View screen, View navigation) {
        int bottomMargin = 0;
        if (navigation.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            bottomMargin = ((ViewGroup.MarginLayoutParams)
                    navigation.getLayoutParams()).bottomMargin;
        }

        int navigationHeight = navigation.getHeight();
        if (navigationHeight <= 0) {
            navigationHeight = dpToPx(76);
        }

        int padding = navigationHeight + bottomMargin + dpToPx(18);
        if (screen.getPaddingBottom() != padding) {
            screen.setPadding(screen.getPaddingLeft(), screen.getPaddingTop(),
                    screen.getPaddingRight(), padding);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        if (bottomNavigation != null && navigationLayoutListener != null) {
            bottomNavigation.removeOnLayoutChangeListener(navigationLayoutListener);
        }
        bottomNavigation = null;
        navigationLayoutListener = null;

        if (rewardDialog != null) {
            rewardDialog.dismiss();
            rewardDialog = null;
        }
        super.onDestroyView();
    }

    private static final class RewardPreview {
        final int nameRes;
        final int visualRes;
        final int points;
        final int backgroundRes;

        RewardPreview(int nameRes, int visualRes, int points, int backgroundRes) {
            this.nameRes = nameRes;
            this.visualRes = visualRes;
            this.points = points;
            this.backgroundRes = backgroundRes;
        }
    }
}