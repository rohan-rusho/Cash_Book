package com.moneytrackultra.cashbook;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * AuthManager with soft/hard logout + offline password verify.
 * Requires PrefsManager methods now provided.
 */
public class AuthManager {

    private static AuthManager INSTANCE;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public interface Callback {
        void onSuccess(User u);
        void onError(String message);
    }

    private AuthManager(){}

    public static AuthManager get(){
        if(INSTANCE == null) INSTANCE = new AuthManager();
        return INSTANCE;
    }

    public boolean hasFirebaseUser(){
        return auth.getCurrentUser() != null;
    }

    public boolean isActiveSession(){
        return hasFirebaseUser()
                && PrefsManager.get().getUser() != null
                && !PrefsManager.get().isSoftLoggedOut();
    }

    /* ---------------- EMAIL REGISTER ---------------- */
    public void registerEmail(String email, String password, String displayName, Callback cb){
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(t -> {
                    if(!t.isSuccessful()){
                        cb.onError(t.getException()!=null? t.getException().getMessage():"Registration failed");
                        return;
                    }
                    FirebaseUser fu = auth.getCurrentUser();
                    if(fu == null){
                        cb.onError("No Firebase user");
                        return;
                    }
                    User u = new User(fu.getUid(), email, displayName, fu.getPhotoUrl()!=null? fu.getPhotoUrl().toString(): null, System.currentTimeMillis());
                    PrefsManager.get().saveUser(u);
                    PrefsManager.get().saveProvider(AuthProvider.EMAIL.name());
                    PrefsManager.get().setSoftLoggedOut(false);
                    // Store offline password hash (optional)
                    PrefsManager.get().saveLocalPassword(password.toCharArray());
                    cb.onSuccess(u);
                });
    }

    /* ---------------- EMAIL LOGIN (ONLINE) ---------------- */
    public void loginEmail(String email, String password, Callback cb){
        auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(t -> {
                    if(!t.isSuccessful()){
                        cb.onError(t.getException()!=null? t.getException().getMessage():"Login failed");
                        return;
                    }
                    FirebaseUser fu = auth.getCurrentUser();
                    if(fu == null){
                        cb.onError("No Firebase user after login");
                        return;
                    }
                    User cached = PrefsManager.get().getUser();
                    if(cached == null || !cached.email.equalsIgnoreCase(email)){
                        User u = new User(fu.getUid(), email,
                                fu.getDisplayName()!=null? fu.getDisplayName(): email.substring(0,email.indexOf("@")),
                                fu.getPhotoUrl()!=null? fu.getPhotoUrl().toString(): null,
                                System.currentTimeMillis());
                        PrefsManager.get().saveUser(u);
                    }
                    PrefsManager.get().saveProvider(AuthProvider.EMAIL.name());
                    PrefsManager.get().setSoftLoggedOut(false);
                    PrefsManager.get().saveLocalPassword(password.toCharArray()); // refresh local hash
                    cb.onSuccess(PrefsManager.get().getUser());
                });
    }

    /* ---------------- OFFLINE EMAIL LOGIN ---------------- */
    public void offlineEmailLogin(String email, String plainPassword, Callback cb){
        User u = PrefsManager.get().getUser();
        if(u == null){
            cb.onError("No cached user");
            return;
        }
        if(!u.email.equalsIgnoreCase(email)){
            cb.onError("Email mismatch");
            return;
        }
        if(!AuthProvider.EMAIL.name().equals(PrefsManager.get().getProvider())){
            cb.onError("Not cached as EMAIL provider");
            return;
        }
        String salt = PrefsManager.get().getPasswordSalt();
        String hash = PrefsManager.get().getPasswordHash();
        if(salt==null || hash==null){
            cb.onError("No offline password stored");
            return;
        }
        boolean ok = PasswordHashUtil.verify(plainPassword.toCharArray(), salt, hash);
        if(ok){
            PrefsManager.get().setSoftLoggedOut(false);
            cb.onSuccess(u);
        } else {
            cb.onError("Wrong password (offline)");
        }
    }

    /* ---------------- LOGOUTS ---------------- */
    public void softLogout(){
        PrefsManager.get().setSoftLoggedOut(true);
    }

    public void hardLogout(){
        auth.signOut();
        PrefsManager.get().setSoftLoggedOut(true);
        PrefsManager.get().saveUser(null);
        PrefsManager.get().clearLocalPassword();
        PrefsManager.get().saveProvider(null);
    }
}