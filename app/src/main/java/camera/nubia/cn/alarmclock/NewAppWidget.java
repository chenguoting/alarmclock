package camera.nubia.cn.alarmclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * Implementation of App Widget functionality.
 */
public class NewAppWidget extends AppWidgetProvider {
    private final static String TAG = "NewAppWidget";
    AppWidgetManager appWidgetManager;
    Context context;
    boolean isEnabled = false;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        //views.setTextViewText(R.id.appwidget_text, widgetText);
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
        init();
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        Log.i(TAG, "onEnabled");
        init();

    }

    private void init() {
        if(isEnabled) {
            return;
        }
        isEnabled = true;
        mHandlerThread = new HandlerThread("WidgetHandlerThread");
        mHandlerThread.start();
        mHandler = new WidgetHandler(mHandlerThread.getLooper());
        mHandler.sendEmptyMessage(msg_play_2030);
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
            R.raw.tesy, //8:30
            R.raw.tesy, //9:00
            R.raw.tesy, //9:30
            R.raw.tesy, //10:00
    };
    int sound_830 = 0;

    final static int msg_play_2030 = 0;
    HandlerThread mHandlerThread;
    WidgetHandler mHandler;
    MediaPlayer mediaPlayer;
    class WidgetHandler extends Handler {
        public WidgetHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case msg_play_2030:
                    Log.i(TAG, "msg_play_2030");
                    long cur = System.currentTimeMillis();
                    long min = cur / 1000 / 60;
                    long hour = min / 60 + 8; //北京时间所以加8
                    updateText((hour%24)+":"+(min%60));
                    long alarmTime = msg.getData().getLong("alarm_time");
                    if(cur >= alarmTime && alarmTime > 0) {
                        playTipSound(alarmTime);
                    }
                    long next = (min + 1) * 60 * 1000;
                    mHandler.sendEmptyMessageAtTime(msg_play_2030, cur+60*1000);
                    break;
            }
        }
    }

    private void updateText(String time) {
        if(context == null) {
            return;
        }
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
        try {
            if(mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(context.getResources().openRawResourceFd(R.raw.tesy).getFileDescriptor());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "play fail "+e.getMessage());
        }
    }
}

