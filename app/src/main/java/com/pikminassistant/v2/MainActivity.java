package com.pikminassistant.v2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private LinearLayout root;
    private TextView status;
    private final int green = Color.rgb(43, 110, 65);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildScreen();
    }

    private TextView heading(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(19);
        t.setTextColor(green);
        t.setPadding(0, 28, 0, 12);
        root.addView(t);
        return t;
    }

    private EditText number(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setInputType(2);
        root.addView(e);
        return e;
    }

    private Spinner spinner(String[] items) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> adapter =
            new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                items);
        s.setAdapter(adapter);
        root.addView(s);
        return s;
    }

    private CheckBox check(String label) {
        CheckBox c = new CheckBox(this);
        c.setText(label);
        c.setTextSize(16);
        root.addView(c);
        return c;
    }

    private void collectSection(String title, String key) {
        heading(title);

        CheckBox enabled = check("啟用" + title);

        TextView colorLabel = heading("皮克敏顏色");
        colorLabel.setTextSize(15);

        String[] colors = {
            "紅", "黃", "藍", "紫",
            "白", "岩石", "羽翅", "冰"
        };

        CheckBox automatic = check("自動選擇顏色");
        automatic.setChecked(true);

        ArrayList<CheckBox> colorChecks = new ArrayList<>();

        for (String color : colors) {
            CheckBox c = check(color);
            c.setEnabled(false);
            colorChecks.add(c);
        }

        automatic.setOnCheckedChangeListener((b, checked) -> {
            for (CheckBox c : colorChecks) {
                c.setEnabled(!checked);
            }
        });

        heading("皮克敏數量模式");
        Spinner mode = spinner(new String[]{
            "自動", "最大值", "自訂數量"
        });

        EditText amount = number("自訂皮克敏數量", "10");
        amount.setVisibility(View.GONE);

        mode.setOnItemSelectedListener(
            new android.widget.AdapterView.OnItemSelectedListener() {
                public void onNothingSelected(
                    android.widget.AdapterView<?> parent) {}

                public void onItemSelected(
                    android.widget.AdapterView<?> parent,
                    View view, int position, long id) {
                    amount.setVisibility(
                        position == 2 ? View.VISIBLE : View.GONE);
                }
            });

        heading("收取目標");
        EditText target = number("設定收取數量", "20");

        TextView remaining = new TextView(this);
        remaining.setText("剩餘：20");
        remaining.setTextSize(17);
        root.addView(remaining);

        Button save = new Button(this);
        save.setText("儲存" + title + "設定");
        root.addView(save);

        save.setOnClickListener(v -> {
            String count = target.getText().toString().trim();

            if (count.isEmpty()) {
                target.setError("請輸入數量");
                return;
            }

            int total;
            try {
                total = Integer.parseInt(count);
                if (total <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                target.setError("請輸入大於零的整數");
                return;
            }

            StringBuilder selected = new StringBuilder();
            for (CheckBox c : colorChecks) {
                if (c.isChecked()) {
                    if (selected.length() > 0) selected.append("、");
                    selected.append(c.getText());
                }
            }

            if (!automatic.isChecked() && selected.length() == 0) {
                Toast.makeText(this, "請選擇皮克敏顏色",
                    Toast.LENGTH_SHORT).show();
                return;
            }

            getPreferences(MODE_PRIVATE).edit()
                .putBoolean(key + "_enabled", enabled.isChecked())
                .putString(key + "_colors",
                    automatic.isChecked() ? "自動" : selected.toString())
                .putInt(key + "_mode", mode.getSelectedItemPosition())
                .putString(key + "_amount", amount.getText().toString())
                .putInt(key + "_target", total)
                .apply();

            remaining.setText("剩餘：" + total);
            Toast.makeText(this, "設定已儲存",
                Toast.LENGTH_SHORT).show();
        });
    }

    private void buildScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 32, 28, 40);
        root.setBackgroundColor(Color.rgb(247, 250, 245));

        TextView title = heading("🌱 Pikmin 助手 V2");
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);

        collectSection("自動收果", "fruit");
        collectSection("自動收盆", "pot");

        heading("自動餵精華／收花瓣");
        check("啟用自動餵精華");
        check("啟用自動收花瓣");

        number("每批餵食輪數", "5");
        number("每次餵食秒數", "2");
        number("收花瓣後等待秒數", "3");

        TextView note = new TextView(this);
        note.setText(
            "換批：辨識哨子並快速點擊兩下\n" +
            "餵食批數：不限制\n" +
            "偵測花瓣快滿：停止全部餵食\n" +
            "目前為設定介面，尚未執行遊戲操作");
        note.setPadding(0, 15, 0, 20);
        root.addView(note);

        heading("懸浮視窗模式");

        Button bubble = new Button(this);
        bubble.setText("開啟單顆懸浮按鈕");
        root.addView(bubble);

        Button bar = new Button(this);
        bar.setText("開啟橫式懸浮控制列");
        root.addView(bar);

        Button close = new Button(this);
        close.setText("關閉懸浮視窗");
        root.addView(close);

        status = new TextView(this);
        status.setText("目前狀態：尚未啟動");
        status.setPadding(0, 15, 0, 15);
        root.addView(status);

        bubble.setOnClickListener(v -> openFloating(false));
        bar.setOnClickListener(v -> openFloating(true));

        close.setOnClickListener(v -> {
            stopService(new Intent(this, FloatingService.class));
            status.setText("懸浮視窗已關閉");
        });

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void openFloating(boolean expanded) {
        if (!Settings.canDrawOverlays(this)) {
            Intent permission = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivity(permission);
            return;
        }

        Intent service = new Intent(this, FloatingService.class);
        service.putExtra("expanded", expanded);
        startService(service);

        status.setText(expanded
            ? "橫式懸浮控制列已開啟"
            : "單顆懸浮按鈕已開啟");
    }
}
