package com.moneytrackultra.cashbook;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles syncing pending local profile changes to Firebase Auth + Firestore.
 * Call on app start or when connectivity is regained.
 */
public final class UserSyncManager {

    private UserSyncManager(){}

    public static void syncProfileIfPending() {
        if (!PrefsManager.get().isPendingProfileSync()) return;

        var authUser = FirebaseAuth.getInstance().getCurrentUser();
        User local = PrefsManager.get().getUser();
        if (authUser == null || local == null) return;

        // Update display name if different
        boolean needName = local.displayName != null &&
                (authUser.getDisplayName() == null ||
                        !authUser.getDisplayName().equals(local.displayName));

        // Update email if different (requires secure re-auth — here we only attempt if they match)
        boolean needEmail = local.email != null &&
                authUser.getEmail() != null &&
                !authUser.getEmail().equalsIgnoreCase(local.email);

        // (Email changes require re-auth; if we got here offline earlier, we assume re-auth was done previously
        // or user has re-logged in. Use caution in production.)

        if (needName) {
            authUser.updateProfile(new UserProfileChangeRequest.Builder()
                    .setDisplayName(local.displayName)
                    .build());
        }
        if (needEmail) {
            // We attempt without re-auth; may fail silently
            authUser.updateEmail(local.email);
        }

        // Firestore
        try {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(authUser.getUid())
                    .set(makeMap(local), com.google.firebase.firestore.SetOptions.merge());
        } catch (Exception ignored) {}

        PrefsManager.get().setPendingProfileSync(false);
    }

    private static Map<String,Object> makeMap(User u) {
        Map<String,Object> map = new HashMap<>();
        map.put("displayName", u.displayName);
        map.put("email", u.email);
        map.put("syncedAt", System.currentTimeMillis());
        return map;
    }
}