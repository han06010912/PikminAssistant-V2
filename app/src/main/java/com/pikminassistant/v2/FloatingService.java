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
    int dark=0xff245c45, mint=0xffe6f3e8;
    int mode=0;
    String task="feed";
    boolean running=false, paused=false;

    int dp(int n){
        return (int)(n*getResources()
            .getDisplayMetrics().density);
    }

    GradientDrawable bg(int color){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(12));
        return d;
    }

    TextView btn(String s){
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setTextColor(-1);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(7),dp(7),dp(7),dp(7));
        t.setBackground(bg(dark));
        return t;
    }

    void msg(String s){
        Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate(){
        super.onCreate();
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        prefs=getSharedPreferences("floating",0);
    }

    @Override
    public int onStartCommand(Intent i,int f,int id){
        if(!Settings.canDrawOverlays(this)){
            stopSelf();
            return START_NOT_STICKY;
        }
        mode=i!=null&&i.getBooleanExtra(
            "expanded",false)?1:0;
        show();
        return START_NOT_STICKY;
    }

    void show(){
        remove();
        root=new LinearLayout(this);
        root.setOrientation(1);
        root.setBackground(bg(mint));

        int type=Build.VERSION.SDK_INT>=26?2038:2002;
        p=new WindowManager.LayoutParams(
            -2,-2,type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT;
        p.x=prefs.getInt("x",dp(20));
        p.y=prefs.getInt("y",dp(160));

        if(mode==0){
            TextView bubble=btn("🌱");
            bubble.setTextSize(21);
            root.addView(bubble);
            drag(bubble,()->{mode=2;show();});
        }else if(mode==1){
            LinearLayout bar=new LinearLayout(this);
            root.addView(bar);
            String[] names={"☰","⚙","▶","Ⅱ","✕"};
            for(int n=0;n<names.length;n++){
                final int k=n;
                TextView b=btn(names[n]);
                bar.addView(b);
                if(k==0)drag(b,()->{mode=0;show();});
                else b.setOnClickListener(v->{
                    if(k==1){mode=2;show();}
                    if(k==2)start();
                    if(k==3)pause();
                    if(k==4)close();
                });
            }
        }else panel();

        window=root;
        wm.addView(window,p);
    }

   void start(){
    PikminAccessibilityService service =
        PikminAccessibilityService.instance;

    if(service == null){
        running = false;
        paused = false;
        msg("無障礙服務尚未連接");
        return;
    }

    running = true;
    paused = false;
    msg("無障礙服務已連接，等待執行動作");
}

    void pause(){
    if(!running){
        msg("目前沒有執行中的任務");
        return;
    }

    paused = !paused;

    if(paused){
        msg("已暫停");
    }else{
        msg("已繼續");
    }
}

    void close(){
        running=false;
        paused=false;
        stopSelf();
    }
    void panel(){
        int screen=getResources()
            .getDisplayMetrics().heightPixels;
        p.width=dp(265);
        p.height=screen/2;

        LinearLayout head=new LinearLayout(this);
        root.addView(head);

        TextView title=btn("🌱 Pikmin 助手 V2");
        head.addView(title,
            new LinearLayout.LayoutParams(0,dp(38),1));
        drag(title,()->{});

        TextView mini=btn("−");
        head.addView(mini);
        mini.setOnClickListener(v->{mode=0;show();});

        TextView exit=btn("✕");
        head.addView(exit);
        exit.setOnClickListener(v->close());

        ScrollView scroll=new ScrollView(this);
        root.addView(scroll,
            new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout body=new LinearLayout(this);
        body.setOrientation(1);
        body.setPadding(dp(7),dp(6),dp(7),dp(8));
        scroll.addView(body);

        LinearLayout tabs=new LinearLayout(this);
        body.addView(tabs);

        String[] names={"收果","收盆","餵食"};
        String[] keys={"fruit","pot","feed"};

        for(int n=0;n<3;n++){
            final String key=keys[n];
            TextView b=btn(names[n]);
            tabs.addView(b,
                new LinearLayout.LayoutParams(0,dp(36),1));
            b.setOnClickListener(v->{task=key;show();});
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
            msg("已停止");
        });
    }

    void label(String s){
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setTextColor(dark);
        fields.addView(t);
    }

    void num(String s,String key,int def){
        label(s);
        EditText e=new EditText(this);
        e.setTextSize(13);
        e.setSingleLine(true);
        e.setInputType(2);
        e.setText(""+prefs.getInt(key,def));
        fields.addView(e);
    }

    void check(String s,boolean value){
        CheckBox c=new CheckBox(this);
        c.setText(s);
        c.setTextSize(12);
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
        remove();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i){
        return null;
    }
}
