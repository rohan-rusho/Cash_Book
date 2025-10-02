package com.moneytrackultra.cashbook;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Arrays;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    public static final String ACTION_DOMAIN_DATA_CLEARED =
            "com.moneytrackultra.cashbook.ACTION_DOMAIN_DATA_CLEARED";

    private ImageButton btnBack;
    private TextView tvTitle;
    private ImageView ivAvatar;
    private ImageView ivEditName;
    private TextView tvUserName;

    private View rowEditProfile;
    private View rowEditCurrency;
    private View rowBuyingLimit;
    private View rowPrivacyPolicy;

    private TextView lblEditProfile, subEditProfile;
    private TextView lblEditCurrency, subEditCurrency;
    private TextView lblBuyingLimit, subBuyingLimit;
    private TextView lblPrivacy, subPrivacy;

    private View btnClearAllData;

    private User currentUser;

    private final List<String> supportedCurrencies = Arrays.asList(
            "BDT", "USD", "INR", "EUR", "GBP", "JPY", "IDR"
    );

    private static final String PRIVACY_URL = "https://example.com/privacy-policy";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        currentUser = PrefsManager.get().getUser();
        bindViews();
        setupBackHandler();
        configureRows();
        refreshHeader();
        refreshRows();
    }

    private void bindViews() {
        btnBack          = findViewById(R.id.btnBack);
        tvTitle          = findViewById(R.id.tvTitle);
        ivAvatar         = findViewById(R.id.ivAvatar);
        ivEditName       = findViewById(R.id.ivEditName);
        tvUserName       = findViewById(R.id.tvUserName);
        rowEditProfile   = findViewById(R.id.rowEditProfile);
        rowEditCurrency  = findViewById(R.id.rowEditCurrency);
        rowBuyingLimit   = findViewById(R.id.rowBuyingLimit);
        rowPrivacyPolicy = findViewById(R.id.rowPrivacyPolicy);
        lblEditProfile   = rowEditProfile.findViewById(R.id.tvRowLabel);
        subEditProfile   = rowEditProfile.findViewById(R.id.tvRowSubtitle);
        lblEditCurrency  = rowEditCurrency.findViewById(R.id.tvRowLabel);
        subEditCurrency  = rowEditCurrency.findViewById(R.id.tvRowSubtitle);
        lblBuyingLimit   = rowBuyingLimit.findViewById(R.id.tvRowLabel);
        subBuyingLimit   = rowBuyingLimit.findViewById(R.id.tvRowSubtitle);
        lblPrivacy       = rowPrivacyPolicy.findViewById(R.id.tvRowLabel);
        subPrivacy       = rowPrivacyPolicy.findViewById(R.id.tvRowSubtitle);
        btnClearAllData  = findViewById(R.id.btnClearData);

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        ivEditName.setOnClickListener(v -> showEditNameDialog());
        tvUserName.setOnClickListener(v -> showEditNameDialog());
        ivAvatar.setOnClickListener(v -> Toast.makeText(this, "Avatar editing not implemented", Toast.LENGTH_SHORT).show());
        btnClearAllData.setOnClickListener(v -> confirmClearData());
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private void configureRows() {
        lblEditProfile.setText("Edit Profile");
        subEditProfile.setText(currentUser != null ? currentUser.email : "Unknown email");
        rowEditProfile.setOnClickListener(v -> showEditNameDialog());

        lblEditCurrency.setText("Change Currency");
        rowEditCurrency.setOnClickListener(v -> showCurrencyDialog());

        lblBuyingLimit.setText("Buying Limit");
        rowBuyingLimit.setOnClickListener(v -> openBuyingLimit());

        lblPrivacy.setText("Privacy Policy");
        rowPrivacyPolicy.setOnClickListener(v -> openPrivacyPolicy());
    }

    private void refreshHeader() {
        currentUser = PrefsManager.get().getUser();
        if (currentUser != null && !TextUtils.isEmpty(currentUser.displayName)) {
            tvUserName.setText(currentUser.displayName);
        } else {
            tvUserName.setText("User");
        }
    }

    private void refreshRows() {
        String c = PrefsManager.get().getCurrency();
        if (TextUtils.isEmpty(c)) c = "Not set";
        subEditCurrency.setText(c);

        int limitCount = PrefsManager.get().getBuyingLimits().size();
        subBuyingLimit.setText(limitCount == 0 ? "No limits set" : limitCount + " limit(s)");
        subPrivacy.setText("View details");
    }

    private void showEditNameDialog() {
        final EditText input = new EditText(this);
        input.setSingleLine();
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
        input.setHint("Your name");
        if (currentUser != null && !TextUtils.isEmpty(currentUser.displayName)) {
            input.setText(currentUser.displayName);
            input.setSelection(input.getText().length());
        }

        new AlertDialog.Builder(this)
                .setTitle("Edit Name")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.length() < 2) {
                        Toast.makeText(this, "Name too short", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveDisplayName(newName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCurrencyDialog() {
        final String[] items = supportedCurrencies.toArray(new String[0]);
        int preselect = -1;
        String current = PrefsManager.get().getCurrency();
        if (current != null) {
            for (int i = 0; i < items.length; i++) {
                if (items[i].equalsIgnoreCase(current)) {
                    preselect = i;
                    break;
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Choose Currency")
                .setSingleChoiceItems(items, preselect, null)
                .setPositiveButton("OK", (d, w) -> {
                    AlertDialog dialog = (AlertDialog) d;
                    int checked = dialog.getListView().getCheckedItemPosition();
                    if (checked >= 0) {
                        String chosen = items[checked];
                        PrefsManager.get().saveCurrency(chosen);
                        refreshRows();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveDisplayName(String newName) {
        if (currentUser == null) {
            Toast.makeText(this, "No user cached", Toast.LENGTH_SHORT).show();
            return;
        }
        currentUser.displayName = newName;
        PrefsManager.get().saveUser(currentUser);
        refreshHeader();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            FirebaseAuth.getInstance().getCurrentUser()
                    .updateProfile(new UserProfileChangeRequest.Builder()
                            .setDisplayName(newName)
                            .build())
                    .addOnFailureListener(e -> { /* ignore */ });
        }
    }

    private void openBuyingLimit() {
        try {
            startActivity(new Intent(this, BuyingLimitActivity.class));
        } catch (Exception e) {
            Toast.makeText(this, "BuyingLimitActivity not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPrivacyPolicy() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL));
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmClearData() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("This will remove all local transactions, wallets, weekly stats, buying limits, and currency preference. Your account will remain. Continue?")
                .setPositiveButton("Clear", (d, w) -> {
                    PrefsManager.get().clearAllDomainDataPreserveUser();
                    sendBroadcast(new Intent(ACTION_DOMAIN_DATA_CLEARED));
                    Toast.makeText(this, "Data cleared", Toast.LENGTH_SHORT).show();
                    refreshRows();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}