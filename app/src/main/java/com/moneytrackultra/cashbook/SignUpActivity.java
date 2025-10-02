package com.moneytrackultra.cashbook;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    private View btnBack;
    private View btnSignUp;
    private Spinner spCurrency;
    private android.widget.EditText etName, etEmail, etPassword;
    private android.widget.TextView tvAlready;

    private final AuthManager auth = AuthManager.get();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        bind();
        setupCurrency();
        setupAlreadyLink();

        btnSignUp.setOnClickListener(v -> doSignup());
        btnBack.setOnClickListener(v -> finish());
    }

    private void bind(){
        btnBack     = findViewById(R.id.btnBack);
        btnSignUp   = findViewById(R.id.btnSignUp);
        spCurrency  = findViewById(R.id.spCurrency);
        etName      = findViewById(R.id.etName);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        tvAlready   = findViewById(R.id.tvAlready);
    }

    private void setupCurrency(){
        ArrayAdapter<CharSequence> ad = ArrayAdapter.createFromResource(
                this,
                R.array.currencies_with_prompt,
                android.R.layout.simple_spinner_item
        );
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCurrency.setAdapter(ad);
        String last = PrefsManager.get().getCurrency();
        if(last != null){
            for(int i=0;i<ad.getCount();i++){
                if(ad.getItem(i).toString().equalsIgnoreCase(last)){
                    spCurrency.setSelection(i);
                    break;
                }
            }
        }
    }

    private void setupAlreadyLink(){
        String base = "Already have an account? Login";
        SpannableString ss = new SpannableString(base);
        int start = base.indexOf("Login");
        int end = start + "Login".length();
        ss.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                finish();
            }
        }, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvAlready.setText(ss);
        tvAlready.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void doSignup(){
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pw = etPassword.getText().toString();

        if(name.isEmpty() || email.isEmpty() || pw.isEmpty()){
            toast("Fill all fields");
            return;
        }
        if(pw.length() < 6){
            toast("Password must be ≥ 6 chars");
            return;
        }
        setLoading(true);

        auth.registerEmail(email, pw, name, new AuthManager.Callback() {
            @Override
            public void onSuccess(User u) {
                // AuthManager already saved local password hash+salt.
                // Ensure provider + currency recorded.
                PrefsManager.get().saveProvider(AuthProvider.EMAIL.name());

                String currency = spCurrency.getSelectedItem() != null
                        ? spCurrency.getSelectedItem().toString()
                        : null;
                if(currency != null && !currency.equalsIgnoreCase("Select Currency")) {
                    PrefsManager.get().saveCurrency(currency);
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
    }

    private void goDashboard(){
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private void setLoading(boolean l){
        btnSignUp.setEnabled(!l);
    }
    private void toast(String m){ Toast.makeText(this,m,Toast.LENGTH_SHORT).show();}
}