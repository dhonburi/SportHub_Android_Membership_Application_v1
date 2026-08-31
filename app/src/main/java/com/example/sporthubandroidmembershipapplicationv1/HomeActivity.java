package com.example.sporthubandroidmembershipapplicationv1;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG_HOME = "tab_home";
    private static final String TAG_QR = "tab_qr";
    private static final String TAG_PROFILE = "tab_profile";

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
    private View headerLayout;

    private Fragment homeTabFragment;
    private Fragment qrTabFragment;
    private Fragment profileTabFragment;

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
                            0
                    );

                    View bottomNavigation =
                            view.findViewById(R.id.bottomNavLayout);

                    ViewGroup.MarginLayoutParams layoutParams =
                            (ViewGroup.MarginLayoutParams)
                                    bottomNavigation.getLayoutParams();

                    layoutParams.bottomMargin =
                            dpToPx(14) + systemBars.bottom;

                    bottomNavigation.setLayoutParams(layoutParams);

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
        headerLayout = findViewById(R.id.headerLayout);

        btnSettings.setOnClickListener(view -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    SettingsActivity.class
            );

            startActivity(intent);
        });

        navHome.setOnClickListener(view -> {
            showTab(TAG_HOME, true);

            updateSelectedNavigation(
                    navHome,
                    iconHome,
                    txtHome
            );
        });

        navQr.setOnClickListener(view -> {
            showTab(TAG_QR, true);

            updateSelectedNavigation(
                    navQr,
                    iconQr,
                    txtQr
            );
        });

        navProfile.setOnClickListener(view -> {
            showTab(TAG_PROFILE, true);

            updateSelectedNavigation(
                    navProfile,
                    iconProfile,
                    txtProfile
            );
        });

        String openFragment =
                getIntent().getStringExtra("OPEN_FRAGMENT");

        if ("QR".equals(openFragment)) {
            showTab(TAG_QR, false);

            updateSelectedNavigation(
                    navQr,
                    iconQr,
                    txtQr
            );
        } else {
            showTab(TAG_HOME, false);

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
        window.setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.setNavigationBarDividerColor(Color.TRANSPARENT);
        }

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

        window.getDecorView()
                .setBackgroundColor(Color.TRANSPARENT);
    }

    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources().getDisplayMetrics().density
        );
    }

    private void updateNavigationBarIcons(boolean useDarkIcons) {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.setAppearanceLightNavigationBars(useDarkIcons);
    }

    private void clearChildPageBackStack() {
        getSupportFragmentManager()
                .popBackStackImmediate(
                        null,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                );
    }

    private void restoreOrCreateTabFragments() {
        FragmentManager fragmentManager =
                getSupportFragmentManager();

        homeTabFragment =
                fragmentManager.findFragmentByTag(TAG_HOME);

        qrTabFragment =
                fragmentManager.findFragmentByTag(TAG_QR);

        profileTabFragment =
                fragmentManager.findFragmentByTag(TAG_PROFILE);

        if (homeTabFragment == null) {
            homeTabFragment = new HomeFragment();
        }

        if (qrTabFragment == null) {
            qrTabFragment = new QrFragment();
        }

        if (profileTabFragment == null) {
            profileTabFragment = new ProfileFragment();
        }
    }

    private Fragment getTabFragment(String tabTag) {
        if (TAG_QR.equals(tabTag)) {
            return qrTabFragment;
        }

        if (TAG_PROFILE.equals(tabTag)) {
            return profileTabFragment;
        }

        return homeTabFragment;
    }

    private void showTab(
            String tabTag,
            boolean clearBackStack
    ) {
        if (clearBackStack) {
            clearChildPageBackStack();
        }

        restoreOrCreateTabFragments();

        Fragment targetFragment =
                getTabFragment(tabTag);

        if (TAG_QR.equals(tabTag)) {
            headerLayout.setVisibility(View.GONE);
            updateNavigationBarIcons(false);
        } else {
            headerLayout.setVisibility(View.VISIBLE);
            updateNavigationBarIcons(true);
        }

        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true);

        Fragment[] tabFragments = {
                homeTabFragment,
                qrTabFragment,
                profileTabFragment
        };

        String[] tabTags = {
                TAG_HOME,
                TAG_QR,
                TAG_PROFILE
        };

        for (int index = 0;
             index < tabFragments.length;
             index++) {

            Fragment tabFragment =
                    tabFragments[index];

            if (!tabFragment.isAdded()) {
                transaction.add(
                        R.id.fragmentContainer,
                        tabFragment,
                        tabTags[index]
                );
            }

            transaction.hide(tabFragment);

            transaction.setMaxLifecycle(
                    tabFragment,
                    Lifecycle.State.STARTED
            );
        }

        transaction.show(targetFragment);

        transaction.setMaxLifecycle(
                targetFragment,
                Lifecycle.State.RESUMED
        );

        transaction.setPrimaryNavigationFragment(
                targetFragment
        );

        transaction.commitNow();
    }

    public void openProfileAndShowTopUp() {
        showTab(TAG_PROFILE, true);

        updateSelectedNavigation(
                navProfile,
                iconProfile,
                txtProfile
        );

        View topUpButton =
                findViewById(R.id.btnTopUpBalance);

        if (topUpButton != null) {
            topUpButton.performClick();
        }
    }

    public void openMembershipFromQr() {
        showTab(TAG_PROFILE, true);

        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .hide(profileTabFragment)
                .setMaxLifecycle(
                        profileTabFragment,
                        Lifecycle.State.STARTED
                )
                .add(
                        R.id.fragmentContainer,
                        new MembershipFragment()
                )
                .addToBackStack("membership")
                .commit();

        updateSelectedNavigation(
                navProfile,
                iconProfile,
                txtProfile
        );
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

        navHome.setBackgroundResource(
                R.drawable.bottom_nav_unselected_bg
        );

        navQr.setBackgroundResource(
                R.drawable.bottom_nav_unselected_bg
        );

        navProfile.setBackgroundResource(
                R.drawable.bottom_nav_unselected_bg
        );

        txtHome.setVisibility(View.GONE);
        txtQr.setVisibility(View.GONE);
        txtProfile.setVisibility(View.GONE);

        iconHome.setColorFilter(white);
        iconQr.setColorFilter(white);
        iconProfile.setColorFilter(white);

        selectedNavigation.setBackgroundResource(
                R.drawable.bottom_nav_selected_bg
        );

        selectedIcon.setColorFilter(black);

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