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
    private final static int requestCode = 1014;
    AppWidgetManager appWidgetManager;
    Context context;
    SharedPreferences sharedPreferences;
    boolean isEnabled = false;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        long cur = System.currentTimeMillis();
        BeiJingDate date = new BeiJingDate(cur);
        views.setTextViewText(R.id.appwidget_text, date.getHour()+":"+date.getMin());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
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

    static int[][] soundId = new int[][]{
            {20, 30, R.raw.sound_2030}, //20:30
            {21, 00, R.raw.sound_2100}, //21:00
            {21, 30, R.raw.sound_2130}, //21:30
            {22, 00, R.raw.sound_2200}, //22:00
            {22, 30, R.raw.sound_2230}  //22:30
    };

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
                    long cur = System.currentTimeMillis();
                    BeiJingDate date = new BeiJingDate(cur);
                    if(date.getHour() != mHour || date.getMin() != mMin) {
                        mHour = date.getHour();
                        mMin = date.getMin();
                        updateText(mHour + ":" + mMin);
                    }
                    long alarmTime = msg.getData().getLong(ALARM_TIME);
                    long threshold = BeiJingDate.getLong(0, 3, 0, 0);
                    if(cur >= alarmTime //时间到达
                            && cur < alarmTime + threshold //由于休眠导致时间过去太久了，或者alarmTime为0
                            ) {
                        playTipSound(alarmTime);
                    }
                    Message message = mHandler.obtainMessage(msg_update);
                    Bundle bundle = new Bundle();
                    bundle.putLong(ALARM_TIME, getAlarmTime(cur, date));
                    message.setData(bundle);
                    mHandler.removeMessages(msg_update);
                    mHandler.sendMessageDelayed(message, 1000);
                    break;
            }
        }
    }

    private long getAlarmTime(long cur, BeiJingDate date) {
        long start = date.getTime(20, 30, 0, 0); //20:30
        long end = date.getTime(22, 30, 0, 0); //22:30
        long interval = BeiJingDate.getLong(0, 30, 0, 0);
        long alarmTime = 0;
        for(long time=start;time<=end;time+=interval) {
            if(cur<time) {
                alarmTime = time;
                break;
            }
        }
        return alarmTime;
    }

    private void updateText(String time) {
        if(context == null) {
            return;
        }
        try {
            Log.i(TAG, "updateText "+time);
            // Construct the RemoteViews object
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
            views.setTextViewText(R.id.appwidget_text, time);
            ComponentName componentName = new ComponentName(context, NewAppWidget.class);
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(componentName, views);
        } catch (Exception e) {
            Log.e(TAG, "updateText fail "+e.getMessage());
        }
    }

    private void playTipSound(long time) {
        if(context == null) {
            return;
        }
        try {
            if(mediaPlayer != null) {
                mediaPlayer.release();
            }
            int resId = 0;
            BeiJingDate date = new BeiJingDate(time);
            long min = date.getMin();
            long hour = date.getHour();
            for(int[] item : soundId) {
                if(item[0] == hour && item[1] == min) {
                    resId = item[2];
                }
            }
            if(resId == 0) {
                Log.e(TAG, "nuknow resId "+resId);
                return;
            }
            Log.i(TAG, "playTipSound "+resId);
            boolean alarm = sharedPreferences.getBoolean(MainActivity.ALARM, false);
            if(alarm) {
                mediaPlayer = MediaPlayer.create(context, resId);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "playTipSound fail "+e.getMessage());
        }
    }
}

