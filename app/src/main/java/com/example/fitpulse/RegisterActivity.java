package com.example.fitpulse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Registration screen:
 * - Collects name, email, password.
 * - Saves them to SharedPreferences ("user_data").
 * - Navigates back to LoginActivity after success.
 */
public class RegisterActivity extends AppCompatActivity {

    // Input fields
    private EditText edtEmail, edtPassword, edtName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // inflate layout

        // Bind views
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        edtName = findViewById(R.id.edt_name);
        Button btnRegister = findViewById(R.id.btn_register);

        // Handle registration button click
        btnRegister.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString();
            String name = edtName.getText().toString().trim();

            // Basic validation
            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save user data locally
            SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
            prefs.edit()
                    .putString("email", email)
                    .putString("password", password)
                    .putString("name", name)
                    .apply();

            // Confirm and go back to login
            Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
