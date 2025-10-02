package com.moneytrackultra.cashbook;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Functional login screen for activity_login.xml you posted.
 *
 * Layout IDs used:
 *  - etEmail
 *  - etPassword
 *  - btnLogin
 *  - btnBack
 *  - tvNeedAccount
 *
 * Features:
 *  - Online login via Firebase (email/password) using AuthManager.
 *  - Offline login (verifies locally stored password hash + salt).
 *  - Auto skip if an active (non soft-logged-out) session exists.
 *  - "Don't have an account? Sign Up" clickable text.
 *  - Simple loading state: disables button & swaps text while authenticating.
 *
 * Prerequisites (already provided previously):
 *  - AuthManager with loginEmail(...) & offlineEmailLogin(...)
 *  - PrefsManager storing user + password hash & salt (after signup)
 *  - PasswordHashUtil for hashing (used at signup time)
 *  - DataSeeder.seedIfNeeded()
 *  - DashboardActivity & SignUpActivity
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private View btnLogin;
    private View btnBack;
    private TextView tvNeedAccount;

    private final AuthManager auth = AuthManager.get();

    private CharSequence originalBtnText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // make sure your XML filename is activity_login.xml

        bindViews();
        initNeedAccountLink();
        initListeners();

        // Auto-skip if already fully active (not soft logged out)
        if (auth.isActiveSession()) {
            goDashboard();
        } else {
            // If provider is social and soft-logged-out but Firebase user still cached, you can optionally auto-resume
            tryAutoResumeSocial();
        }
    }

    private void bindViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnBack = findViewById(R.id.btnBack);
        tvNeedAccount = findViewById(R.id.tvNeedAccount);
        originalBtnText = (btnLogin instanceof TextView) ? ((TextView) btnLogin).getText() : "Login";
    }

    private void initNeedAccountLink() {
        String base = "Don't have an account? Sign Up";
        SpannableString ss = new SpannableString(base);
        int start = base.indexOf("Sign Up");
        int end = start + "Sign Up".length();
        ss.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
                finish();
            }
        }, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvNeedAccount.setText(ss);
        tvNeedAccount.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void initListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN)) {
                attemptLogin();
                return true;
            }
            return false;
        });
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String pw = etPassword.getText().toString();

        if (email.isEmpty() || pw.isEmpty()) {
            toast("Please enter email & password");
            return;
        }

        // Online path
        if (isOnline()) {
            setLoading(true);
            auth.loginEmail(email, pw, new AuthManager.Callback() {
                @Override
                public void onSuccess(User u) {
                    // If user just logged in online first time & no local hash yet,
                    // create hash now for future offline usage.
                    if (PrefsManager.get().getPasswordHash() == null) {
                        PrefsManager.get().saveLocalPassword(pw.toCharArray());
                        PrefsManager.get().saveProvider(AuthProvider.EMAIL.name());
                    }

                    setLoading(false);
                    goDashboard();
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    toast(message);
                }
            });
        } else {
            // Offline path (no network)
            auth.offlineEmailLogin(email, pw, new AuthManager.Callback() {
                @Override
                public void onSuccess(User u) {

                    goDashboard();
                }

                @Override
                public void onError(String message) {
                    toast(message);
                }
            });
        }
    }

    /**
     * If the provider was GOOGLE or FACEBOOK and user only did a soft logout,
     * they can resume without re-entering credentials (even offline)
     * because Firebase cached user is still present. (Optional behavior)
     */
    private void tryAutoResumeSocial() {
        String provider = PrefsManager.get().getProvider();
        boolean soft = PrefsManager.get().isSoftLoggedOut();
        if (soft && provider != null &&
                (AuthProvider.GOOGLE.name().equals(provider))
                && auth.hasFirebaseUser()) {
            // Clear soft logout and continue
            PrefsManager.get().setSoftLoggedOut(false);
            toast("Resuming session...");
            goDashboard();
        }
    }

    private void goDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        if (btnLogin instanceof TextView) {
            ((TextView) btnLogin).setText(loading ? "Logging in..." : originalBtnText);
        }
    }

    private boolean isOnline() {
        try {
            android.net.ConnectivityManager cm =
                    (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            android.net.Network net = cm.getActiveNetwork();
            return net != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}