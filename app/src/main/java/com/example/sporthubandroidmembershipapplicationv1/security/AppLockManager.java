package com.example.sporthubandroidmembershipapplicationv1.security;

import android.app.Activity;
import android.app.Application;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import androidx.core.content.ContextCompat;
import com.example.sporthubandroidmembershipapplicationv1.HomeActivity;
import com.example.sporthubandroidmembershipapplicationv1.LoginActivity;
import com.example.sporthubandroidmembershipapplicationv1.PinActivity;
import com.example.sporthubandroidmembershipapplicationv1.RegisterActivity;
import com.example.sporthubandroidmembershipapplicationv1.session.MemberSession;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class AppLockManager implements Application.ActivityLifecycleCallbacks {
    public static final String EXTRA_LOGIN_REASON = "US15_LOGIN_REASON";
    public static final String EXTRA_RETURN_TO_EXISTING = "US15_RETURN_TO_EXISTING";
    private static AppLockManager instance;
    private final Application app;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<Activity> started = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Activity> activities = Collections.newSetFromMap(new IdentityHashMap<>());
    private WeakReference<Activity> current = new WeakReference<>(null);
    private volatile boolean unlocked;
    private boolean routing;
    private long backgroundAt = -1;
    private final Runnable backgroundLock = () -> unlocked = false;
    private final Runnable expiry = () -> {
        unlocked = false;
        Activity activity = current.get();
        if (activity != null && !started.isEmpty()) forceLogin(activity, "Your seven-day login has expired. Please sign in again.");
    };
    private AppLockManager(Application app) {
        this.app = app; app.registerActivityLifecycleCallbacks(this);
        ContextCompat.registerReceiver(app, new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                unlocked = false;
                for (Activity activity : activities) if (protectedActivity(activity)) cover(activity);
            }
        }, new IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED);
    }
    public static void install(Application app) { instance = new AppLockManager(app); }
    public static AppLockManager get() {
        if (instance == null) throw new IllegalStateException("SportHubApplication is not configured.");
        return instance;
    }
    public boolean isUnlocked() { return unlocked; }
    private boolean protectedActivity(Activity a) {
        return !(a instanceof LoginActivity) && !(a instanceof RegisterActivity) && !(a instanceof PinActivity);
    }
    private void protectRecents(Activity a) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Hide task previews without hiding the live activity during an
            // ordinary Settings/Profile transition or a predictive Back gesture.
            a.setRecentsScreenshotEnabled(false);
        } else {
            // Older Android versions have no separate task-preview control.
            // Keep their protected windows secure even while unlocked.
            a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
    private void cover(Activity a) {
        if ((a.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_SECURE) == 0)
            a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        a.getWindow().getDecorView().setVisibility(View.INVISIBLE);
    }
    private void reveal(Activity a) {
        a.getWindow().getDecorView().setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && (a.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_SECURE) != 0)
            a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
    public void authenticated() {
        unlocked = true; routing = false; backgroundAt = -1;
        handler.removeCallbacks(backgroundLock); scheduleExpiry();
    }
    private void scheduleExpiry() {
        handler.removeCallbacks(expiry);
        MemberSession session = new MemberSession(app);
        if (session.hasLogin()) handler.postDelayed(expiry, Math.max(0,
                Math.min(session.getExpiresAt() - session.estimatedServerNow(), SessionPolicy.LOGIN_LIFETIME_MS)));
    }
    public void forceLogin(Activity activity, String reason) {
        if (activity == null || activity.isFinishing() || routing) return;
        routing = true; unlocked = false;
        if (protectedActivity(activity)) cover(activity);
        new MemberSession(app).invalidate(); handler.removeCallbacks(expiry);
        activity.startActivity(new Intent(activity, LoginActivity.class).putExtra(EXTRA_LOGIN_REASON, reason)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
    public void authenticationRejected(String requestToken) {
        handler.post(() -> {
            MemberSession session = new MemberSession(app);
            if (!requestToken.equals(session.getMemberAccessToken())) return;
            Activity activity = current.get();
            if (activity != null && !started.isEmpty()) {
                routing = false; forceLogin(activity, "Your login is no longer valid. Please sign in again.");
            } else { session.invalidate(); unlocked = false; }
        });
    }
    private void guard(Activity activity) {
        if (!protectedActivity(activity) || activity.isFinishing()) return;
        MemberSession session = new MemberSession(app);
        KeyguardManager keyguard = (KeyguardManager) app.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguard != null && keyguard.isKeyguardLocked()) { unlocked = false; cover(activity); return; }
        if (session.isExpired()) { forceLogin(activity, "Please sign in to continue. Remembered login lasts seven days."); return; }
        if (!unlocked || !session.hasPin()) {
            cover(activity);
            if (!routing) {
                routing = true;
                activity.startActivity(new Intent(activity, PinActivity.class).putExtra(EXTRA_RETURN_TO_EXISTING, true));
            }
        } else { reveal(activity); scheduleExpiry(); }
    }
    public Intent homeIntent(Context context) {
        MemberSession session = new MemberSession(app);
        return new Intent(context, HomeActivity.class).putExtra("Username", session.getDisplayName())
                .putExtra("MemberId", session.getMemberId()).putExtra("MemberNumber", session.getMemberNumber())
                .putExtra("OPEN_FRAGMENT", "QR").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }
    @Override public void onActivityCreated(Activity a, Bundle state) {
        activities.add(a);
        if (protectedActivity(a)) {
            protectRecents(a);
            if (!unlocked || new MemberSession(app).isExpired()) cover(a);
        }
    }
    @Override public void onActivityStarted(Activity a) {
        if (started.isEmpty() && backgroundAt >= 0) {
            if (SessionPolicy.lockOnReturn(SystemClock.elapsedRealtime() - backgroundAt, false)) unlocked = false;
            backgroundAt = -1;
        }
        handler.removeCallbacks(backgroundLock); started.add(a); guard(a);
    }
    @Override public void onActivityResumed(Activity a) {
        current = new WeakReference<>(a);
        if (a instanceof LoginActivity || a instanceof PinActivity) routing = false;
        guard(a);
    }
    @Override public void onActivityPaused(Activity a) {
        // onPause also runs when another SportHub activity opens or finishes.
        // Keep an authenticated page drawable for that navigation animation.
        // A real lock must still hide it immediately.
        if (protectedActivity(a) && (!unlocked || new MemberSession(app).isExpired())) cover(a);
    }
    @Override public void onActivityStopped(Activity a) {
        started.remove(a);
        if (started.isEmpty() && !a.isChangingConfigurations()) {
            // Only an empty started set means the whole app left the screen.
            // Cover its protected pages then, not every internal page switch.
            for (Activity activity : activities) if (protectedActivity(activity)) cover(activity);
            backgroundAt = SystemClock.elapsedRealtime(); handler.removeCallbacks(expiry);
            handler.postDelayed(backgroundLock, SessionPolicy.BACKGROUND_GRACE_MS);
        }
    }
    @Override public void onActivityDestroyed(Activity a) { started.remove(a); activities.remove(a); }
    @Override public void onActivitySaveInstanceState(Activity a, Bundle state) { }
}
