package no.nordicsemi.android.nrftoolbox.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 时间类
 *
 * @author Administrator
 */
public class MyTime {


    public static String geTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss");
        Date date = new Date();
        return format.format(date);
    }


    public static String getMeasureTimeHourMin() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        return format.format(date);
    }

    public static String getTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        return format.format(date);
    }





    // 获取当前日期的前一天
    public static String getBeforeDay(int number) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, number);
        Date date = c.getTime();
        return format.format(date);
    }


    public static long getDaysBetween(String date1, String date2) {

        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
        Long c = null;
        try {
            c = sf.parse(date2).getTime() - sf.parse(date1).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long d = c / 1000 / 60 / 60 / 24;//天

        return d;
    }

    public static String getMyData(String time) {
        String result = "";
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = format.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        SimpleDateFormat format2 = new SimpleDateFormat("HH:mm:ss");
        result = format2.format(date);

        return result;

    }


    // 获取当前日期的前一天
    public static boolean getIsOldTime(String time) {

        if (time == null && time.equals("")) {
            return true;
        }

        boolean result = true;

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String today = format.format(date);

        String[] today_st = today.split("-");
        String[] time_str = time.split("-");

        if (today_st != null && today_st.length == 3 && time_str != null && time_str.length == 3) {

            if (Integer.valueOf(today_st[0]) < Integer.valueOf(time_str[0])) {
                result = false;
            } else if (Integer.valueOf(today_st[0]) > Integer.valueOf(time_str[0])) {

            } else {

                if (Integer.valueOf(today_st[1]) < Integer.valueOf(time_str[1])) {
                    result = false;
                } else if (Integer.valueOf(today_st[1]) > Integer.valueOf(time_str[1])) {
                } else {


                    if (Integer.valueOf(today_st[2]) < Integer.valueOf(time_str[2])) {
                        result = false;
                    } else if (Integer.valueOf(today_st[2]) > Integer.valueOf(time_str[2])) {

                    } else {
                        result = false;
                    }
                }
            }

        }


        return result;
    }



}
