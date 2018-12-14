package camera.nubia.cn.alarmclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;

/**
 * Implementation of App Widget functionality.
 */
public class NewAppWidget extends AppWidgetProvider {
    private final static String TAG = "NewAppWidget";
    AppWidgetManager appWidgetManager;
    Context context;
    SharedPreferences sharedPreferences;
    boolean isEnabled = false;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        long cur = System.currentTimeMillis();
        long min = cur / 1000 / 60;
        long hour = min / 60 + 8; //北京时间所以加8
        views.setTextViewText(R.id.appwidget_text, (hour%24)+":"+(min%60));
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1014, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.appwidget_text, pendingIntent);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.i(TAG, "onUpdate");
        this.appWidgetManager = appWidgetManager;
        this.context = context;
        sharedPreferences = context.getSharedPreferences(MainActivity.packageName, Context.MODE_PRIVATE);
        init();
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        mHandler.removeMessages(msg_update);
        mHandler.sendEmptyMessageDelayed(msg_update, 1000);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        Log.i(TAG, "onEnabled");
        init();
        mHandler.removeMessages(msg_update);
        mHandler.sendEmptyMessage(msg_update);
    }

    private void init() {
        if(isEnabled) {
            return;
        }
        isEnabled = true;
        mHandlerThread = new HandlerThread("WidgetHandlerThread");
        mHandlerThread.start();
        mHandler = new WidgetHandler(mHandlerThread.getLooper());
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.i(TAG, "onDisabled");
        release();
    }

    private void release() {
        if(!isEnabled) {
            return;
        }
        isEnabled = false;
        mHandler.removeCallbacksAndMessages(null);
        mHandlerThread.quitSafely();
    }

    static int[] soundId = new int[]{
            R.raw.sound_2030, //8:30
            R.raw.sound_2100, //9:00
            R.raw.sound_2130, //9:30
            R.raw.sound_2200, //10:00
            R.raw.sound_2230
    };
    int sound_830 = 0;

    final static int msg_update = 0;
    final static String ALARM_TIME = "alarm_time";
    HandlerThread mHandlerThread;
    WidgetHandler mHandler;
    MediaPlayer mediaPlayer;
    long mHour, mMin;
    class WidgetHandler extends Handler {
        public WidgetHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case msg_update:
                    //boolean alarm = sharedPreferences.getBoolean(MainActivity.ALARM, false);
                    //Log.i(TAG, "msg_update "+alarm);
                    long cur = System.currentTimeMillis();
                    long min = cur / 1000 / 60;
                    long hour = min / 60 + 8; //北京时间所以加8
                    if(hour != mHour || min != mMin) {
                        updateText((hour % 24) + ":" + (min % 60));
                        mHour = hour;
                        mMin = min;
                    }
                    long alarmTime = msg.getData().getLong(ALARM_TIME);
                    //Log.i(TAG, "comp "+alarmTime+" "+cur+" "+(cur-alarmTime));
                    if(cur >= alarmTime && alarmTime > 0) {
                        playTipSound(alarmTime);
                    }
                    long start = ((hour/24*24+20-8)*60+30)*60*1000; //20:30
                    long end = ((hour/24*24+22-8)*60+30)*60*1000; //22:30
                    long interval = 30*60*1000;
                    for(long time=start;time<=end;time+=interval) {
                        if(cur<time) {
                            alarmTime = time;
                            break;
                        }
                    }
                    if(cur > alarmTime) {
                        alarmTime = 0;
                    }
                    Message message = mHandler.obtainMessage(msg_update);
                    Bundle bundle = new Bundle();
                    bundle.putLong(ALARM_TIME, alarmTime);
                    message.setData(bundle);
                    mHandler.removeMessages(msg_update);
                    mHandler.sendMessageDelayed(message, 1000);
                    break;
            }
        }
    }

    private void updateText(String time) {
        if(context == null) {
            return;
        }
        Log.i(TAG, "updateText "+time);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        views.setTextViewText(R.id.appwidget_text, time);
        ComponentName componentName = new ComponentName(context, NewAppWidget.class);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(componentName, views);
    }

    private void playTipSound(long time) {
        if(context == null) {
            return;
        }
        if(mediaPlayer != null) {
            mediaPlayer.release();
        }
        int soundIndex;
        long min = time / 1000 / 60;
        long hour = min / 60 + 8; //北京时间所以加8
        if(hour % 24 == 20) {
            soundIndex = -1;
        }
        else if(hour % 24 == 21) {
            soundIndex = 1;
        }
        else if(hour % 24 == 22) {
            soundIndex = 3;
        }
        else {
            return;
        }
        if(min % 60 == 30) {
            soundIndex++;
        }
        Log.i(TAG, "playTipSound "+soundIndex);
        boolean alarm = sharedPreferences.getBoolean(MainActivity.ALARM, false);
        if(alarm) {
            mediaPlayer = MediaPlayer.create(context, soundId[soundIndex]);
            mediaPlayer.start();
        }
    }
}

