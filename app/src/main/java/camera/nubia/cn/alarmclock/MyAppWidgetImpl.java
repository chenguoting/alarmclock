package camera.nubia.cn.alarmclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;

public class MyAppWidgetImpl {
    private final static String TAG = "MyAppWidgetImpl";
    private final static int requestCode = 1014;
    private final static int REFRESH_INTERVAL = 1000;
    private final static int msg_update = 0;
    private static int[][] soundId = new int[][]{
            {20, 30, R.raw.sound_2030, 0}, //20:30
            {21, 00, R.raw.sound_2100, 0}, //21:00
            {21, 30, R.raw.sound_2130, 0}, //21:30
            {22, 00, R.raw.sound_2200, 0}, //22:00
            {22, 30, R.raw.sound_2230, 0}  //22:30
    };
    private AppWidgetManager appWidgetManager;
    private Context context;
    private SharedPreferences sharedPreferences;
    private boolean isInit = false;
    private RemoteViews views;
    private HandlerThread mHandlerThread;
    private WidgetHandler mHandler;
    private MediaPlayer mediaPlayer;
    private String timeStr = "";

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.i(TAG, "onUpdate");
        this.appWidgetManager = appWidgetManager;
        this.context = context;
        init();
        long cur = System.currentTimeMillis();
        BeiJingDate date = new BeiJingDate(cur);
        updateText(date.getTimeStr());
        mHandler.removeMessages(msg_update);
        mHandler.sendEmptyMessageDelayed(msg_update, REFRESH_INTERVAL);
    }

    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.i(TAG, "onDeleted");
    }

    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        Log.i(TAG, "onEnabled");
        this.context = context;
        init();
    }

    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.i(TAG, "onDisabled");
        release();
    }

    private void init() {
        if(isInit) {
            return;
        }
        isInit = true;
        Log.i(TAG, "init");
        // Construct the RemoteViews object
        views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.appwidget_text, pendingIntent);
        sharedPreferences = context.getSharedPreferences(MainActivity.packageName, Context.MODE_PRIVATE);
        mHandlerThread = new HandlerThread("WidgetHandlerThread");
        mHandlerThread.start();
        mHandler = new WidgetHandler(mHandlerThread.getLooper());
    }

    private void release() {
        if(!isInit) {
            return;
        }
        isInit = false;
        Log.i(TAG, "release");
        mHandlerThread.quit();
        mHandler.removeCallbacksAndMessages(null);
        timeStr = "";
        if(mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private class WidgetHandler extends Handler {
        public WidgetHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case msg_update:
                    long cur = System.currentTimeMillis();
                    //更新时间显示
                    BeiJingDate date = new BeiJingDate(cur);
                    String str = date.getTimeStr();
                    if(!timeStr.equals(str)) {
                        updateText(str);
                    }
                    //对比时间，播放提示
                    int resId = checkTime(date);
                    if(resId != 0) {
                        playTipSound(resId);
                    }

                    mHandler.removeMessages(msg_update);
                    mHandler.sendEmptyMessageDelayed(msg_update, REFRESH_INTERVAL);
                    break;
            }
        }
    }

    private void updateText(String time) {
        if(context == null) {
            return;
        }
        try {
            Log.i(TAG, "updateText "+time);
            views.setTextViewText(R.id.appwidget_text, time);
            ComponentName componentName = new ComponentName(context, NewAppWidget.class);
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(componentName, views);
            timeStr = time;
        } catch (Exception e) {
            Log.e(TAG, "updateText fail "+e.getMessage());
        }
    }

    private static int checkTime(BeiJingDate date) {
        int resId = 0;
        long min = date.getMin();
        long hour = date.getHour();
        for(int[] item : soundId) {
            if(item[0] == hour && item[1] == min) {
                if(item[3] == 0) {
                    resId = item[2];
                    item[3] = 1;
                    Log.i(TAG, "find " + hour + ":" + min + " " + resId);
                }
            }
            else {
                item[3] = 0;
            }
        }
        return resId;
    }

    private void playTipSound(int resId) {
        if(context == null) {
            return;
        }
        try {
            boolean alarm = sharedPreferences.getBoolean(MainActivity.ALARM, false);
            Log.i(TAG, "alarm setting : "+alarm);
            if(alarm) {
                if(mediaPlayer != null) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                mediaPlayer = MediaPlayer.create(context, resId);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "playTipSound fail "+e.getMessage());
        }
    }
}
