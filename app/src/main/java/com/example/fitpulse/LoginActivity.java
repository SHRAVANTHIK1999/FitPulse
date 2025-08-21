package com.example.fitpulse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Login screen:
 * - Reads saved credentials from SharedPreferences ("user_data").
 * - If already logged in, skips directly to MainActivity.
 * - On login button: validates against saved email/password and sets "logged_in".
 */
public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button btnLogin;
    private TextView registerLink;

    // App-wide preferences used to store user data and login state
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // inflate layout

        // Bind views
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        btnLogin = findViewById(R.id.btn_login);
        registerLink = findViewById(R.id.register_link);

        // Open the preferences file where RegisterActivity saved user info
        prefs = getSharedPreferences("user_data", MODE_PRIVATE);

        // Skip login if already logged in
        boolean isLoggedIn = prefs.getBoolean("logged_in", false);
        if (isLoggedIn) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        btnLogin.setOnClickListener(v -> {
            // Read user input
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            // Read saved credentials
            String savedEmail = prefs.getString("email", null);
            String savedPassword = prefs.getString("password", null);

            // Compare input with saved values
            if (email.equals(savedEmail) && password.equals(savedPassword)) {
                // Save login state
                prefs.edit().putBoolean("logged_in", true).apply();

                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class)); // go to Home
                finish(); // prevent back navigation to login
            } else {
                Toast.makeText(this, "Invalid email or password!", Toast.LENGTH_SHORT).show();
            }
        });

        // Navigate to registration screen
        registerLink.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
}
