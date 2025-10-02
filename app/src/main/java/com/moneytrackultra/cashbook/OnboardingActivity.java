package com.moneytrackultra.cashbook;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class OnboardingActivity extends AppCompatActivity {

    private View btnSignUp;
    private View btnLogin;
    private View btnGoogle;
    private View progressOverlay;

    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;

    // Launcher for IntentSender (One Tap)
    private ActivityResultLauncher<IntentSenderRequest> googleLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // Fast path (already authenticated)
        if (FirebaseAuth.getInstance().getCurrentUser() != null
                && PrefsManager.get().getUser() != null
                && !PrefsManager.get().isSoftLoggedOut()) {
            goDashboard();
            return;
        }

        bindViews();
        initLauncher();
        initClicks();
        initGoogleOneTap();
    }

    private void bindViews() {
        btnSignUp = findViewById(R.id.btnSignUp);
        btnLogin  = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
    }

    private void initLauncher() {
        googleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getData() == null) {
                        showToast("Cancelled");
                        return;
                    }
                    try {
                        SignInCredential credential =
                                oneTapClient.getSignInCredentialFromIntent(result.getData());
                        String idToken = credential.getGoogleIdToken();
                        String email = credential.getId();
                        if (idToken == null || email == null) {
                            showToast("Google sign-in failed");
                            return;
                        }
                        handleGoogleFirebaseAuth(idToken,
                                email,
                                credential.getDisplayName());
                    } catch (ApiException e) {
                        showToast("Google error: " + e.getStatusCode());
                    }
                }
        );
    }

    private void initClicks() {
        btnSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));

        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        btnGoogle.setOnClickListener(v -> beginGoogleSignIn());
    }

    private void initGoogleOneTap() {
        oneTapClient = Identity.getSignInClient(this);

        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(false)
                                .build()
                )
                .setAutoSelectEnabled(false)
                .build();
    }

    private void beginGoogleSignIn() {
        setLoading(true);
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this::onBeginSuccess)
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showToast("Google not available: " + e.getMessage());
                });
    }

    private void onBeginSuccess(BeginSignInResult result) {
        setLoading(false);
        if (result == null || result.getPendingIntent() == null) {
            showToast("No intent");
            return;
        }
        IntentSenderRequest request =
                new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender())
                        .build();
        googleLauncher.launch(request);
    }

    private void handleGoogleFirebaseAuth(String idToken, String email, String displayName) {
        setLoading(true);
        FirebaseAuth.getInstance()
                .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (!task.isSuccessful()) {
                        showToast("Firebase auth failed");
                        return;
                    }
                    User existing = PrefsManager.get().getUser();
                    if (existing == null || !email.equalsIgnoreCase(existing.email)) {
                        String name = (displayName != null && !displayName.isEmpty())
                                ? displayName
                                : email.substring(0, email.indexOf("@"));
                        User u = new User(
                                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                                email,
                                name,
                                null,
                                System.currentTimeMillis()
                        );
                        PrefsManager.get().saveUser(u);
                    }

                    goDashboard();
                });
    }

    private void goDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void setLoading(boolean loading) {
        btnSignUp.setEnabled(!loading);
        btnLogin.setEnabled(!loading);
        btnGoogle.setEnabled(!loading);
        if (loading) {
            if (progressOverlay == null) {
                progressOverlay = new View(this);
                progressOverlay.setBackgroundColor(0x33000000);
                addContentView(progressOverlay,
                        new android.widget.FrameLayout.LayoutParams(
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

                android.widget.ProgressBar pb = new android.widget.ProgressBar(this);
                pb.setIndeterminate(true);
                android.widget.FrameLayout.LayoutParams lp =
                        new android.widget.FrameLayout.LayoutParams(120,120);
                lp.gravity = android.view.Gravity.CENTER;
                addContentView(pb, lp);
            } else {
                progressOverlay.setVisibility(View.VISIBLE);
            }
        } else if (progressOverlay != null) {
            progressOverlay.setVisibility(View.GONE);
        }
    }
}