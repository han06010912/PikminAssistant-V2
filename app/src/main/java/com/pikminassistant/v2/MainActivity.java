package com.pikminassistant.v2;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private CheckBox fruit, nectar, petals;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 50, 40, 40);
        layout.setBackgroundColor(Color.rgb(245, 250, 241));

        TextView title = new TextView(this);
        title.setText("🌱 Pikmin 助手 V2");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(43, 100, 54));
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("選擇需要執行的功能");
        subtitle.setTextSize(16);
        subtitle.setPadding(0, 35, 0, 25);
        layout.addView(subtitle);

        fruit = new CheckBox(this);
        fruit.setText("🍎 自動收果");
        fruit.setTextSize(18);
        layout.addView(fruit);

        nectar = new CheckBox(this);
        nectar.setText("💧 自動餵精華");
        nectar.setTextSize(18);
        layout.addView(nectar);

        petals = new CheckBox(this);
        petals.setText("🌸 自動收花瓣");
        petals.setTextSize(18);
        layout.addView(petals);

        status = new TextView(this);
        status.setText("目前狀態：尚未啟動");
        status.setTextSize(16);
        status.setPadding(0, 35, 0, 25);
        layout.addView(status);

        Button start = new Button(this);
        start.setText("啟動");
        layout.addView(start);

        Button stop = new Button(this);
        stop.setText("停止");
        layout.addView(stop);

        start.setOnClickListener(v -> {
            if (!fruit.isChecked() &&
                !nectar.isChecked() &&
                !petals.isChecked()) {
                Toast.makeText(this,
                    "請先選擇功能",
                    Toast.LENGTH_SHORT).show();
                return;
            }

            status.setText("目前狀態：已選擇功能（測試模式）");
            Toast.makeText(this,
                "主畫面測試成功",
                Toast.LENGTH_SHORT).show();
        });

        stop.setOnClickListener(v -> {
            status.setText("目前狀態：已停止");
        });

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }
}
