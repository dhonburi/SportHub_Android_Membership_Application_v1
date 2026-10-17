package com.example.sporthubandroidmembershipapplicationv1;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.sporthubandroidmembershipapplicationv1.models.RegisterRequest;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class RegisterActivity extends AppCompatActivity {
    public static final String EXTRA_REGISTERED_EMAIL = "registered_email";

    private static final String DOB_STATE = "selected_date";
    private static final String PENDING_STATE = "submission_pending";

    private final Map<String, TextInputLayout> fields = new LinkedHashMap<>();

    private RegistrationViewModel model;

    private TextInputEditText firstNameInput, lastNameInput, emailInput;
    private TextInputEditText passwordInput, confirmPasswordInput, phoneInput;
    private TextInputEditText dateInput;
    private MaterialAutoCompleteTextView genderInput;

    private Button submitButton, signInButton;
    private TextView errorText;
    private ProgressBar progress;

    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        WindowInsetsControllerCompat controller =
                ViewCompat.getWindowInsetsController(getWindow().getDecorView());

        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }

        View root = findViewById(R.id.registrationRoot);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.ime());

            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.requestApplyInsets(root);

        model = new ViewModelProvider(this).get(RegistrationViewModel.class);

        bindFields();

        submitButton = findViewById(R.id.btnRegister);
        signInButton = findViewById(R.id.btnBackToSignIn);
        errorText = findViewById(R.id.registrationError);
        progress = findViewById(R.id.registrationProgress);

        String[] genders = {
                "Not provided",
                "Male",
                "Female",
                "Rather Not Say"
        };

        genderInput.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                genders));

        genderInput.setKeyListener(null);
        genderInput.setShowSoftInputOnFocus(false);
        genderInput.setOnClickListener(view -> genderInput.showDropDown());

        dateInput.setKeyListener(null);
        dateInput.setShowSoftInputOnFocus(false);
        dateInput.setOnClickListener(view -> selectDate());

        if (savedInstanceState != null) {
            selectedDate = savedInstanceState.getString(DOB_STATE);
            showSelectedDate();
        }

        submitButton.setOnClickListener(view -> submitRegistration());
        signInButton.setOnClickListener(view -> finish());

        model.getState().observe(this, state -> {
            if (state == null || isFinishing()) {
                return;
            }

            setLoading(state.loading);

            if (state.success) {
                passwordInput.setText("");
                confirmPasswordInput.setText("");

                Intent result = new Intent().putExtra(
                        EXTRA_REGISTERED_EMAIL,
                        model.getSubmittedEmail());

                setResult(Activity.RESULT_OK, result);
                finish();
                return;
            }

            clearFieldErrors();

            if (!state.message.isEmpty()) {
                showError(state.message);
                showServerErrors(state.errors);
            } else if (savedInstanceState != null
                    && savedInstanceState.getBoolean(PENDING_STATE)
                    && !state.loading) {
                showError(
                        "The app restarted before account creation was confirmed. "
                                + "Try signing in first, or re-enter your password and resubmit.");
            } else {
                errorText.setVisibility(View.GONE);
            }
        });

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (model.isLoading()) {
                            Toast.makeText(
                                    RegisterActivity.this,
                                    "Please wait for account creation to finish.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            finish();
                        }
                    }
                });
    }

    private void bindFields() {
        fields.put("firstname", findViewById(R.id.fieldFirstName));
        fields.put("lastname", findViewById(R.id.fieldLastName));
        fields.put("email", findViewById(R.id.fieldEmail));
        fields.put("password", findViewById(R.id.fieldPassword));
        fields.put("confirmpassword", findViewById(R.id.fieldConfirmPassword));
        fields.put("phone", findViewById(R.id.fieldPhone));
        fields.put("gender", findViewById(R.id.fieldGender));
        fields.put("dateofbirth", findViewById(R.id.fieldDateOfBirth));

        firstNameInput = findViewById(R.id.inputFirstName);
        lastNameInput = findViewById(R.id.inputLastName);
        emailInput = findViewById(R.id.inputEmail);
        passwordInput = findViewById(R.id.inputPassword);
        confirmPasswordInput = findViewById(R.id.inputConfirmPassword);
        phoneInput = findViewById(R.id.inputPhone);
        genderInput = findViewById(R.id.inputGender);
        dateInput = findViewById(R.id.inputDateOfBirth);
    }

    private void submitRegistration() {
        if (model.isLoading()) {
            return;
        }

        clearFieldErrors();
        errorText.setVisibility(View.GONE);

        String first = value(firstNameInput).trim();
        String last = value(lastNameInput).trim();
        String email = value(emailInput).trim().toLowerCase(Locale.ROOT);

        String password = value(passwordInput);
        String confirm = value(confirmPasswordInput);

        String phone = value(phoneInput).trim();
        String gender = genderInput.getText().toString().trim();

        validateName("firstname", first, "First name");
        validateName("lastname", last, "Last name");

        if (email.isEmpty()) {
            fields.get("email").setError("Email address is required.");
        } else if (email.length() > 256
                || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            fields.get("email").setError("Enter a valid email address.");
        }

        int passwordLength = password.codePointCount(0, password.length());

        if (password.trim().isEmpty()) {
            fields.get("password").setError("Password is required.");
        } else if (passwordLength < 15 || passwordLength > 128) {
            fields.get("password").setError("Use between 15 and 128 characters.");
        }

        if (confirm.isEmpty()) {
            fields.get("confirmpassword").setError("Please confirm your password.");
        } else if (!password.equals(confirm)) {
            fields.get("confirmpassword").setError("Passwords must match exactly.");
        }

        if (!phone.isEmpty() && !phone.matches("[0-9]{9,11}")) {
            fields.get("phone").setError(
                    "Use 9–11 digits, without spaces or a + prefix.");
        }

        if (gender.isEmpty() || gender.equals("Not provided")) {
            gender = null;
        } else if (!gender.equals("Male")
                && !gender.equals("Female")
                && !gender.equals("Rather Not Say")) {
            fields.get("gender").setError(
                    "Select one of the available options.");
        }

        if (selectedDate != null && selectedDate.compareTo(todayUtc()) > 0) {
            fields.get("dateofbirth").setError(
                    "Date of birth cannot be in the future.");
        }

        for (TextInputLayout field : fields.values()) {
            if (field.getError() != null) {
                showError("Please correct the highlighted details.");

                if (field.getEditText() != null) {
                    field.getEditText().requestFocus();
                }

                return;
            }
        }

        View focused = getCurrentFocus();

        if (focused != null) {
            InputMethodManager keyboard =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

            if (keyboard != null) {
                keyboard.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            }

            focused.clearFocus();
        }

        model.submit(new RegisterRequest(
                first,
                last,
                email,
                phone.isEmpty() ? null : phone,
                gender,
                selectedDate == null ? null : selectedDate + "T00:00:00",
                password,
                confirm));
    }

    private void validateName(String key, String name, String label) {
        if (name.isEmpty()) {
            fields.get(key).setError(label + " is required.");
        } else if (name.length() < 2 || name.length() > 50) {
            fields.get(key).setError("Use between 2 and 50 characters.");
        } else if (!name.matches("^[\\p{L} ]+$")) {
            fields.get(key).setError("Use letters and spaces only.");
        }
    }

    private void showServerErrors(Map<String, String[]> errors) {
        for (Map.Entry<String, String[]> entry : errors.entrySet()) {
            String key = entry.getKey();

            if (key == null) {
                continue;
            }

            key = key.substring(key.lastIndexOf('.') + 1)
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z]", "");

            TextInputLayout field = fields.get(key);
            String[] messages = entry.getValue();

            if (field != null
                    && messages != null
                    && messages.length > 0
                    && messages[0] != null) {
                field.setError(messages[0]);
            }
        }
    }

    private void selectDate() {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        int year = utc.get(Calendar.YEAR);
        int month = utc.get(Calendar.MONTH);
        int day = utc.get(Calendar.DAY_OF_MONTH);

        if (selectedDate != null) {
            String[] parts = selectedDate.split("-");

            year = Integer.parseInt(parts[0]);
            month = Integer.parseInt(parts[1]) - 1;
            day = Integer.parseInt(parts[2]);
        }

        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    selectedDate = String.format(
                            Locale.ROOT,
                            "%04d-%02d-%02d",
                            y,
                            m + 1,
                            d);

                    showSelectedDate();
                    fields.get("dateofbirth").setError(null);
                },
                year,
                month,
                day);

        Calendar maximum = Calendar.getInstance();

        maximum.set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                23,
                59,
                59);

        picker.getDatePicker().setMaxDate(maximum.getTimeInMillis());

        picker.setButton(
                DatePickerDialog.BUTTON_NEUTRAL,
                "Clear",
                (dialog, which) -> {
                    selectedDate = null;
                    dateInput.setText("");
                    fields.get("dateofbirth").setError(null);
                });

        picker.show();
    }

    private void showSelectedDate() {
        if (selectedDate != null) {
            String[] parts = selectedDate.split("-");

            dateInput.setText(
                    parts[2] + "/" + parts[1] + "/" + parts[0]);
        }
    }

    private static String todayUtc() {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        return String.format(
                Locale.ROOT,
                "%04d-%02d-%02d",
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH) + 1,
                utc.get(Calendar.DAY_OF_MONTH));
    }

    private static String value(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private void clearFieldErrors() {
        for (TextInputLayout field : fields.values()) {
            field.setError(null);
        }
    }

    private void setLoading(boolean loading) {
        submitButton.setEnabled(!loading);
        signInButton.setEnabled(!loading);

        submitButton.setText(
                loading ? "Creating account…" : "Create Account");

        submitButton.setAlpha(loading ? 0.6f : 1f);

        progress.setVisibility(
                loading ? View.VISIBLE : View.GONE);

        for (TextInputLayout field : fields.values()) {
            field.setEnabled(!loading);
        }
    }

    private void showError(String message) {
        errorText.setTextColor(Color.parseColor("#B3261E"));
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(DOB_STATE, selectedDate);
        outState.putBoolean(PENDING_STATE, model.isLoading());
    }
}