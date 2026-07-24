package com.example.sporthubandroidmembershipapplicationv1;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

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

        EditText usernameInput =
                findViewById(R.id.editTextEmailAddress);

        EditText passwordInput =
                findViewById(R.id.editTextPassword);

        String username = usernameInput
                .getText()
                .toString()
                .trim();

        String password = passwordInput
                .getText()
                .toString();

        if (username.isEmpty()) {
            IncorrectLogin(
                    "Please enter a Username/Email"
            );
            return;
        }

        if (password.isEmpty()) {
            IncorrectLogin(
                    "Please enter a Password"
            );
            return;
        }

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

    public void LaunchHome(String username) {

        Intent intent = new Intent(
                LoginActivity.this,
                HomeActivity.class
        );

        intent.putExtra(
                "Username",
                username
        );

        // Keeps your current login → QR behaviour
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
}