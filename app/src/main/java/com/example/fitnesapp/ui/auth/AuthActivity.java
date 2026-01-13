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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
        // R.string.default_web_client_id генерируется автоматически файлом google-services.json
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
        // Если пользователь уже вошел ранее, проверяем профиль и перекидываем
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
            etEmail.setError("Enter email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter password");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Успех -> Проверяем, заполнен ли профиль
                        checkProfileAndRedirect(mAuth.getCurrentUser().getUid());
                    } else {
                        Toast.makeText(AuthActivity.this, "Authentication failed: " + task.getException().getMessage(),
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

        // Результат от окна выбора аккаунта Google
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google вход успешен, теперь авторизуемся в Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed code: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Успех -> Проверяем профиль
                        checkProfileAndRedirect(mAuth.getCurrentUser().getUid());
                    } else {
                        Toast.makeText(AuthActivity.this, "Firebase Auth failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- ГЛАВНАЯ ЛОГИКА ПЕРЕНАПРАВЛЕНИЯ ---
    // Решает, куда отправить пользователя: в Главное меню или Заполнять данные
    private void checkProfileAndRedirect(String uid) {
        DatabaseReference profileRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("profile");

        profileRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                DataSnapshot snapshot = task.getResult();

                // Проверяем наличие веса как индикатор заполненного профиля
                Double weight = snapshot.child("weight").getValue(Double.class);

                if (weight != null && weight > 0) {
                    // АНКЕТА ЗАПОЛНЕНА -> Идем в приложение
                    Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish(); // Закрываем экран входа
                } else {
                    // ПРОФИЛЬ ЕСТЬ, НО ПУСТОЙ -> Заполнять данные
                    Intent intent = new Intent(AuthActivity.this, SetupProfileActivity.class);
                    startActivity(intent);
                    finish();
                }
            } else {
                // ПРОФИЛЯ ВООБЩЕ НЕТ (например, новый Google юзер) -> Заполнять данные
                Intent intent = new Intent(AuthActivity.this, SetupProfileActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}