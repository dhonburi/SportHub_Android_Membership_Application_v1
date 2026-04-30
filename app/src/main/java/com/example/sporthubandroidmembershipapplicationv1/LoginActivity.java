package com.example.sporthubandroidmembershipapplicationv1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {

    String[] validUsernames = {
            "kyle",
            "solomon",
            "dhon",
            "member",
            "admin"
    };

    String[] validPasswords = {
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    public void CheckLogin(View v){
        EditText userText = findViewById(R.id.editTextEmailAddress);
        String username = userText.getText().toString();

        if (username.isEmpty()) {
            IncorrectLogin("Please enter a Username/Email");
            return;
        }

        EditText passText = findViewById(R.id.editTextPassword);
        String password = passText.getText().toString();

        if (password.isEmpty()) {
            IncorrectLogin("Please enter a Password");
            return;
        }


        for (int i = 0; i < validUsernames.length; i++) {
            if (username.toLowerCase().equals(validUsernames[i]) &&
                    password.equals(validPasswords[i])) {
                LaunchHome(validUsernames[i].substring(0, 1).toUpperCase() + validUsernames[i].substring(1));
                return;
            }
        }

        IncorrectLogin("Incorrect Username or Password");

    }

    public void LaunchHome(String username){
        // Open the home page once Login is good
        Intent i = new Intent(this, HomeActivity.class);
        i.putExtra("Username", username);
        startActivity(i);
    }

    public void IncorrectLogin(String message){
        View v = findViewById(R.id.alertText);

        // show it first
        ((TextView)v).setText(message);
        v.setVisibility(View.VISIBLE);
        v.setAlpha(1f);

        // wait 1.5 seconds, then fade out
        v.postDelayed(() -> {

            v.animate()
                    .alpha(0f)
                    .setDuration(500) // fade duration
                    .withEndAction(() -> {
                        v.setVisibility(View.INVISIBLE);
                        v.setAlpha(1f); // reset for next time
                    });

        }, 1500); // delay before fade starts
    }
}