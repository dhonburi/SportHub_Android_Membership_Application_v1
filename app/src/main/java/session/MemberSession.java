package com.example.sporthubandroidmembershipapplicationv1.session;

import android.content.Context;
import android.content.SharedPreferences;

public class MemberSession {

    private static final String PREFERENCES_NAME =
            "sporthub_member_session";

    private static final String KEY_MEMBER_ID =
            "member_id";

    private static final String KEY_MEMBER_NUMBER =
            "member_number";

    private static final String KEY_IS_ADMIN =
            "is_admin";

    // Kept in process memory, not written to SharedPreferences.
    private static volatile String staffAccessToken;

    private final SharedPreferences preferences;

    public MemberSession(Context context) {
        preferences = context.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
    }

    public void save(
            int memberId,
            String memberNumber,
            boolean isAdmin
    ) {
        save(memberId, memberNumber, isAdmin, null);
    }

    public void save(
            int memberId,
            String memberNumber,
            boolean isAdmin,
            String newStaffAccessToken
    ) {
        preferences
                .edit()
                .putInt(KEY_MEMBER_ID, memberId)
                .putString(KEY_MEMBER_NUMBER, memberNumber)
                .putBoolean(KEY_IS_ADMIN, isAdmin)
                .apply();

        staffAccessToken =
                isAdmin
                        && newStaffAccessToken != null
                        && !newStaffAccessToken.trim().isEmpty()
                        ? newStaffAccessToken
                        : null;
    }

    public int getMemberId() {
        return preferences.getInt(
                KEY_MEMBER_ID,
                -1
        );
    }

    public String getMemberNumber() {
        return preferences.getString(
                KEY_MEMBER_NUMBER,
                null
        );
    }

    public boolean isAdmin() {
        return preferences.getBoolean(
                KEY_IS_ADMIN,
                false
        );
    }

    public String getStaffAccessToken() {
        return isAdmin() ? staffAccessToken : null;
    }

    public void clear() {
        staffAccessToken = null;

        preferences
                .edit()
                .clear()
                .apply();
    }
}