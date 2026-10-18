package com.pikminassistant.v2;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class FloatingService extends Service {
    WindowManager wm;
    WindowManager.LayoutParams p;
    LinearLayout root, fields;
    View window;
    SharedPreferences prefs;
    int dark = 0xff245c45;
    int mint = 0xffe6f3e8;
    int mode = 0;
    String task = "feed";
    boolean running = false, paused = false;

    int dp(int n) {
        return (int)(n * getResources()
            .getDisplayMetrics().density);
    }

    GradientDrawable bg(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(15));
        return d;
    }

    TextView btn(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(14);
        t.setTextColor(-1);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(9),dp(10),dp(9),dp(10));
        t.setBackground(bg(dark));
        return t;
    }

    void message(String s) {
        Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        prefs = getSharedPreferences("floating",0);
    }

    @Override
    public int onStartCommand(Intent i,int flags,int id) {
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
        remove();
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setBackground(bg(mint));

        int type = Build.VERSION.SDK_INT >= 26
            ? 2038 : 2002;

        p = new WindowManager.LayoutParams(
            -2,-2,type,32,PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.LEFT;
        p.x = prefs.getInt("x",30);
        p.y = prefs.getInt("y",200);

        if (mode == 0) {
            TextView bubble = btn("🌱");
            bubble.setTextSize(22);
            root.addView(bubble);
            drag(bubble,()->{mode=2;show();});
            bubble.setOnLongClickListener(v->{
                confirmClose();
                return true;
            });
        } else if (mode == 1) {
            LinearLayout bar = new LinearLayout(this);
            root.addView(bar);
            String[] names = {"☰","⚙","▶","Ⅱ","✕"};
            for (int n=0;n<names.length;n++) {
                final int index=n;
                TextView b=btn(names[n]);
                bar.addView(b);
                if(index==0) {
                    drag(b,()->{mode=0;show();});
                } else {
                    b.setOnClickListener(v->{
                        if(index==1){mode=2;show();}
                        if(index==2)start();
                        if(index==3)pause();
                        if(index==4)confirmClose();
                    });
                }
            }
        } else {
            panel();
        }
        window=root;
        wm.addView(window,p);
    }

    void start() {
        running=true;
        paused=false;
        message("介面已啟動，自動操作尚未連接");
    }

    void pause() {
        if(!running)return;
        paused=!paused;
        message(paused?"已暫停":"已繼續");
    }

    void confirmClose() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("關閉 Pikmin 助手？")
            .setMessage("將停止服務並移除懸浮視窗")
            .setNegativeButton("取消",null)
            .setPositiveButton("關閉",(d,w)->{
                running=false;
                paused=false;
                stopSelf();
            }).show();
    }
        void panel() {
        p.width=Math.min(dp(305),
            getResources().getDisplayMetrics().widthPixels-dp(24));
        p.height=-2;

        LinearLayout head=new LinearLayout(this);
        root.addView(head);
        TextView title=btn("🌱 Pikmin 助手 V2");
        head.addView(title,new LinearLayout.LayoutParams(
            0,dp(44),1));
        drag(title,()->{});

        TextView small=btn("−");
        head.addView(small);
        small.setOnClickListener(v->{mode=0;show();});

        TextView exit=btn("✕");
        head.addView(exit);
        exit.setOnClickListener(v->confirmClose());

        ScrollView scroll=new ScrollView(this);
        root.addView(scroll);

        LinearLayout body=new LinearLayout(this);
        body.setOrientation(1);
        body.setPadding(dp(10),dp(8),dp(10),dp(10));
        scroll.addView(body);

        LinearLayout tabs=new LinearLayout(this);
        body.addView(tabs);
        String[] names={"收果","收盆","餵食"};
        String[] keys={"fruit","pot","feed"};

        for(int n=0;n<3;n++){
            final String k=keys[n];
            TextView b=btn(names[n]);
            tabs.addView(b,new LinearLayout.LayoutParams(
                0,dp(43),1));
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
        start.setOnClickListener(v->start());

        TextView pause=btn("Ⅱ 暫停／繼續");
        body.addView(pause);
        pause.setOnClickListener(v->pause());

        TextView stop=btn("■ 停止執行");
        body.addView(stop);
        stop.setOnClickListener(v->{
            running=false;
            paused=false;
            message("已停止");
        });
    }

    void label(String s){
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextColor(dark);
        t.setTextSize(13);
        t.setPadding(0,dp(7),0,dp(2));
        fields.addView(t);
    }

    void num(String s,String key,int def){
        label(s);
        EditText e=new EditText(this);
        e.setTextSize(15);
        e.setSingleLine(true);
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
        c.setTextSize(13);
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
                pos[0]=p.x;
                pos[1]=p.y;
                moved[0]=false;
                return true;
            }
            if(e.getAction()==2){
                float dx=e.getRawX()-down[0];
                float dy=e.getRawY()-down[1];
                if(Math.hypot(dx,dy)>dp(8))
                    moved[0]=true;
                if(moved[0]){
                    p.x=pos[0]+(int)dx;
                    p.y=pos[1]+(int)dy;
                    wm.updateViewLayout(window,p);
                }
                return true;
            }
            if(e.getAction()==1){
                if(moved[0]){
                    prefs.edit()
                        .putInt("x",p.x)
                        .putInt("y",p.y).apply();
                }else tap.run();
                return true;
            }
            return true;
        });
    }

    void remove(){
        if(window!=null){
            try{wm.removeView(window);}
            catch(Exception ignored){}
            window=null;
        }
    }

    @Override
    public void onDestroy(){
        running=false;
        paused=false;
        remove();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i){
        return null;
    }
}
