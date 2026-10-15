package com.example.sporthubandroidmembershipapplicationv1;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sporthubandroidmembershipapplicationv1.models.SessionResponse;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;
import com.example.sporthubandroidmembershipapplicationv1.security.AppLockManager;
import com.example.sporthubandroidmembershipapplicationv1.security.PinCrypto;
import com.example.sporthubandroidmembershipapplicationv1.session.MemberSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class PinActivity extends AppCompatActivity {
    private static final int PIN_LENGTH = 6;
    private static final int INK = Color.rgb(17, 17, 17);
    private static final int ERROR_COLOR = Color.rgb(179, 38, 30);

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final StringBuilder enteredPin = new StringBuilder(PIN_LENGTH);
    private final List<Button> keypadButtons = new ArrayList<>();
    private final List<View> pinDots = new ArrayList<>();
    private MemberSession session;
    private TextView title, description, error;
    private LinearLayout dotRow;
    private Button recovery, logout;
    private boolean setup, confirming, busy, resumed;
    private String firstPin;
    private int attemptEpoch;
    private Call<SessionResponse> sessionCall;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        session = new MemberSession(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        EdgeToEdge.enable(this);
        buildScreen();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { moveTaskToBack(true); }
        });
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundResource(R.drawable.synergyappbackground);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setPadding(dp(24), dp(24), dp(24), dp(24));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

        TextView brand = text("SYNERGY\nSPORTS", 28, Color.WHITE, true);
        brand.setGravity(Gravity.CENTER);
        page.addView(brand, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout cardFrame = new FrameLayout(this);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(-1, -2);
        frameParams.topMargin = dp(24);
        page.addView(cardFrame, frameParams);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24), dp(66), dp(24), dp(20));
        card.setBackground(shape(Color.WHITE, 24));
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(50);
        cardFrame.addView(card, cardParams);

        AppCompatImageView logo = new AppCompatImageView(this);
        GradientDrawable logoBackground = new GradientDrawable();
        logoBackground.setShape(GradientDrawable.OVAL);
        logoBackground.setColor(INK);
        logoBackground.setStroke(dp(2), Color.WHITE);
        logo.setBackground(logoBackground);
        logo.setImageResource(R.drawable.synergy_logo_black);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setPadding(dp(3), dp(3), dp(3), dp(3));
        logo.setClipToOutline(true);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        cardFrame.addView(logo, new FrameLayout.LayoutParams(dp(100), dp(100),
                Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        title = text("", 22, INK, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title, new LinearLayout.LayoutParams(-1, -2));
        description = text("", 14, Color.DKGRAY, false);
        description.setGravity(Gravity.CENTER);
        add(card, description, 6);

        dotRow = new LinearLayout(this);
        dotRow.setGravity(Gravity.CENTER);
        dotRow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        dotRow.setFocusable(true);
        LinearLayout.LayoutParams dotsParams = new LinearLayout.LayoutParams(-1, dp(14));
        dotsParams.topMargin = dp(16);
        card.addView(dotRow, dotsParams);
        for (int index = 0; index < PIN_LENGTH; index++) {
            View dot = new View(this);
            dot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(8), dp(8));
            dotParams.setMargins(dp(5), 0, dp(5), 0);
            dotRow.addView(dot, dotParams);
            pinDots.add(dot);
        }

        error = text("", 13, ERROR_COLOR, false);
        error.setGravity(Gravity.CENTER);
        error.setMinHeight(dp(32));
        error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        add(card, error, 6);

        addKeypadRow(card, new String[]{"1", "2", "3"}, new String[]{"", "ABC", "DEF"});
        addKeypadRow(card, new String[]{"4", "5", "6"}, new String[]{"GHI", "JKL", "MNO"});
        addKeypadRow(card, new String[]{"7", "8", "9"}, new String[]{"PQRS", "TUV", "WXYZ"});
        addKeypadRow(card, new String[]{"", "0", "⌫"}, new String[]{"", "", ""});

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(58));
        actionsParams.topMargin = dp(24);
        page.addView(actions, actionsParams);

        recovery = secondaryButton();
        LinearLayout.LayoutParams recoveryParams = new LinearLayout.LayoutParams(0, -1, 1f);
        recoveryParams.setMarginEnd(dp(7));
        actions.addView(recovery, recoveryParams);
        logout = secondaryButton();
        logout.setText("Log Out  ↗");
        LinearLayout.LayoutParams logoutParams = new LinearLayout.LayoutParams(0, -1, 1f);
        logoutParams.setMarginStart(dp(7));
        actions.addView(logout, logoutParams);

        recovery.setOnClickListener(v -> {
            if (busy) return;
            if (setup) {
                firstPin = null;
                confirming = false;
                clearEntry();
                error.setText("");
                renderHeader();
            } else {
                session.clear();
                AppLockManager.get().forceLogin(this,
                        "Sign in with your account credentials to reset your PIN.");
            }
        });
        logout.setOnClickListener(v -> {
            if (busy) return;
            session.clear();
            AppLockManager.get().forceLogin(this, "You have signed out. Please sign in again.");
        });

        setContentView(scroll);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        updateDots();
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radius));
        return background;
    }

    private void add(LinearLayout parent, View view, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(top);
        parent.addView(view, params);
    }

    private AppCompatButton baseButton() {
        AppCompatButton button = new AppCompatButton(this);
        button.setBackgroundTintList(null);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setAllCaps(false);
        button.setTextColor(INK);
        button.setGravity(Gravity.CENTER);
        return button;
    }

    private AppCompatButton secondaryButton() {
        AppCompatButton button = baseButton();
        button.setBackgroundResource(R.drawable.login_secondary_button_background);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setPadding(dp(8), 0, dp(8), 0);
        return button;
    }

    private void addKeypadRow(LinearLayout card, String[] digits, String[] letters) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(row, new LinearLayout.LayoutParams(-1, -2));
        for (int index = 0; index < digits.length; index++) {
            String digit = digits[index];
            AppCompatButton key = baseButton();
            key.setBackground(shape(Color.WHITE, 16));
            key.setStateListAnimator(null);
            key.setElevation(0);
            key.setMinHeight(dp(64));
            key.setMinimumHeight(dp(64));
            key.setPadding(0, dp(2), 0, dp(2));
            key.setIncludeFontPadding(false);
            key.setTextSize(36);
            key.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            SpannableString label = new SpannableString(digit + "\n" + (letters[index].isEmpty() ? " " : letters[index]));
            label.setSpan(new RelativeSizeSpan(0.28f), digit.length() + 1, label.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            key.setText(label);
            row.addView(key, new LinearLayout.LayoutParams(0, -2, 1f));
            if (digit.isEmpty()) {
                key.setVisibility(View.INVISIBLE);
                continue;
            }
            key.setContentDescription("⌫".equals(digit) ? "Delete last PIN digit" : digit);
            key.setOnClickListener(v -> {
                if ("⌫".equals(digit)) deleteDigit();
                else enterDigit(digit.charAt(0));
            });
            keypadButtons.add(key);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void renderHeader() {
        title.setText(setup ? confirming ? "Confirm your app PIN" : "Create your app PIN" : "Enter security PIN");
        description.setVisibility(setup ? View.VISIBLE : View.GONE);
        description.setText(confirming ? "Re-enter your six-digit PIN to confirm." : "Choose a six-digit PIN for this device.");
        recovery.setText(setup ? "Start again  ↗" : "Forgot PIN  ↗");
    }

    private void updateDots() {
        for (int index = 0; index < pinDots.size(); index++) {
            pinDots.get(index).setBackground(shape(index < enteredPin.length() ? INK : Color.rgb(217,217,217), 4));
        }
        dotRow.setContentDescription("PIN: " + enteredPin.length() + " of " + PIN_LENGTH + " digits entered.");
    }

    private void clearEntry() {
        enteredPin.setLength(0);
        updateDots();
    }

    private void enterDigit(char digit) {
        if (busy || !resumed || digit < '0' || digit > '9' || enteredPin.length() >= PIN_LENGTH) return;
        error.setText("");
        enteredPin.append(digit);
        updateDots();
        if (enteredPin.length() == PIN_LENGTH) submit();
    }

    private void deleteDigit() {
        if (busy || !resumed) return;
        error.setText("");
        if (enteredPin.length() > 0) enteredPin.deleteCharAt(enteredPin.length() - 1);
        else if (setup && confirming) {
            firstPin = null;
            confirming = false;
            renderHeader();
        }
        updateDots();
    }

    private void setBusy(boolean value) {
        busy = value;
        for (Button key : keypadButtons) {
            key.setEnabled(!value);
            key.setAlpha(value ? 0.6f : 1f);
        }
        recovery.setEnabled(!value);
        logout.setEnabled(!value);
        recovery.setAlpha(value ? 0.6f : 1f);
        logout.setAlpha(value ? 0.6f : 1f);
        error.setTextColor(value ? Color.DKGRAY : ERROR_COLOR);
        if (value) error.setText("Checking…");
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (session.isExpired()) {
            AppLockManager.get().forceLogin(this, "Your login has expired. Please sign in again.");
            return;
        }
        setup = !session.hasPin();
        confirming = false;
        firstPin = null;
        clearEntry();
        renderHeader();
        setBusy(false);
        error.setText("");
    }

    private void submit() {
        if (busy || !resumed) return;
        String value = enteredPin.toString();
        if (!PinCrypto.valid(value)) {
            error.setText("Enter exactly six numeric digits.");
            return;
        }
        if (setup && !confirming) {
            firstPin = value;
            confirming = true;
            clearEntry();
            renderHeader();
            return;
        }
        if (setup && !value.equals(firstPin)) {
            clearEntry();
            error.setText("The PINs do not match. Please try again.");
            return;
        }
        int epoch = attemptEpoch;
        boolean settingPin = setup;
        String expectedToken = session.getMemberAccessToken();
        setBusy(true);
        worker.execute(() -> {
            try {
                boolean accepted;
                if (settingPin) { session.setPin(value, expectedToken); accepted = true; }
                else accepted = session.verifyPin(value);
                runOnUiThread(() -> {
                    if (!resumed || isFinishing() || isDestroyed() || epoch != attemptEpoch) return;
                    if (!session.hasLogin()) AppLockManager.get().forceLogin(this, "Five incorrect PIN attempts. Please sign in again.");
                    else if (session.isExpired()) AppLockManager.get().forceLogin(this, "Your login has expired. Please sign in again.");
                    else if (!accepted) {
                        setBusy(false);
                        clearEntry();
                        error.setText("Incorrect PIN. Attempts remaining: " + session.attemptsRemaining());
                    } else validateServer(epoch, expectedToken);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (!resumed || isFinishing() || isDestroyed() || epoch != attemptEpoch) return;
                    setBusy(false);
                    clearEntry();
                    error.setText("Unable to verify this PIN. Use account login to recover access.");
                });
            }
        });
    }

    private void validateServer(int epoch, String expectedToken) {
        sessionCall = ApiClient.getAuthApiService().session();
        sessionCall.enqueue(new Callback<SessionResponse>() {
            public void onResponse(Call<SessionResponse> call, Response<SessionResponse> response) {
                if (!resumed || isFinishing() || isDestroyed() || epoch != attemptEpoch || !expectedToken.equals(session.getMemberAccessToken())) return;
                SessionResponse body = response.body();
                if (response.isSuccessful() && body != null && body.getMemberId() == session.getMemberId()
                        && body.getMemberSessionExpiresAtUnixMs() == session.getExpiresAt()
                        && body.getServerTimeUnixMs() < body.getMemberSessionExpiresAtUnixMs()) {
                    session.updateServerTime(body.getServerTimeUnixMs()); AppLockManager.get().authenticated();
                    if (!getIntent().getBooleanExtra(AppLockManager.EXTRA_RETURN_TO_EXISTING, false))
                        startActivity(AppLockManager.get().homeIntent(PinActivity.this));
                    finish();
                } else if (response.code() == 401 || response.code() == 403 || response.isSuccessful())
                    AppLockManager.get().forceLogin(PinActivity.this, "Your login is no longer valid. Please sign in again.");
                else {
                    setBusy(false);
                    clearEntry();
                    error.setText("The API could not check your session. Please try again.");
                }
            }
            public void onFailure(Call<SessionResponse> call, Throwable errorValue) {
                if (!resumed || isFinishing() || isDestroyed() || epoch != attemptEpoch || call.isCanceled()) return;
                setBusy(false);
                clearEntry();
                error.setText("Unable to connect to SportHub. Check your connection and try again.");
            }
        });
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            deleteDigit();
            return true;
        }
        char digit = (char) event.getUnicodeChar();
        if (digit >= '0' && digit <= '9') {
            enterDigit(digit);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onPause() {
        resumed = false;
        attemptEpoch++;
        if (sessionCall != null) sessionCall.cancel();
        firstPin = null;
        confirming = false;
        clearEntry();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        firstPin = null;
        enteredPin.setLength(0);
        super.onDestroy();
    }
}
