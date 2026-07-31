package com.example.sporthubandroidmembershipapplicationv1;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.sporthubandroidmembershipapplicationv1.models.MemberProfileResponse;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;
import com.example.sporthubandroidmembershipapplicationv1.session.MemberSession;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileDetailsActivity extends AppCompatActivity {

    private TextView txtHeaderName;
    private TextView txtHeaderMemberId;
    private TextView txtName;
    private TextView txtEmail;
    private TextView txtPhone;
    private TextView txtGender;
    private TextView txtStatus;
    private ProgressBar progressProfileDetails;
    private Button btnRetryProfileDetails;

    private MemberSession memberSession;
    private Call<MemberProfileResponse> memberProfileCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_details);

        configureSystemBars();
        applySystemBarInsets();
        bindViews();

        memberSession = new MemberSession(this);

        findViewById(R.id.btnBackProfileDetails)
                .setOnClickListener(view -> finish());

        btnRetryProfileDetails.setOnClickListener(
                view -> loadMemberProfile()
        );

        loadMemberProfile();
    }

    private void configureSystemBars() {
        Window window = getWindow();

        window.setStatusBarColor(Color.WHITE);
        window.setNavigationBarColor(Color.parseColor("#151515"));

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
        controller.setAppearanceLightNavigationBars(false);
    }

    private void applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.profileDetailsMain),
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
    }

    private void bindViews() {
        txtHeaderName =
                findViewById(R.id.txtProfileDetailsHeaderName);

        txtHeaderMemberId =
                findViewById(R.id.txtProfileDetailsHeaderMemberId);

        txtName = findViewById(R.id.txtProfileDetailsName);
        txtEmail = findViewById(R.id.txtProfileDetailsEmail);
        txtPhone = findViewById(R.id.txtProfileDetailsPhone);
        txtGender = findViewById(R.id.txtProfileDetailsGender);
        txtStatus = findViewById(R.id.txtProfileDetailsStatus);

        progressProfileDetails =
                findViewById(R.id.progressProfileDetails);

        btnRetryProfileDetails =
                findViewById(R.id.btnRetryProfileDetails);
    }

    private void loadMemberProfile() {
        int memberId = memberSession.getMemberId();
        String memberNumber = memberSession.getMemberNumber();

        if (memberId <= 0) {
            showUnavailable(
                    "No logged-in member was found.",
                    memberNumber
            );
            return;
        }

        showLoading(memberNumber);

        memberProfileCall = ApiClient
                .getMemberApiService()
                .getMemberProfile(memberId);

        memberProfileCall.enqueue(
                new Callback<MemberProfileResponse>() {
                    @Override
                    public void onResponse(
                            Call<MemberProfileResponse> call,
                            Response<MemberProfileResponse> response
                    ) {
                        MemberProfileResponse profile = response.body();

                        if (response.isSuccessful() && profile != null) {
                            displayMemberProfile(profile);
                            return;
                        }

                        String message = response.code() == 404
                                ? "Member profile not found."
                                : "Profile details are unavailable right now.";

                        showUnavailable(message, memberNumber);
                    }

                    @Override
                    public void onFailure(
                            Call<MemberProfileResponse> call,
                            Throwable throwable
                    ) {
                        if (call.isCanceled()) {
                            return;
                        }

                        showUnavailable(
                                "Unable to connect. Check the API and try again.",
                                memberNumber
                        );
                    }
                }
        );
    }

    private void showLoading(String memberNumber) {
        progressProfileDetails.setVisibility(View.VISIBLE);
        txtStatus.setVisibility(View.VISIBLE);
        txtStatus.setText("Loading your profile details...");
        btnRetryProfileDetails.setVisibility(View.GONE);

        txtHeaderName.setText("Loading profile...");
        setHeaderMemberNumber(memberNumber);
        clearDetailValues();
    }

    private void displayMemberProfile(
            MemberProfileResponse profile
    ) {
        progressProfileDetails.setVisibility(View.GONE);
        txtStatus.setVisibility(View.GONE);
        btnRetryProfileDetails.setVisibility(View.GONE);

        String fullName = buildFullName(profile);

        txtHeaderName.setText(fullName);
        setHeaderMemberNumber(profile.getMemberNumber());

        txtName.setText(fullName);
        txtEmail.setText(displayValue(profile.getEmail()));
        txtPhone.setText(displayValue(profile.getPhone()));
        txtGender.setText(displayValue(profile.getGender()));
    }

    private void showUnavailable(
            String message,
            String memberNumber
    ) {
        progressProfileDetails.setVisibility(View.GONE);
        txtStatus.setVisibility(View.VISIBLE);
        txtStatus.setText(message);
        btnRetryProfileDetails.setVisibility(View.VISIBLE);

        txtHeaderName.setText("Profile unavailable");
        setHeaderMemberNumber(memberNumber);
        clearDetailValues();
    }

    private String buildFullName(MemberProfileResponse profile) {
        String firstName = cleanValue(profile.getFirstName());
        String lastName = cleanValue(profile.getLastName());
        String fullName = (firstName + " " + lastName).trim();

        return fullName.isEmpty()
                ? "SportHub Member"
                : fullName;
    }

    private void setHeaderMemberNumber(String memberNumber) {
        String cleanMemberNumber = cleanValue(memberNumber);

        txtHeaderMemberId.setText(
                cleanMemberNumber.isEmpty()
                        ? "ID: Not provided"
                        : "ID: " + cleanMemberNumber
        );
    }

    private void clearDetailValues() {
        txtName.setText("Not provided");
        txtEmail.setText("Not provided");
        txtPhone.setText("Not provided");
        txtGender.setText("Not provided");
    }

    private String displayValue(String value) {
        String cleanedValue = cleanValue(value);

        return cleanedValue.isEmpty()
                ? "Not provided"
                : cleanedValue;
    }

    private String cleanValue(String value) {
        return value == null
                ? ""
                : value.trim();
    }

    @Override
    protected void onDestroy() {
        if (memberProfileCall != null
                && !memberProfileCall.isCanceled()) {
            memberProfileCall.cancel();
        }

        super.onDestroy();
    }
}