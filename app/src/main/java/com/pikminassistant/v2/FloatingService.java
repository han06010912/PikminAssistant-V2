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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.text.InputType;

public class FloatingService extends Service {

    private WindowManager manager;
    private WindowManager.LayoutParams params;
    private View floatingView;
    private SharedPreferences prefs;

    private final int DARK = Color.rgb(36, 92, 69);
    private final int MINT = Color.rgb(230, 243, 232);
    private final int PALE = Color.rgb(248, 252, 247);

    private boolean expanded = false;
    private boolean panelOpen = false;
    private boolean running = false;
    private boolean paused = false;

    private String selectedTask = "feed";
    private int startX, startY;
    private float downX, downY;
    private boolean dragging;

    private LinearLayout settingsArea;

    @Override
    public void onCreate() {
        super.onCreate();
        manager = (WindowManager)
            getSystemService(WINDOW_SERVICE);
        prefs = getSharedPreferences(
            "floating", MODE_PRIVATE);
        selectedTask = prefs.getString("task", "feed");
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
            panelOpen = false;
        }

        showWindow();
        return START_NOT_STICKY;
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            getResources().getDisplayMetrics());
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(
        String value, int size, int color) {

        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private TextView action(
        String value, int color, int textColor) {

        TextView t = text(value, 16, textColor);
        t.setGravity(Gravity.CENTER);
        t.setBackground(bg(color, 14));
        t.setPadding(dp(12), dp(13),
            dp(12), dp(13));
        return t;
    }

    private void addSpace(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(
            1, dp(height)));
    }

    private EditText number(
        LinearLayout parent,
        String label,
        String key,
        int defaultValue) {

        parent.addView(text(label, 15, DARK));

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setInputType(
            InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(
            prefs.getInt(key, defaultValue)));
        input.setSelectAllOnFocus(true);

        parent.addView(input);

        input.setOnFocusChangeListener((v, focus) -> {
            if (!focus) saveNumber(input, key);
        });

        return input;
    }

    private void saveNumber(EditText input, String key) {
        try {
            int value = Integer.parseInt(
                input.getText().toString().trim());
            if (value < 0) throw new Exception();

            prefs.edit().putInt(key, value).apply();
        } catch (Exception e) {
            Toast.makeText(this,
                "請輸入有效數字",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void showWindow() {
        removeWindow();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(bg(MINT, 22));
        root.setElevation(dp(8));

        if (panelOpen) {
            buildPanel(root);
        } else if (expanded) {
            buildBar(root);
        } else {
            buildBubble(root);
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
        params.x = prefs.getInt("x", dp(30));
        params.y = prefs.getInt("y", dp(200));

        if (panelOpen) {
            params.width = Math.min(
                dp(350),
                getResources().getDisplayMetrics()
                    .widthPixels - dp(24));
            params.height = Math.min(
                dp(620),
                getResources().getDisplayMetrics()
                    .heightPixels - dp(100));
            params.flags =
                WindowManager.LayoutParams
                    .FLAG_NOT_TOUCH_MODAL;
            params.softInputMode =
                WindowManager.LayoutParams
                    .SOFT_INPUT_ADJUST_RESIZE;
        }

        floatingView = root;
        manager.addView(floatingView, params);
    }

    private void buildBubble(LinearLayout root) {
        TextView bubble = action(
            "🌱", DARK, Color.WHITE);
        bubble.setTextSize(25);
        root.addView(bubble);

        attachDrag(bubble, () -> {
            panelOpen = true;
            showWindow();
        });
    }

    private void buildBar(LinearLayout root) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(5), dp(5), dp(5), dp(5));
        root.addView(bar);

        TextView move = action("☰", DARK, Color.WHITE);
        TextView setup = action("⚙", DARK, Color.WHITE);
        TextView play = action("▶", DARK, Color.WHITE);
        TextView pause = action("Ⅱ", DARK, Color.WHITE);
        TextView close = action("×", DARK, Color.WHITE);

        bar.addView(move);
        bar.addView(setup);
        bar.addView(play);
        bar.addView(pause);
        bar.addView(close);

        attachDrag(move, () -> {
            expanded = false;
            showWindow();
        });

        setup.setOnClickListener(v -> {
            panelOpen = true;
            showWindow();
        });

        play.setOnClickListener(v -> {
            running = true;
            paused = false;
            Toast.makeText(this,
                "控制狀態：執行中（尚未連接自動操作）",
                Toast.LENGTH_SHORT).show();
        });

        pause.setOnClickListener(v -> {
            paused = !paused;
            Toast.makeText(this,
                paused ? "已暫停" : "已繼續",
                Toast.LENGTH_SHORT).show();
        });

        close.setOnClickListener(v -> stopSelf());
    }

    private void buildPanel(LinearLayout root) {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(15), dp(12),
            dp(12), dp(10));
        root.addView(header);

        TextView title = text(
            "🌱 Pikmin 助手 V2", 19, DARK);
        header.addView(title,
            new LinearLayout.LayoutParams(
                0, dp(48), 1));

        TextView collapse = action(
            "−", DARK, Color.WHITE);
        header.addView(collapse);

        attachDrag(title, () -> {});
        collapse.setOnClickListener(v -> {
            panelOpen = false;
            showWindow();
        });

        ScrollView scroll = new ScrollView(this);
        root.addView(scroll,
            new LinearLayout.LayoutParams(
                -1, 0, 1));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(15), dp(5),
            dp(15), dp(15));
        scroll.addView(content);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(tabs);

        addTab(tabs, "收果", "fruit");
        addTab(tabs, "收盆", "pot");
        addTab(tabs, "餵食", "feed");

        addSpace(content, 12);

        settingsArea = new LinearLayout(this);
        settingsArea.setOrientation(LinearLayout.VERTICAL);
        settingsArea.setPadding(dp(12), dp(12),
            dp(12), dp(12));
        settingsArea.setBackground(bg(PALE, 15));
        content.addView(settingsArea);

        updateSettings();

        addSpace(content, 12);

        TextView start = action(
            "▶ 開始執行", DARK, Color.WHITE);
        content.addView(start);

        start.setOnClickListener(v -> {
            saveVisibleInputs();
            running = true;
            paused = false;
            Toast.makeText(this,
                "設定已保存，自動操作尚未實作",
                Toast.LENGTH_LONG).show();
        });

        addSpace(content, 10);

        TextView stop = action(
