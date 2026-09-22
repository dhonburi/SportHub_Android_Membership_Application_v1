package com.example.sporthubandroidmembershipapplicationv1;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.sporthubandroidmembershipapplicationv1.models.LoginRequest;
import com.example.sporthubandroidmembershipapplicationv1.models.LoginResponse;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;
import com.example.sporthubandroidmembershipapplicationv1.session.MemberSession;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String LOG_TAG = "SPORT_HUB_LOGIN";

    /*
     * false = use existing mock accounts
     * true  = use the Azure database-backed API
     */
    private static final boolean USE_API_LOGIN = true;

    private final String[] validUsernames = {
            "kyle",
            "solomon",
            "dhon",
            "member",
            "admin"
    };

    private final String[] validPasswords = {
            "Kyle123!@",
            "Solomon123!@",
            "Dhon123!@",
            "Member123!@",
            "Admin123!@"
    };

    private boolean loginInProgress = false;
    private Call<LoginResponse> loginCall;

    private TextInputLayout loginEmailField;
    private TextInputLayout loginPasswordField;
    private boolean registrationConfirmationShowing = false;

    private final ActivityResultLauncher<Intent> registrationLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        Intent data = result.getData();

                        if (result.getResultCode() != Activity.RESULT_OK
                                || data == null) {
                            return;
                        }

                        String registeredEmail = data.getStringExtra(
                                RegisterActivity.EXTRA_REGISTERED_EMAIL
                        );

                        if (registeredEmail != null
                                && !registeredEmail.trim().isEmpty()) {

                            ShowRegistrationConfirmation(registeredEmail);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        loginEmailField = findViewById(R.id.loginEmailField);
        loginPasswordField = findViewById(R.id.loginPasswordField);

        EditText loginEmailInput =
                findViewById(R.id.editTextEmailAddress);

        EditText loginPasswordInput =
                findViewById(R.id.editTextPassword);

        loginPasswordInput.setSaveEnabled(false);

        AttachErrorClearing(loginEmailInput, loginEmailField);
        AttachErrorClearing(loginPasswordInput, loginPasswordField);

        loginPasswordInput.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        CheckLogin(findViewById(R.id.btnLogin));
                        return true;
                    }

                    return false;
                }
        );

        if (!USE_API_LOGIN) {
            TextView emailLabel = findViewById(R.id.loginEmailLabel);
            emailLabel.setText("Username *");
            loginEmailInput.setHint("Enter username");
            loginEmailInput.setInputType(InputType.TYPE_CLASS_TEXT);
        }

        getWindow().setNavigationBarColor(
                Color.parseColor("#181818")
        );

        WindowInsetsControllerCompat controller =
                ViewCompat.getWindowInsetsController(
                        getWindow().getDecorView()
                );

        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.displayCutout()
                                    | WindowInsetsCompat.Type.ime()
                    );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return WindowInsetsCompat.CONSUMED;
                }
        );

        Button forgotPasswordButton =
                findViewById(R.id.btnForgotPassword);

        Button createAccountButton =
                findViewById(R.id.btnCreateAccount);

        forgotPasswordButton.setOnClickListener(view ->
                Toast.makeText(
                        LoginActivity.this,
                        "Forgot password will be added later.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        createAccountButton.setOnClickListener(view -> {

            if (loginInProgress) {
                return;
            }

            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );

            registrationLauncher.launch(intent);
        });
    }

    public void CheckLogin(View view) {

        if (loginInProgress) {
            return;
        }

        ClearLoginMessages();

        EditText usernameInput =
                findViewById(R.id.editTextEmailAddress);

        EditText passwordInput =
                findViewById(R.id.editTextPassword);

        String usernameOrEmail = usernameInput
                .getText()
                .toString()
                .trim();

        // Preserve the password exactly as entered.
        String password = passwordInput
                .getText()
                .toString();

        boolean valid = true;

        if (usernameOrEmail.isEmpty()) {
            loginEmailField.setError(
                    USE_API_LOGIN
                            ? "Email address is required."
                            : "Username is required."
            );

            valid = false;
        } else if (USE_API_LOGIN
                && !Patterns.EMAIL_ADDRESS
                .matcher(usernameOrEmail)
                .matches()) {

            loginEmailField.setError(
                    "Enter a valid email address."
            );

            valid = false;
        }

        if (password.isEmpty()) {
            loginPasswordField.setError(
                    "Password is required."
            );

            valid = false;
        }

        if (!valid) {
            IncorrectLogin(
                    "Please correct the highlighted details."
            );

            if (loginEmailField.getError() != null) {
                usernameInput.requestFocus();
            } else {
                passwordInput.requestFocus();
            }

            return;
        }

        // Registration rules must not prevent existing accounts signing in.
        if (USE_API_LOGIN) {
            LoginWithApi(
                    view,
                    usernameOrEmail,
                    password
            );
        } else {
            LoginWithMockAccount(
                    usernameOrEmail,
                    password
            );
        }
    }

    private void LoginWithMockAccount(
            String username,
            String password
    ) {
        String lowercaseUsername =
                username.toLowerCase(Locale.ROOT);

        for (int i = 0; i < validUsernames.length; i++) {

            if (lowercaseUsername.equals(validUsernames[i])
                    && password.equals(validPasswords[i])) {

                String formattedUsername =
                        validUsernames[i]
                                .substring(0, 1)
                                .toUpperCase(Locale.ROOT)
                                + validUsernames[i].substring(1);

                LaunchHome(formattedUsername);
                return;
            }
        }

        IncorrectLogin(
                "Incorrect Username or Password"
        );
    }

    private void LoginWithApi(
            View loginButton,
            String email,
            String password
    ) {
        SetLoginLoading(
                loginButton,
                true
        );

        Log.d(
                LOG_TAG,
                "Starting Azure login request for: " + email
        );

        LoginRequest loginRequest =
                new LoginRequest(
                        email,
                        password
                );

        loginCall = ApiClient
                .getAuthApiService()
                .login(loginRequest);

        loginCall.enqueue(
                new Callback<LoginResponse>() {

                    @Override
                    public void onResponse(
                            Call<LoginResponse> call,
                            Response<LoginResponse> response
                    ) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        SetLoginLoading(
                                loginButton,
                                false
                        );

                        Log.d(
                                LOG_TAG,
                                "Received HTTP response: "
                                        + response.code()
                        );

                        LoginResponse loginResponse =
                                response.body();

                        if (response.isSuccessful()
                                && loginResponse != null
                                && loginResponse.isSuccess()) {

                            Log.d(
                                    LOG_TAG,
                                    "Login successful. Member number: "
                                            + loginResponse.getMemberNumber()
                            );

                            LaunchHomeFromApi(
                                    email,
                                    loginResponse
                            );

                            return;
                        }

                        String errorMessage =
                                "Invalid email or password";

                        if (response.code() >= 500) {
                            errorMessage =
                                    "Sign-in is temporarily unavailable. Please try again.";
                        } else if (response.code() == 429) {
                            errorMessage =
                                    "Too many sign-in attempts. Please wait and try again.";
                        } else if (response.code() == 404
                                || response.code() == 405) {
                            errorMessage =
                                    "Sign-in is currently unavailable. Please try again later.";
                        } else if (response.isSuccessful()
                                && loginResponse != null
                                && loginResponse.getMessage() != null
                                && !loginResponse
                                .getMessage()
                                .trim()
                                .isEmpty()) {

                            errorMessage =
                                    loginResponse.getMessage();
                        }

                        Log.e(
                                LOG_TAG,
                                "Login rejected. HTTP status: "
                                        + response.code()
                                        + ". Message: "
                                        + errorMessage
                        );

                        IncorrectLogin(errorMessage);
                    }

                    @Override
                    public void onFailure(
                            Call<LoginResponse> call,
                            Throwable throwable
                    ) {
                        if (call.isCanceled()
                                || isFinishing()
                                || isDestroyed()) {

                            return;
                        }

                        SetLoginLoading(
                                loginButton,
                                false
                        );

                        Log.e(
                                LOG_TAG,
                                "Azure login request failed",
                                throwable
                        );

                        IncorrectLogin(
                                "Unable to connect. Check your internet connection and try again."
                        );
                    }
                }
        );
    }

    private void SetLoginLoading(
            View loginButton,
            boolean loading
    ) {
        loginInProgress = loading;

        loginButton.setEnabled(!loading);
        loginEmailField.setEnabled(!loading);
        loginPasswordField.setEnabled(!loading);

        Button createAccountButton =
                findViewById(R.id.btnCreateAccount);

        createAccountButton.setEnabled(!loading);

        loginButton.setAlpha(
                loading ? 0.6f : 1f
        );
    }

    private void LaunchHomeFromApi(
            String email,
            LoginResponse loginResponse
    ) {
        if (loginResponse.getMemberId() == null
                || loginResponse.getMemberNumber() == null
                || loginResponse
                .getMemberNumber()
                .trim()
                .isEmpty()) {

            IncorrectLogin(
                    "Your member profile could not be loaded."
            );

            return;
        }

        MemberSession memberSession =
                new MemberSession(this);

        memberSession.save(
                loginResponse.getMemberId(),
                loginResponse.getMemberNumber(),
                loginResponse.isAdmin(),
                loginResponse.getStaffAccessToken()
        );

        String displayName =
                FormatDisplayNameFromEmail(email);

        Intent intent = new Intent(
                LoginActivity.this,
                HomeActivity.class
        );

        intent.putExtra(
                "Username",
                displayName
        );

        if (loginResponse.getUserId() != null) {
            intent.putExtra(
                    "UserId",
                    loginResponse.getUserId()
            );
        }

        intent.putExtra(
                "MemberId",
                loginResponse.getMemberId()
        );

        intent.putExtra(
                "MemberNumber",
                loginResponse.getMemberNumber()
        );

        intent.putExtra(
                "OPEN_FRAGMENT",
                "QR"
        );

        startActivity(intent);
        finish();
    }

    private String FormatDisplayNameFromEmail(
            String email
    ) {
        String displayName = email;

        int atPosition =
                displayName.indexOf("@");

        if (atPosition > 0) {
            displayName =
                    displayName.substring(
                            0,
                            atPosition
                    );
        }

        if (displayName.isEmpty()) {
            return "Member";
        }

        return displayName
                .substring(0, 1)
                .toUpperCase(Locale.ROOT)
                + displayName.substring(1);
    }

    public void LaunchHome(String username) {
        Intent intent = new Intent(
                LoginActivity.this,
                HomeActivity.class
        );

        intent.putExtra(
                "Username",
                username
        );

        intent.putExtra(
                "OPEN_FRAGMENT",
                "QR"
        );

        startActivity(intent);
        finish();
    }

    private void AttachErrorClearing(
            EditText input,
            TextInputLayout field
    ) {
        input.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence text,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence text,
                            int start,
                            int before,
                            int count
                    ) {
                        field.setError(null);

                        if (!registrationConfirmationShowing) {
                            ClearGeneralLoginMessage();
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable text) {
                    }
                }
        );
    }

    private void ClearGeneralLoginMessage() {
        TextView alertText =
                findViewById(R.id.alertText);

        alertText.setVisibility(View.GONE);
        alertText.setAlpha(1f);

        registrationConfirmationShowing = false;
    }

    private void ClearLoginMessages() {
        loginEmailField.setError(null);
        loginPasswordField.setError(null);

        ClearGeneralLoginMessage();
    }

    private void ShowRegistrationConfirmation(String registeredEmail) {
        ClearLoginMessages();

        EditText usernameInput =
                findViewById(R.id.editTextEmailAddress);

        EditText passwordInput =
                findViewById(R.id.editTextPassword);

        usernameInput.setText(registeredEmail);
        passwordInput.setText("");
        passwordInput.requestFocus();

        TextView alertText =
                findViewById(R.id.alertText);

        alertText.setAlpha(1f);

        alertText.setTextColor(
                Color.parseColor("#1B5E20")
        );

        alertText.setText(
                "Account created successfully. Sign in with your new password."
        );

        alertText.setVisibility(View.VISIBLE);
        registrationConfirmationShowing = true;

        Toast.makeText(
                LoginActivity.this,
                "Account created successfully.",
                Toast.LENGTH_LONG
        ).show();
    }

    public void IncorrectLogin(String message) {
        TextView alertText =
                findViewById(R.id.alertText);

        registrationConfirmationShowing = false;

        alertText.setTextColor(
                Color.parseColor("#B3261E")
        );

        alertText.setText(message);
        alertText.setVisibility(View.VISIBLE);
        alertText.setAlpha(1f);
    }

    @Override
    protected void onDestroy() {
        if (loginCall != null
                && !loginCall.isCanceled()) {

            loginCall.cancel();
        }

        super.onDestroy();
    }
}
