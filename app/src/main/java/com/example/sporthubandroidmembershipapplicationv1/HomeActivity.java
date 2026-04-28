package com.example.sporthubandroidmembershipapplicationv1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HomeActivity extends AppCompatActivity {
    String Username;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent i = getIntent();
        Username = i.getStringExtra("Username");
        View v = findViewById(R.id.textView);
        String welcomeMessage = "Welcome " + Username + "!";
        ((TextView)v).setText(welcomeMessage);
    }

    public void Logout(){
        // Open the login page and clear session

        // TEMPORARY - MAKE IT MORE SECURE
        Username = "";

        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
    }
}