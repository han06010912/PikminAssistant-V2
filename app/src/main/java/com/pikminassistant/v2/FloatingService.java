package com.pikminassistant.v2;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class FloatingService extends Service {
    WindowManager wm;
    WindowManager.LayoutParams p;
    View window;
    LinearLayout root, fields;
    SharedPreferences prefs;
    int dark = 0xff245c45, mint = 0xffe6f3e8;
    int mode = 0;
    String task = "feed";

    int dp(int n) {
        return (int)(n * getResources()
            .getDisplayMetrics().density);
    }

    GradientDrawable bg(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(18));
        return d;
    }

    TextView btn(String label) {
        TextView v = new TextView(this);
        v.setText(label);
        v.setTextSize(16);
        v.setTextColor(-1);
        v.setGravity(17);
        v.setPadding(dp(12),dp(12),dp(12),dp(12));
        v.setBackground(bg(dark));
        return v;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        prefs = getSharedPreferences("floating",0);
    }

    @Override
    public int onStartCommand(Intent i,int f,int id) {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        mode = i != null && i.getBooleanExtra(
            "expanded",false) ? 1 : 0;
        show();
        return START_NOT_STICKY;
    }

    void show() {
        if (window != null) wm.removeView(window);
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setBackground(bg(mint));

        p = new WindowManager.LayoutParams(
            -2,-2,2038,8,-3);
        p.gravity = Gravity.TOP | Gravity.LEFT;
        p.x = prefs.getInt("x",30);
        p.y = prefs.getInt("y",200);

        if (mode == 0) {
            TextView b = btn("🌱");
            root.addView(b);
            drag(b,() -> {mode=2;show();});
        } else if (mode == 1) {
            LinearLayout bar = new LinearLayout(this);
            root.addView(bar);
            for (String s : new String[]{"☰","⚙","▶","Ⅱ","■"}) {
                TextView b = btn(s);
                bar.addView(b);
                if (s.equals("☰"))
                    drag(b,()->{mode=0;show();});
                else if (s.equals("⚙"))
                    b.setOnClickListener(v->{mode=2;show();});
                else if (s.equals("■"))
                    b.setOnClickListener(v->stopSelf());
                else
                    b.setOnClickListener(v->Toast.makeText(
                        this,"自動操作尚未啟用",0).show());
            }
        } else {
            panel();
        }
        window = root;
        wm.addView(window,p);
    }
    void panel() {
        p.width = Math.min(dp(350),
            getResources().getDisplayMetrics().widthPixels-dp(24));
        p.height = Math.min(dp(600),
            getResources().getDisplayMetrics().heightPixels-dp(100));
        p.flags = 32;

        LinearLayout head = new LinearLayout(this);
        root.addView(head);
        TextView title = btn("🌱 Pikmin 助手 V2");
        head.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));
        drag(title,()->{});
        TextView close = btn("−");
        head.addView(close);
        close.setOnClickListener(v->{mode=0;show();});

        ScrollView scroll = new ScrollView(this);
        root.addView(scroll);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(1);
        body.setPadding(dp(12),dp(8),dp(12),dp(12));
        scroll.addView(body);

        LinearLayout tabs = new LinearLayout(this);
        body.addView(tabs);
        String[] names = {"收果","收盆","餵食"};
        String[] keys = {"fruit","pot","feed"};
        for(int n=0;n<3;n++){
            final String k=keys[n];
            TextView b=btn(names[n]);
            tabs.addView(b,new LinearLayout.LayoutParams(0,dp(50),1));
            b.setOnClickListener(v->{task=k;show();});
        }

        fields=new LinearLayout(this);
        fields.setOrientation(1);
        body.addView(fields);

        if(task.equals("feed")){
            num("餵食輪數","rounds",5);
            num("餵食秒數","seconds",10);
            num("收花瓣後等待秒數","wait",3);
            check("花瓣快滿停止全部餵食",true);
            label("完成輪數後連點哨子換批");
        }else{
            check("自動選擇顏色",true);
            for(String c:new String[]{
                "紅","黃","藍","紫","白","岩石","羽翅","冰"})
                check(c,false);
            label("皮克敏數量");
            Spinner spinner=new Spinner(this);
            spinner.setAdapter(new ArrayAdapter<String>(
                this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"自動","最大值","自訂"}));
            fields.addView(spinner);
            num("自訂數量",task+"amount",10);
            num("收取目標",task+"target",20);
        }

        TextView start=btn("▶ 開始執行");
        body.addView(start);
        start.setOnClickListener(v->Toast.makeText(
            this,"目前為介面測試版本",0).show());
    }

    void label(String s){
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextColor(dark);
        t.setTextSize(16);
        t.setPadding(0,dp(10),0,dp(5));
        fields.addView(t);
    }

    void num(String s,String key,int def){
        label(s);
        EditText e=new EditText(this);
        e.setInputType(2);
        e.setText(""+prefs.getInt(key,def));
        fields.addView(e);
        e.setOnFocusChangeListener((v,f)->{
            if(!f)try{
                prefs.edit().putInt(key,Integer.parseInt(
                    e.getText().toString())).apply();
            }catch(Exception ignored){}
        });
    }

    void check(String s,boolean value){
        CheckBox c=new CheckBox(this);
        c.setText(s);
        c.setChecked(value);
        fields.addView(c);
    }

    void drag(View v,Runnable tap){
        final float[] down=new float[2];
        final int[] pos=new int[2];
        final boolean[] moved={false};
        v.setOnTouchListener((view,e)->{
            if(e.getAction()==0){
                down[0]=e.getRawX();
                down[1]=e.getRawY();
                pos[0]=p.x;pos[1]=p.y;
                moved[0]=false;
                return true;
            }
            if(e.getAction()==2){
                float dx=e.getRawX()-down[0];
                float dy=e.getRawY()-down[1];
                if(Math.hypot(dx,dy)>dp(8))moved[0]=true;
                if(moved[0]){
                    p.x=pos[0]+(int)dx;
                    p.y=pos[1]+(int)dy;
                    wm.updateViewLayout(window,p);
                }
                return true;
            }
            if(e.getAction()==1){
                if(moved[0])prefs.edit()
                    .putInt("x",p.x).putInt("y",p.y).apply();
                else tap.run();
                return true;
            }
            return true;
        });
    }

    @Override
    public void onDestroy(){
        if(window!=null)wm.removeView(window);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i){
        return null;
    }
}
