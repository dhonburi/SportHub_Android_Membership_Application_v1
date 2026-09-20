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
        preferences
                .edit()
                .putInt(KEY_MEMBER_ID, memberId)
                .putString(KEY_MEMBER_NUMBER, memberNumber)
                .putBoolean(KEY_IS_ADMIN, isAdmin)
                .apply();
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

    public void clear() {
        preferences
                .edit()
                .clear()
                .apply();
    }
}