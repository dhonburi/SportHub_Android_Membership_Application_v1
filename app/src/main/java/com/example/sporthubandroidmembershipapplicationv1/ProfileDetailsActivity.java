package com.example.sporthubandroidmembershipapplicationv1;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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

    private static final String FIELD_NAME = "name";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_PHONE = "phone";
    private static final String FIELD_GENDER = "gender";

    private static final String NOT_PROVIDED = "Not provided";

    private TextView txtHeaderName;
    private TextView txtHeaderMemberId;
    private TextView txtName;
    private TextView txtEmail;
    private TextView txtPhone;
    private TextView txtGender;
    private TextView txtStatus;
    private ProgressBar progressProfileDetails;
    private Button btnRetryProfileDetails;

    private View rowName;
    private View rowEmail;
    private View rowPhone;
    private View rowGender;

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

        setUpEditableRows();

        loadMemberProfile();
    }

    private void setUpEditableRows() {
        rowName.setOnClickListener(view ->
                showEditFieldDialog(
                        "Edit name",
                        txtName,
                        InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                        true,
                        FIELD_NAME
                )
        );

        rowEmail.setOnClickListener(view ->
                showEditFieldDialog(
                        "Edit email",
                        txtEmail,
                        InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                        false,
                        FIELD_EMAIL
                )
        );

        rowPhone.setOnClickListener(view ->
                showEditFieldDialog(
                        "Edit phone",
                        txtPhone,
                        InputType.TYPE_CLASS_PHONE,
                        false,
                        FIELD_PHONE
                )
        );

        rowGender.setOnClickListener(view ->
                showEditFieldDialog(
                        "Edit gender",
                        txtGender,
                        InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                        false,
                        FIELD_GENDER
                )
        );
    }

    /**
     * Shows a small dialog that lets the member type a new value for one
     * field and applies it straight to the on-screen TextView.
     *
     * NOTE: This is a local-only edit for now — nothing is sent to the
     * API or saved to the database yet. Once US-06 wires up the backend
     * PUT/PATCH endpoint, the "Save" action below is where that network
     * call should be added, alongside a loading/error state like
     * loadMemberProfile() already uses.
     */
    private void showEditFieldDialog(
            String title,
            TextView targetView,
            int inputType,
            boolean required,
            String fieldKind
    ) {
        EditText input = new EditText(this);
        input.setInputType(inputType);
        input.setSingleLine(true);

        String currentValue = targetView.getText().toString();

        if (!NOT_PROVIDED.equals(currentValue)) {
            input.setText(currentValue);
            input.setSelection(currentValue.length());
        }

        int paddingPx = (int) (20 * getResources().getDisplayMetrics().density);

        FrameLayout inputContainer = new FrameLayout(this);
        inputContainer.setPadding(paddingPx, paddingPx / 2, paddingPx, 0);
        inputContainer.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(inputContainer)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(saveClick ->
                                trySaveField(
                                        dialog,
                                        input,
                                        targetView,
                                        required,
                                        fieldKind
                                )
                        )
        );

        dialog.show();
    }

    private void trySaveField(
            AlertDialog dialog,
            EditText input,
            TextView targetView,
            boolean required,
            String fieldKind
    ) {
        String newValue = input.getText().toString().trim();

        if (required && newValue.isEmpty()) {
            input.setError("This field can't be empty");
            return;
        }

        if (FIELD_EMAIL.equals(fieldKind)
                && !newValue.isEmpty()
                && !Patterns.EMAIL_ADDRESS.matcher(newValue).matches()) {

            input.setError("Enter a valid email address");
            return;
        }

        String displayValue = newValue.isEmpty()
                ? NOT_PROVIDED
                : newValue;

        targetView.setText(displayValue);

        if (FIELD_NAME.equals(fieldKind)) {
            txtHeaderName.setText(displayValue);
        }

        Toast.makeText(
                this,
                "Updated locally — not yet saved to the server.",
                Toast.LENGTH_SHORT
        ).show();

        dialog.dismiss();
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

        rowName = findViewById(R.id.rowProfileDetailsName);
        rowEmail = findViewById(R.id.rowProfileDetailsEmail);
        rowPhone = findViewById(R.id.rowProfileDetailsPhone);
        rowGender = findViewById(R.id.rowProfileDetailsGender);

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