package com.pikminassistant.v2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private CheckBox fruit, pot, nectar, petals;
    private TextView status;
    private boolean expanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showMainScreen();
    }

    private void showMainScreen() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(
            Color.rgb(245, 250, 241));

        TextView title = new TextView(this);
        title.setText("🌱 Pikmin 助手 V2");
        title.setTextSize(26);
        title.setTextColor(
            Color.rgb(43, 100, 54));
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("自動化功能設定");
        subtitle.setTextSize(18);
        subtitle.setPadding(0, 35, 0, 15);
        layout.addView(subtitle);

        fruit = new CheckBox(this);
        fruit.setText("🍎 自動收果");
        fruit.setTextSize(18);
        layout.addView(fruit);

        pot = new CheckBox(this);
        pot.setText("🪴 自動收盆");
        pot.setTextSize(18);
        layout.addView(pot);

        nectar = new CheckBox(this);
        nectar.setText("💧 自動餵精華");
        nectar.setTextSize(18);
        layout.addView(nectar);

        petals = new CheckBox(this);
        petals.setText("🌸 自動收花瓣");
        petals.setTextSize(18);
        layout.addView(petals);

        TextView floatingTitle = new TextView(this);
        floatingTitle.setText("懸浮視窗模式");
        floatingTitle.setTextSize(18);
        floatingTitle.setPadding(0, 35, 0, 15);
        layout.addView(floatingTitle);

        Button bubbleMode = new Button(this);
        bubbleMode.setText("單顆懸浮按鈕");
        layout.addView(bubbleMode);

        Button barMode = new Button(this);
        barMode.setText("橫式懸浮控制列");
        layout.addView(barMode);

        status = new TextView(this);
        status.setText("目前狀態：尚未啟動");
        status.setTextSize(16);
        status.setPadding(0, 30, 0, 20);
        layout.addView(status);

        Button stopFloating = new Button(this);
        stopFloating.setText("關閉懸浮視窗");
        layout.addView(stopFloating);

        bubbleMode.setOnClickListener(v -> {
            expanded = false;
            openFloatingWindow();
        });

        barMode.setOnClickListener(v -> {
            expanded = true;
            openFloatingWindow();
        });

        stopFloating.setOnClickListener(v -> {
            stopService(new Intent(
                this, FloatingService.class));
            status.setText("目前狀態：懸浮視窗已關閉");
        });

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }

    private void openFloatingWindow() {

        if (!Settings.canDrawOverlays(this)) {

            Toast.makeText(
                this,
                "請先允許顯示在其他應用程式上層",
                Toast.LENGTH_LONG
            ).show();

            Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
            );

            startActivity(intent);
            return;
        }

        Intent serviceIntent = new Intent(
            this, FloatingService.class);

        serviceIntent.putExtra(
            "expanded", expanded);

        startService(serviceIntent);

        status.setText(
            expanded
                ? "目前狀態：橫式懸浮控制列已開啟"
                : "目前狀態：單顆懸浮按鈕已開啟"
        );
    }
}
