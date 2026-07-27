package com.example.sporthubandroidmembershipapplicationv1;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.sporthubandroidmembershipapplicationv1.models.LoginRequest;
import com.example.sporthubandroidmembershipapplicationv1.models.LoginResponse;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    /*
     * Keep this false until Dhon's login API is completed
     * and successfully tested through Swagger or Postman.
     *
     * false = use the existing mock accounts
     * true  = use the real database-backed API
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

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

        createAccountButton.setOnClickListener(view ->
                Toast.makeText(
                        LoginActivity.this,
                        "Account creation will be added later.",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    public void CheckLogin(View view) {

        // Prevent repeated login requests.
        if (loginInProgress) {
            return;
        }

        EditText usernameInput =
                findViewById(R.id.editTextEmailAddress);

        EditText passwordInput =
                findViewById(R.id.editTextPassword);

        String usernameOrEmail = usernameInput
                .getText()
                .toString()
                .trim();

        String password = passwordInput
                .getText()
                .toString();

        if (usernameOrEmail.isEmpty()) {
            IncorrectLogin(
                    USE_API_LOGIN
                            ? "Please enter your email address"
                            : "Please enter a Username/Email"
            );
            return;
        }

        if (password.isEmpty()) {
            IncorrectLogin(
                    "Please enter a Password"
            );
            return;
        }

        /*
         * The real backend requires an email address.
         * We only enforce email formatting while API login is enabled,
         * so the existing mock usernames continue working for now.
         */
        if (USE_API_LOGIN
                && !Patterns.EMAIL_ADDRESS
                .matcher(usernameOrEmail)
                .matches()) {

            IncorrectLogin(
                    "Please enter a valid email address"
            );
            return;
        }

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

                        LoginResponse loginResponse =
                                response.body();

                        if (response.isSuccessful()
                                && loginResponse != null
                                && loginResponse.isSuccess()) {

                            LaunchHomeFromApi(
                                    email,
                                    loginResponse
                            );

                            return;
                        }

                        String errorMessage =
                                "Invalid email or password";

                        if (loginResponse != null
                                && loginResponse.getMessage() != null
                                && !loginResponse
                                .getMessage()
                                .trim()
                                .isEmpty()) {

                            errorMessage =
                                    loginResponse.getMessage();
                        }

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

                        IncorrectLogin(
                                "Unable to connect to the server. Please try again."
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

        loginButton.setAlpha(
                loading ? 0.6f : 1f
        );
    }

    private void LaunchHomeFromApi(
            String email,
            LoginResponse loginResponse
    ) {

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

        if (loginResponse.getMemberId() != null) {
            intent.putExtra(
                    "MemberId",
                    loginResponse.getMemberId()
            );
        }

        if (loginResponse.getMemberNumber() != null) {
            intent.putExtra(
                    "MemberNumber",
                    loginResponse.getMemberNumber()
            );
        }

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

        // Keeps your current login → QR behaviour.
        intent.putExtra(
                "OPEN_FRAGMENT",
                "QR"
        );

        startActivity(intent);
        finish();
    }

    public void IncorrectLogin(String message) {

        TextView alertText =
                findViewById(R.id.alertText);

        alertText.setText(message);
        alertText.setVisibility(View.VISIBLE);
        alertText.setAlpha(1f);

        alertText.postDelayed(() ->

                        alertText.animate()
                                .alpha(0f)
                                .setDuration(500)
                                .withEndAction(() -> {
                                    alertText.setVisibility(
                                            View.INVISIBLE
                                    );

                                    alertText.setAlpha(1f);
                                }),

                1500
        );
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