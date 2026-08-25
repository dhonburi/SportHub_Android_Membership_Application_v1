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
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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

import java.util.regex.Pattern;

public class ProfileDetailsActivity extends AppCompatActivity {

    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_PHONE = "phone";
    private static final String NOT_PROVIDED = "Not provided";

    private static final int MINIMUM_NAME_LENGTH = 2;
    private static final int MAXIMUM_NAME_LENGTH = 50;
    private static final int MAXIMUM_PHONE_LENGTH = 11;

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[\\p{L} ]+$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\d{9,11}$"
    );

    private static final String[] GENDER_OPTIONS = {
            "Select gender",
            "Male",
            "Female",
            "Rather Not Say"
    };

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
                        InputType.TYPE_CLASS_NUMBER,
                        false,
                        FIELD_PHONE,
                        MAXIMUM_PHONE_LENGTH
                )
        );

        rowGender.setOnClickListener(view -> showEditGenderDialog());
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
                new InputFilter[]{
                        new InputFilter.LengthFilter(MAXIMUM_NAME_LENGTH)
                }
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
                new InputFilter[]{
                        new InputFilter.LengthFilter(MAXIMUM_NAME_LENGTH)
                }
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
                            String firstName = normalizeName(
                                    firstNameInput.getText().toString()
                            );

                            String lastName = normalizeName(
                                    lastNameInput.getText().toString()
                            );

                            boolean firstNameValid = validateName(
                                    firstNameInput,
                                    firstName,
                                    "First name"
                            );

                            boolean lastNameValid = validateName(
                                    lastNameInput,
                                    lastName,
                                    "Last name"
                            );

                            if (!firstNameValid || !lastNameValid) {
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

                            if (FIELD_EMAIL.equals(fieldKind)
                                    && !validateEmail(input, newValue)) {
                                return;
                            }

                            if (FIELD_PHONE.equals(fieldKind)
                                    && !validatePhone(input, newValue)) {
                                return;
                            }

                            if (required && newValue.isEmpty()) {
                                input.setError("This field is required.");
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

    private void showEditGenderDialog() {
        if (currentProfile == null) {
            showToast("Load the member profile before editing it.");
            return;
        }

        Spinner genderSpinner = new Spinner(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                GENDER_OPTIONS
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        genderSpinner.setAdapter(adapter);
        genderSpinner.setSelection(
                findGenderSelection(currentProfile.getGender())
        );

        TextView errorMessage = new TextView(this);
        errorMessage.setTextColor(Color.parseColor("#B00020"));
        errorMessage.setTextSize(12);
        errorMessage.setVisibility(View.GONE);

        LinearLayout inputContainer = new LinearLayout(this);
        inputContainer.setOrientation(LinearLayout.VERTICAL);

        int paddingPx = getDialogPaddingPx();
        inputContainer.setPadding(
                paddingPx,
                paddingPx / 2,
                paddingPx,
                0
        );

        inputContainer.addView(genderSpinner);
        inputContainer.addView(errorMessage);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit gender")
                .setView(inputContainer)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(saveClick -> {
                            int selectedPosition =
                                    genderSpinner.getSelectedItemPosition();

                            if (selectedPosition <= 0) {
                                errorMessage.setText(
                                        "Please select a gender."
                                );
                                errorMessage.setVisibility(View.VISIBLE);
                                return;
                            }

                            errorMessage.setVisibility(View.GONE);

                            String selectedGender = GENDER_OPTIONS[
                                    selectedPosition
                                    ];

                            saveProfileUpdate(
                                    dialog,
                                    null,
                                    cleanValue(currentProfile.getFirstName()),
                                    cleanValue(currentProfile.getLastName()),
                                    cleanValue(currentProfile.getEmail()),
                                    cleanValue(currentProfile.getPhone()),
                                    selectedGender
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
            showFieldError(
                    errorInput,
                    "No logged-in member was found."
            );
            return;
        }

        if (firstName.isEmpty()
                || lastName.isEmpty()
                || email.isEmpty()) {
            showFieldError(
                    errorInput,
                    "Reload the profile before saving changes."
            );
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
                            showFieldError(
                                    errorInput,
                                    "That email is already used by another account."
                            );
                        } else if (response.code() == 400) {
                            showFieldError(
                                    errorInput,
                                    "Check the field value and try again."
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

    private String normalizeName(String value) {
        return cleanValue(value).replaceAll("\\s+", " ");
    }

    private boolean validateName(
            EditText input,
            String value,
            String fieldLabel
    ) {
        if (value.isEmpty()) {
            input.setError(fieldLabel + " is required.");
            return false;
        }

        if (!NAME_PATTERN.matcher(value).matches()) {
            input.setError(
                    fieldLabel + " must only contain letters and spaces."
            );
            return false;
        }

        if (value.length() < MINIMUM_NAME_LENGTH
                || value.length() > MAXIMUM_NAME_LENGTH) {
            input.setError(
                    fieldLabel
                            + " must be between 2 and 50 characters."
            );
            return false;
        }

        input.setError(null);
        return true;
    }

    private boolean validateEmail(EditText input, String value) {
        if (value.isEmpty()) {
            input.setError("Email address is required.");
            return false;
        }

        if (containsWhitespace(value)
                || !hasRequiredEmailParts(value)
                || !Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            input.setError(
                    "Email must include @ and a valid domain, "
                            + "such as name@example.com."
            );
            return false;
        }

        input.setError(null);
        return true;
    }

    private boolean validatePhone(EditText input, String value) {
        if (value.isEmpty()) {
            input.setError(null);
            return true;
        }

        if (!value.matches("\\d+")) {
            input.setError("Phone number must only contain numbers.");
            return false;
        }

        if (!PHONE_PATTERN.matcher(value).matches()) {
            input.setError(
                    "Phone number must contain between 9 and 11 digits."
            );
            return false;
        }

        input.setError(null);
        return true;
    }

    private boolean containsWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return true;
            }
        }

        return false;
    }

    private boolean hasRequiredEmailParts(String value) {
        int atIndex = value.indexOf('@');
        int finalDotIndex = value.lastIndexOf('.');

        return atIndex > 0
                && finalDotIndex > atIndex + 1
                && finalDotIndex < value.length() - 1;
    }

    private int findGenderSelection(String currentGender) {
        String cleanedGender = cleanValue(currentGender);

        for (int index = 1; index < GENDER_OPTIONS.length; index++) {
            if (GENDER_OPTIONS[index].equalsIgnoreCase(cleanedGender)) {
                return index;
            }
        }

        return 0;
    }

    private void showFieldError(EditText input, String message) {
        if (input != null) {
            input.setError(message);
            return;
        }

        showToast(message);
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