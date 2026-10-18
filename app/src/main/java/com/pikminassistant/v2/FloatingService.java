package com.pikminassistant.v2;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingService extends Service {

    private WindowManager manager;
    private WindowManager.LayoutParams params;
    private View floatingView;
    private SharedPreferences prefs;

    private boolean expanded = false;
    private boolean running = false;
    private boolean paused = false;

    private int startX, startY;
    private float downX, downY;
    private boolean dragging;

    @Override
    public void onCreate() {
        super.onCreate();
        manager = (WindowManager)
                getSystemService(WINDOW_SERVICE);
        prefs = getSharedPreferences(
                "floating", MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(
            Intent intent, int flags, int startId) {

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null &&
                intent.hasExtra("expanded")) {
            expanded = intent.getBooleanExtra(
                    "expanded", false);
        } else {
            expanded = prefs.getBoolean(
                    "expanded", false);
        }

        showFloatingWindow();
        return START_NOT_STICKY;
    }

    private GradientDrawable background() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.rgb(43, 120, 70));
        d.setCornerRadius(100);
        return d;
    }

    private TextView button(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(20);
        v.setGravity(Gravity.CENTER);
        v.setPadding(22, 18, 22, 18);
        return v;
    }

    private void showFloatingWindow() {
        removeFloatingWindow();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER);
        root.setBackground(background());

        if (!expanded) {
            TextView bubble = button("🌱");
            bubble.setTextSize(26);
            root.addView(bubble);

            attachTouch(bubble, () -> {
                expanded = true;
                saveMode();
                showFloatingWindow();
            });

        } else {
            TextView collapse = button("−");
            TextView play = button("▶");
            TextView pause = button("Ⅱ");
            TextView stop = button("■");

            root.addView(collapse);
            root.addView(play);
            root.addView(pause);
            root.addView(stop);

            attachTouch(collapse, () -> {
                expanded = false;
                saveMode();
                showFloatingWindow();
            });

            attachTouch(play, () -> {
                running = true;
                paused = false;
                play.setText("●");
                pause.setText("Ⅱ");
            });

            attachTouch(pause, () -> {
                if (running) {
                    paused = !paused;
                    pause.setText(paused ? "▶" : "Ⅱ");
                }
            });

            attachTouch(stop, () -> {
                running = false;
                paused = false;
                stopSelf();
            });
        }

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams
                    .TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = prefs.getInt("x", 40);
        params.y = prefs.getInt("y", 250);

        floatingView = root;
        manager.addView(floatingView, params);
    }

    private void attachTouch(
            View target, Runnable onTap) {

        target.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:
                    startX = params.x;
                    startY = params.y;
                    downX = event.getRawX();
                    downY = event.getRawY();
                    dragging = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - downX;
                    float dy = event.getRawY() - downY;

                    if (!dragging &&
                            Math.hypot(dx, dy) >
                            ViewConfiguration.get(this)
                                    .getScaledTouchSlop()) {
                        dragging = true;
                    }

                    if (dragging) {
                        params.x = startX + (int) dx;
                        params.y = startY + (int) dy;

                        try {
                            manager.updateViewLayout(
                                    floatingView, params);
                        } catch (Exception ignored) {
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (dragging) {
                        savePosition();
                    } else {
                        onTap.run();
                    }
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    if (dragging) {
                        savePosition();
                    }
                    return true;
            }
            return false;
        });
    }

    private void savePosition() {
        prefs.edit()
                .putInt("x", params.x)
                .putInt("y", params.y)
                .apply();
    }

    private void saveMode() {
        prefs.edit()
                .putBoolean("expanded", expanded)
                .apply();
    }

    private void removeFloatingWindow() {
        if (floatingView != null) {
            try {
                manager.removeView(floatingView);
            } catch (Exception ignored) {
            }
            floatingView = null;
        }
    }

    @Override
    public void onDestroy() {
        removeFloatingWindow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
