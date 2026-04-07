package com.example.fitnesapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.fitnesapp.MainActivity;
import com.example.fitnesapp.R;
import com.example.fitnesapp.ui.setupprofile.SetupProfileActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AuthActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private CardView btnGoogle;
    private TextView tvRegister;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // 1. Инициализация Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Инициализация UI
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        tvRegister = findViewById(R.id.tvRegister);

        // 3. Настройка Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 4. Обработчики нажатий
        btnLogin.setOnClickListener(v -> loginWithEmail());

        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(AuthActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    // --- ПРОВЕРКА ПРИ ЗАПУСКЕ (АВТО-ВХОД) ---
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkProfileAndRedirect(currentUser.getUid());
        }
    }

    // --- ЛОГИКА ВХОДА ПО EMAIL ---
    private void loginWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.auth_error_enter_email));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.auth_error_enter_password));
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        checkProfileAndRedirect(mAuth.getCurrentUser().getUid());
                    } else {
                        Toast.makeText(AuthActivity.this,
                                getString(R.string.auth_error_failed, task.getException().getMessage()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- ЛОГИКА ВХОДА ЧЕРЕЗ GOOGLE ---
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Передаем код ошибки как аргумент
                Toast.makeText(this,
                        getString(R.string.auth_error_google_failed, e.getStatusCode()),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        checkProfileAndRedirect(mAuth.getCurrentUser().getUid());
                    } else {
                        Toast.makeText(AuthActivity.this,
                                getString(R.string.auth_error_firebase_failed),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- ГЛАВНАЯ ЛОГИКА ПЕРЕНАПРАВЛЕНИЯ ---
    private void checkProfileAndRedirect(String uid) {
        DatabaseReference goalsRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("profile");

        goalsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(AuthActivity.this, SetupProfileActivity.class);
                    startActivity(intent);
                }
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Заменяем русский текст на локализованный
                Toast.makeText(AuthActivity.this,
                        getString(R.string.auth_error_loading, error.getMessage()),
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AuthActivity.this, SetupProfileActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}