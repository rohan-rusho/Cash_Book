package com.moneytrackultra.cashbook;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * EditProfileActivity
 * Layout expected: activity_edit_profile.xml (rename your XML to this or change setContentView).
 *
 * Fields:
 *  - etName, etEmail (editable)
 *  - etPassword (disabled, just placeholder)
 *  - btnSave
 *  - tvChangePassword (launches password change flow dialog)
 *
 * Local-only changes when offline will be persisted and flagged for later sync.
 */
public class EditProfileActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etName;
    private EditText etEmail;
    private EditText etPassword; // disabled / display only
    private Button btnSave;
    private TextView tvChangePassword;

    private User cachedUser;
    private String originalName;
    private String originalEmail;

    // Firestore (optional; remove if you don't use Firestore)
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_profile); // match your layout filename

        firestore = FirebaseFirestore.getInstance();

        bindViews();
        setupBackPress();
        loadUser();
        configureUI();
        setupListeners();
    }

    private void bindViews() {
        btnBack          = findViewById(R.id.btnBack);
        etName           = findViewById(R.id.etName);
        etEmail          = findViewById(R.id.etEmail);
        etPassword       = findViewById(R.id.etPassword);
        btnSave          = findViewById(R.id.btnSave);
        tvChangePassword = findViewById(R.id.tvChangePassword);
    }

    private void setupBackPress() {
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private void loadUser() {
        cachedUser = PrefsManager.get().getUser();
        if (cachedUser == null) {
            toast("No user cached");
            finish();
            return;
        }
        originalName  = cachedUser.displayName != null ? cachedUser.displayName : "";
        originalEmail = cachedUser.email != null ? cachedUser.email : "";

        etName.setText(originalName);
        etEmail.setText(originalEmail);
        etPassword.setText("************"); // masked placeholder
    }

    private void configureUI() {
        etName.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(40) });
        etEmail.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(80) });

        etEmail.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        etName.setImeOptions(EditorInfo.IME_ACTION_NEXT);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> attemptSave());
        tvChangePassword.setOnClickListener(v -> showPasswordChangeDialog());
    }

    private void attemptSave() {
        if (cachedUser == null) {
            toast("User not loaded");
            return;
        }
        String newName  = etName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();

        if (!validate(newName, newEmail)) return;

        boolean nameChanged  = !newName.equals(originalName);
        boolean emailChanged = !newEmail.equalsIgnoreCase(originalEmail);

        if (!nameChanged && !emailChanged) {
            toast("No changes");
            return;
        }

        if (emailChanged) {
            // Need re-auth (Firebase requirement)
            if (!isOnline()) {
                // Offline path: store pending update
                storePendingUpdate(newName, newEmail);
                toast("Offline: changes saved locally and will sync later");
                finish();
                return;
            }
            promptReAuthThenUpdate(newName, newEmail);
        } else {
            // Only name changed
            if (!isOnline()) {
                applyLocalUpdate(newName, null); // local only
                markPendingProfileSync();
                toast("Offline: name updated locally");
                finish();
            } else {
                updateNameOnline(newName);
            }
        }
    }

    private boolean validate(String name, String email) {
        if (name.length() < 2) {
            toast("Name too short");
            return false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Invalid email");
            return false;
        }
        return true;
    }

    private void promptReAuthThenUpdate(String newName, String newEmail) {
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Current password");
        new AlertDialog.Builder(this)
                .setTitle("Confirm Password")
                .setMessage("To change your email you must re-enter your current password.")
                .setView(input)
                .setPositiveButton("Continue", (d, w) -> {
                    String pwd = input.getText().toString();
                    if (pwd.isEmpty()) {
                        toast("Password required");
                        return;
                    }
                    reAuthAndProceed(newName, newEmail, pwd);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reAuthAndProceed(String newName, String newEmail, String passwordPlain) {
        var firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            toast("No Firebase session");
            // fallback local
            applyLocalUpdate(newName, newEmail);
            markPendingProfileSync();
            finish();
            return;
        }

        String currentEmail = firebaseUser.getEmail();
        if (currentEmail == null) currentEmail = originalEmail;

        AuthCredential credential = EmailAuthProvider.getCredential(currentEmail, passwordPlain);
        setLoading(true);
        firebaseUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    // Re-auth success: update email then name
                    firebaseUser.updateEmail(newEmail)
                            .addOnSuccessListener(v -> {
                                updateDisplayNameAndFirestore(firebaseUser, newName, newEmail, true);
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                toast("Email update failed: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Re-auth failed: " + e.getMessage());
                });
    }

    private void updateNameOnline(String newName) {
        var firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            // fallback local
            applyLocalUpdate(newName, null);
            markPendingProfileSync();
            toast("Offline session: name cached");
            finish();
            return;
        }
        setLoading(true);
        firebaseUser.updateProfile(new UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build())
                .addOnSuccessListener(v -> {
                    updateFirestoreUser(firebaseUser.getUid(), newName, cachedUser.email);
                    applyLocalUpdate(newName, null);
                    setLoading(false);
                    toast("Name updated");
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Name update failed: " + e.getMessage());
                });
    }

    private void updateDisplayNameAndFirestore(com.google.firebase.auth.FirebaseUser firebaseUser,
                                               String newName,
                                               String newEmail,
                                               boolean finishAfter) {
        firebaseUser.updateProfile(new UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build())
                .addOnSuccessListener(v -> {
                    updateFirestoreUser(firebaseUser.getUid(), newName, newEmail);
                    applyLocalUpdate(newName, newEmail);
                    setLoading(false);
                    toast("Profile updated");
                    if (finishAfter) finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Display name failed (email already updated): " + e.getMessage());
                });
    }

    // Firestore optional
    private void updateFirestoreUser(String uid, String name, String email) {
        try {
            if (firestore == null || uid == null) return;
            Map<String,Object> map = new HashMap<>();
            map.put("displayName", name);
            map.put("email", email);
            map.put("updatedAt", System.currentTimeMillis());
            firestore.collection("users")
                    .document(uid)
                    .set(map, com.google.firebase.firestore.SetOptions.merge());
        } catch (Exception ignored) { }
    }

    private void applyLocalUpdate(String newName, String newEmail) {
        if (cachedUser == null) return;
        if (newName != null) cachedUser.displayName = newName;
        if (newEmail != null) cachedUser.email = newEmail;
        PrefsManager.get().saveUser(cachedUser);
        originalName = cachedUser.displayName;
        if (newEmail != null) originalEmail = cachedUser.email;
    }

    private void storePendingUpdate(String newName, String newEmail) {
        applyLocalUpdate(newName, newEmail);
        markPendingProfileSync();
    }

    private void markPendingProfileSync() {
        PrefsManager.get().setPendingProfileSync(true);
    }

    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network net = cm.getActiveNetwork();
            return net != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void showPasswordChangeDialog() {
        // Simple dialog offering to send password reset email
        if (cachedUser == null || TextUtils.isEmpty(cachedUser.email)) {
            toast("No email on record");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setMessage("We will send a password reset link to:\n" + cachedUser.email)
                .setPositiveButton("Send", (d,w) -> sendResetEmail(cachedUser.email))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendResetEmail(String email) {
        setLoading(true);
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    toast("Reset email sent");
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Failed: " + e.getMessage());
                });
    }

    private void setLoading(boolean loading) {
        btnSave.setEnabled(!loading);
        btnSave.setAlpha(loading ? 0.6f : 1f);
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Attempt pending sync if we came back online
        if (isOnline() && PrefsManager.get().isPendingProfileSync()) {
            UserSyncManager.syncProfileIfPending();
        }
    }
}