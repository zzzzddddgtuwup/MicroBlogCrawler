package m.sina;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zzzzddddgtuwup on 2/25/15.
 */
public class Tools {
    public static boolean timeBefore(String s1, String s2) {
        Date t1 = strToDate(s1);
        Date t2 = strToDate(s2);
        return t1.before(t2);
    }


    public static String dateToStr(Date date) {
        DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format1.format(date);
    }

    public static Date strToDate(String s) {
        DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date result = null;
        try {
            result = format1.parse(s);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Date convertCalendar(String time) {
        TimeZone timezone = TimeZone.getTimeZone("GMT+8"); //设置为东八区
        TimeZone.setDefault(timezone);// 设置时区

        Calendar date = Calendar.getInstance();
//        System.out.println(date.getTime().toString());
        if (time.contains("秒前")) {
            int sec = getNum(time);
            date.set(Calendar.SECOND, date.get(Calendar.SECOND) - sec);
        } else if (time.contains("分钟前")) {
            int min = getNum(time);
            date.set(Calendar.MINUTE, date.get(Calendar.MINUTE) - min);
        } else if (time.contains("今天")) {
            int hour = getNum(time.substring(time.indexOf(' ') + 1));
            int min = getNum(time.substring(time.indexOf(':') + 1));
            date.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DATE), hour, min);
        } else if (time.contains("月") && time.contains("日")) {
            int month = getNum(time);
            int day = getNum(time.substring(time.indexOf('月') + 1));
            int hour = getNum(time.substring(time.indexOf(' ') + 1));
            int min = getNum(time.substring(time.indexOf(':') + 1));
            date.set(date.get(Calendar.YEAR), month - 1, day, hour, min);
        } else if (time.indexOf('-') == 2) {
            int month = getNum(time);
            int day = getNum(time.substring(time.indexOf('-') + 1));
            int hour = getNum(time.substring(time.indexOf(' ') + 1));
            int min = getNum(time.substring(time.indexOf(':') + 1));
            date.set(date.get(Calendar.YEAR), month - 1, day, hour, min);
        } else {
            DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date result = null;
            try {
                result = format1.parse(time);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return result;
        }
        return date.getTime();
    }

    private static int getNum(String num) {
        String s = "";
        for (int i = 0; i < num.length() && num.charAt(i) <= '9' && num.charAt((i)) >= '0'; i++) {
            s += num.charAt(i);
        }
        return Integer.parseInt(s);
    }

    public static List<String> getTimeSeries(String startTime, String endTime) throws ParseException {
        List<String> result = new ArrayList<String>();
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH");
        startDate.setTime(sdf.parse(startTime));
        endDate.setTime(sdf.parse(endTime));

        while (!startDate.after(endDate)) {
            result.add(sdf.format(startDate.getTime()));
            startDate.add(Calendar.HOUR_OF_DAY, 1);
        }
        return result;
    }

    public static List<String> deleteDuplicate(String s) {
        Matcher m = Pattern.compile("//@(.*?)[:|：]").matcher(s);
        List<String> result = new ArrayList<>();
        String prev = "";
        while (m.find()) {
            String cur = m.group();
            cur = cur.substring(3,cur.length()-1);
            if(!cur.equals(prev)){
                if(!prev.equals("")){
                    result.add(prev);
                }
                prev = cur;
            }
        }
        if(!prev.equals(""))
            result.add(prev);
        return result;
    }

    public static void main(String[] args) {
        //List<String> result = deleteDuplicate("@平安北京 今天下午发通报，称昨日查获三名涉毒人员。此前已有媒体报道，演员王学兵和张博因吸毒被拘。此外，通报还显示有一名女性侯某同时被抓获。#王学兵张博吸毒被拘#");
        //System.out.println(result);
        String time = "今天 06:09";
        System.out.println(dateToStr(Tools.convertCalendar(time)));
    }
}
