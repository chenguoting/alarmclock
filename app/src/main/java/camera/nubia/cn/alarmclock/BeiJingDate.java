package camera.nubia.cn.alarmclock;

public class BeiJingDate {
    private final static int SEC = 1000;
    private final static int MIN = 60*SEC;
    private final static int HOUR = 60*MIN;
    private final static int DAY = 60*HOUR;
    private final static int BEI_JING = 8*60*60*1000;

    private long date;
    private long beiJingDate;
    private int hour;
    private int min;
    private int sec;
    private int ms;
    private long day;
    public BeiJingDate(long date) {
        this.date = date;
        beiJingDate = date + BEI_JING;
        ms = (int) (beiJingDate % 1000);
        sec = (int) (beiJingDate / SEC % 60);
        min = (int) (beiJingDate / MIN % 60);
        hour = (int) (beiJingDate / HOUR % 24);
        day = beiJingDate / DAY;
    }

    public int getHour() {
        return hour;
    }

    public int getMin() {
        return min;
    }

    public long getTime(int hour, int min, int sec, int ms) {
        return day*DAY + hour*HOUR + min*MIN + sec*SEC + ms - BEI_JING;
    }

    public static long getLong(int hour, int min, int sec, int ms) {
        return hour*HOUR + min*MIN + sec*SEC + ms;
    }
}
