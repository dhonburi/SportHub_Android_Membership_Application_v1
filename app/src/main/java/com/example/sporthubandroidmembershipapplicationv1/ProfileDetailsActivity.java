package com.example.sporthubandroidmembershipapplicationv1;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
import com.example.sporthubandroidmembershipapplicationv1.models.UpdateMemberProfileRequest;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;
import com.example.sporthubandroidmembershipapplicationv1.session.MemberSession;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileDetailsActivity extends AppCompatActivity {

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
    private MemberProfileResponse currentProfile;
    private Call<MemberProfileResponse> memberProfileCall;
    private Call<MemberProfileResponse> profileUpdateCall;

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
        setEditingEnabled(false);
        loadMemberProfile();
    }

    private void setUpEditableRows() {
        rowName.setOnClickListener(view -> showEditNameDialog());

        rowEmail.setOnClickListener(view ->
                showEditFieldDialog(
                        "Edit email",
                        txtEmail,
                        InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                        true,
                        FIELD_EMAIL,
                        256
                )
        );

        rowPhone.setOnClickListener(view ->
                showEditFieldDialog(
                        "Edit phone",
                        txtPhone,
                        InputType.TYPE_CLASS_PHONE,
                        false,
                        FIELD_PHONE,
                        30
                )
        );

        rowGender.setOnClickListener(view ->
                showEditFieldDialog(
                        "Edit gender",
                        txtGender,
                        InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                        false,
                        FIELD_GENDER,
                        50
                )
        );
    }

    private void showEditNameDialog() {
        if (currentProfile == null) {
            showToast("Load the member profile before editing it.");
            return;
        }

        EditText firstNameInput = new EditText(this);
        firstNameInput.setHint("First name");
        firstNameInput.setSingleLine(true);
        firstNameInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );
        firstNameInput.setFilters(
                new InputFilter[]{new InputFilter.LengthFilter(100)}
        );
        firstNameInput.setText(cleanValue(currentProfile.getFirstName()));

        EditText lastNameInput = new EditText(this);
        lastNameInput.setHint("Last name");
        lastNameInput.setSingleLine(true);
        lastNameInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );
        lastNameInput.setFilters(
                new InputFilter[]{new InputFilter.LengthFilter(100)}
        );
        lastNameInput.setText(cleanValue(currentProfile.getLastName()));

        LinearLayout inputContainer = new LinearLayout(this);
        inputContainer.setOrientation(LinearLayout.VERTICAL);

        int paddingPx = getDialogPaddingPx();
        inputContainer.setPadding(
                paddingPx,
                paddingPx / 2,
                paddingPx,
                0
        );
        inputContainer.addView(firstNameInput);
        inputContainer.addView(lastNameInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit name")
                .setView(inputContainer)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(saveClick -> {
                            String firstName = firstNameInput
                                    .getText().toString().trim();
                            String lastName = lastNameInput
                                    .getText().toString().trim();

                            if (firstName.isEmpty()) {
                                firstNameInput.setError(
                                        "First name can't be empty"
                                );
                                return;
                            }

                            if (lastName.isEmpty()) {
                                lastNameInput.setError(
                                        "Last name can't be empty"
                                );
                                return;
                            }

                            saveProfileUpdate(
                                    dialog,
                                    firstNameInput,
                                    firstName,
                                    lastName,
                                    cleanValue(currentProfile.getEmail()),
                                    cleanValue(currentProfile.getPhone()),
                                    cleanValue(currentProfile.getGender())
                            );
                        })
        );

        dialog.show();
    }

    private void showEditFieldDialog(
            String title,
            TextView targetView,
            int inputType,
            boolean required,
            String fieldKind,
            int maximumLength
    ) {
        if (currentProfile == null) {
            showToast("Load the member profile before editing it.");
            return;
        }

        EditText input = new EditText(this);
        input.setInputType(inputType);
        input.setSingleLine(true);
        input.setFilters(
                new InputFilter[]{new InputFilter.LengthFilter(maximumLength)}
        );

        String currentValue = targetView.getText().toString();
        if (!NOT_PROVIDED.equals(currentValue)) {
            input.setText(currentValue);
            input.setSelection(currentValue.length());
        }

        int paddingPx = getDialogPaddingPx();
        FrameLayout inputContainer = new FrameLayout(this);
        inputContainer.setPadding(
                paddingPx,
                paddingPx / 2,
                paddingPx,
                0
        );
        inputContainer.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(inputContainer)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(saveClick -> {
                            String newValue = input
                                    .getText().toString().trim();

                            if (required && newValue.isEmpty()) {
                                input.setError("This field can't be empty");
                                return;
                            }

                            if (FIELD_EMAIL.equals(fieldKind)
                                    && !Patterns.EMAIL_ADDRESS
                                    .matcher(newValue).matches()) {
                                input.setError("Enter a valid email address");
                                return;
                            }

                            if (FIELD_PHONE.equals(fieldKind)
                                    && !newValue.isEmpty()
                                    && !Patterns.PHONE
                                    .matcher(newValue).matches()) {
                                input.setError("Enter a valid phone number");
                                return;
                            }

                            String email = cleanValue(
                                    currentProfile.getEmail()
                            );
                            String phone = cleanValue(
                                    currentProfile.getPhone()
                            );
                            String gender = cleanValue(
                                    currentProfile.getGender()
                            );

                            if (FIELD_EMAIL.equals(fieldKind)) {
                                email = newValue;
                            } else if (FIELD_PHONE.equals(fieldKind)) {
                                phone = newValue;
                            } else if (FIELD_GENDER.equals(fieldKind)) {
                                gender = newValue;
                            }

                            saveProfileUpdate(
                                    dialog,
                                    input,
                                    cleanValue(currentProfile.getFirstName()),
                                    cleanValue(currentProfile.getLastName()),
                                    email,
                                    phone,
                                    gender
                            );
                        })
        );

        dialog.show();
    }

    private void saveProfileUpdate(
            AlertDialog dialog,
            EditText errorInput,
            String firstName,
            String lastName,
            String email,
            String phone,
            String gender
    ) {
        int memberId = memberSession.getMemberId();

        if (memberId <= 0) {
            errorInput.setError("No logged-in member was found");
            return;
        }

        if (firstName.isEmpty()
                || lastName.isEmpty()
                || email.isEmpty()) {
            errorInput.setError("Reload the profile before saving changes");
            return;
        }

        UpdateMemberProfileRequest request =
                new UpdateMemberProfileRequest(
                        firstName,
                        lastName,
                        email,
                        emptyToNull(phone),
                        emptyToNull(gender)
                );

        showSaving();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setEnabled(false);

        profileUpdateCall = ApiClient
                .getMemberApiService()
                .updateMemberProfile(memberId, request);

        profileUpdateCall.enqueue(
                new Callback<MemberProfileResponse>() {
                    @Override
                    public void onResponse(
                            Call<MemberProfileResponse> call,
                            Response<MemberProfileResponse> response
                    ) {
                        MemberProfileResponse updatedProfile = response.body();

                        if (response.isSuccessful()
                                && updatedProfile != null) {
                            displayMemberProfile(updatedProfile);
                            dialog.dismiss();
                            showToast("Profile updated successfully.");
                            return;
                        }

                        finishSavingAfterFailure();
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                                .setEnabled(true);

                        if (response.code() == 409) {
                            errorInput.setError(
                                    "That email is already used by another account"
                            );
                        } else if (response.code() == 400) {
                            errorInput.setError(
                                    "Check the value and try again"
                            );
                        } else if (response.code() == 404) {
                            showToast("Member profile not found.");
                        } else {
                            showToast(
                                    "Profile changes could not be saved."
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<MemberProfileResponse> call,
                            Throwable throwable
                    ) {
                        if (call.isCanceled()) {
                            return;
                        }

                        finishSavingAfterFailure();
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                                .setEnabled(true);
                        showToast(
                                "Unable to connect. Your changes were not saved."
                        );
                    }
                }
        );
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

        if (memberProfileCall != null
                && !memberProfileCall.isCanceled()) {
            memberProfileCall.cancel();
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
        currentProfile = null;
        setEditingEnabled(false);

        progressProfileDetails.setVisibility(View.VISIBLE);
        txtStatus.setVisibility(View.VISIBLE);
        txtStatus.setText("Loading your profile details...");
        btnRetryProfileDetails.setVisibility(View.GONE);

        txtHeaderName.setText("Loading profile...");
        setHeaderMemberNumber(memberNumber);
        clearDetailValues();
    }

    private void showSaving() {
        setEditingEnabled(false);
        progressProfileDetails.setVisibility(View.VISIBLE);
        txtStatus.setVisibility(View.VISIBLE);
        txtStatus.setText("Saving your profile changes...");
        btnRetryProfileDetails.setVisibility(View.GONE);
    }

    private void finishSavingAfterFailure() {
        progressProfileDetails.setVisibility(View.GONE);
        txtStatus.setVisibility(View.GONE);
        btnRetryProfileDetails.setVisibility(View.GONE);
        setEditingEnabled(currentProfile != null);
    }

    private void displayMemberProfile(MemberProfileResponse profile) {
        currentProfile = profile;

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

        setEditingEnabled(true);
    }

    private void showUnavailable(
            String message,
            String memberNumber
    ) {
        currentProfile = null;
        setEditingEnabled(false);

        progressProfileDetails.setVisibility(View.GONE);
        txtStatus.setVisibility(View.VISIBLE);
        txtStatus.setText(message);
        btnRetryProfileDetails.setVisibility(View.VISIBLE);

        txtHeaderName.setText("Profile unavailable");
        setHeaderMemberNumber(memberNumber);
        clearDetailValues();
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

    private void setEditingEnabled(boolean enabled) {
        setRowEnabled(rowName, enabled);
        setRowEnabled(rowEmail, enabled);
        setRowEnabled(rowPhone, enabled);
        setRowEnabled(rowGender, enabled);
    }

    private void setRowEnabled(View row, boolean enabled) {
        row.setEnabled(enabled);
        row.setAlpha(enabled ? 1.0f : 0.55f);
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
        txtName.setText(NOT_PROVIDED);
        txtEmail.setText(NOT_PROVIDED);
        txtPhone.setText(NOT_PROVIDED);
        txtGender.setText(NOT_PROVIDED);
    }

    private String displayValue(String value) {
        String cleanedValue = cleanValue(value);

        return cleanedValue.isEmpty()
                ? NOT_PROVIDED
                : cleanedValue;
    }

    private String cleanValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String emptyToNull(String value) {
        String cleanedValue = cleanValue(value);
        return cleanedValue.isEmpty() ? null : cleanedValue;
    }

    private int getDialogPaddingPx() {
        return (int) (
                20 * getResources().getDisplayMetrics().density
        );
    }

    private void showToast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    protected void onDestroy() {
        if (memberProfileCall != null
                && !memberProfileCall.isCanceled()) {
            memberProfileCall.cancel();
        }

        if (profileUpdateCall != null
                && !profileUpdateCall.isCanceled()) {
            profileUpdateCall.cancel();
        }

        super.onDestroy();
    }
}