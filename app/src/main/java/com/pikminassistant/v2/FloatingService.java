package com.pikminassistant.v2;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingService extends Service {

    private WindowManager manager;
    private WindowManager.LayoutParams params;
    private View floatingView;

    private boolean expanded = false;
    private boolean running = false;
    private boolean paused = false;

    private int startX, startY;
    private float touchX, touchY;

    @Override
    public void onCreate() {
        super.onCreate();
        manager = (WindowManager)
            getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(
            Intent intent, int flags, int startId) {

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            expanded = intent.getBooleanExtra(
                "expanded", false);
        }

        showFloatingWindow();
        return START_NOT_STICKY;
    }

    private GradientDrawable background(int color) {
        GradientDrawable drawable =
            new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(60);
        return drawable;
    }

    private TextView makeButton(String text) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextSize(18);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setPadding(18, 14, 18, 14);
        return button;
    }

    private void showFloatingWindow() {
        removeFloatingWindow();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setBackground(
            background(Color.rgb(45, 120, 70)));

        if (!expanded) {
            TextView bubble = makeButton("🌱");
            bubble.setTextSize(25);
            root.addView(bubble);

            bubble.setOnClickListener(v -> {
                expanded = true;
                showFloatingWindow();
            });

        } else {
            TextView toggle = makeButton("−");
            TextView play = makeButton("▶");
            TextView pause = makeButton("Ⅱ");
            TextView stop = makeButton("■");

            root.addView(toggle);
            root.addView(play);
            root.addView(pause);
            root.addView(stop);

            toggle.setOnClickListener(v -> {
                expanded = false;
                showFloatingWindow();
            });

            play.setOnClickListener(v -> {
                running = true;
                paused = false;
                play.setText("●");
            });

            pause.setOnClickListener(v -> {
                if (running) {
                    paused = !paused;
                    pause.setText(paused ? "▶" : "Ⅱ");
                }
            });

            stop.setOnClickListener(v -> {
                running = false;
                paused = false;
                stopSelf();
            });
        }

        int overlayType =
            Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams
                    .TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams
                    .TYPE_PHONE;

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = getSharedPreferences(
            "floating", MODE_PRIVATE)
            .getInt("x", 40);
        params.y = getSharedPreferences(
            "floating", MODE_PRIVATE)
            .getInt("y", 250);

        root.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = params.x;
                    startY = params.y;
                    touchX = event.getRawX();
                    touchY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    params.x = startX +
                        (int)(event.getRawX() - touchX);
                    params.y = startY +
                        (int)(event.getRawY() - touchY);
                    manager.updateViewLayout(
                        floatingView, params);
                    return true;

                case MotionEvent.ACTION_UP:
                    getSharedPreferences(
                        "floating", MODE_PRIVATE)
                        .edit()
                        .putInt("x", params.x)
                        .putInt("y", params.y)
                        .apply();
                    return true;
            }
            return false;
        });

        floatingView = root;
        manager.addView(floatingView, params);
    }

    private void removeFloatingWindow() {
        if (floatingView != null) {
            manager.removeView(floatingView);
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
