package camera.nubia.cn.alarmclock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    public final static String ALARM = "alarm";
    public final static String packageName = "camera.nubia.cn.alarmclock";
    SharedPreferences sharedPreferences;
    Button button;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences(packageName, Context.MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.state_text);
        button = findViewById(R.id.switch_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean alarm = sharedPreferences.getBoolean(ALARM, false);
                if(alarm) {
                    sharedPreferences.edit().putBoolean(ALARM, false).apply();
                }
                else {
                    sharedPreferences.edit().putBoolean(ALARM, true).apply();
                }
                update();
            }
        });
        update();
    }

    private void update() {
        boolean alarm = sharedPreferences.getBoolean(ALARM, false);
        if(alarm) {
            button.setText("关闭提醒");
            textView.setText("提醒已开启");
        }
        else {
            button.setText("开启提醒");
            textView.setText("提醒已关闭");
        }
    }
}
