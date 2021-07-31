package cn.hephaestus.smartmeetingroom.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author yinhaiming
 * @date 2021/04/08 15:24
 * Description: 日期工具
 */
public class DateUtil {
    /**常量:datetime格式*/
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**常量:date格式*/
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public static String getNowDate(){
        return  new SimpleDateFormat(DATE_FORMAT).format(new Date());
    }
    public static String getNowDateTime(){
        return  new SimpleDateFormat(DATETIME_FORMAT).format(new Date());
    }

    /**
     * @Description: 获取相对时间
     * @param field 为跨度
     * @param amount 为跨多少
     */
    public static String getRelativeDate(int field, int amount){
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(field,amount);
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        return dateFormat.format(calendar.getTime());
    }

    public static String getRelativeDateTime(int field, int amount){
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(field,amount);
        DateFormat dateFormat = new SimpleDateFormat(DATETIME_FORMAT);
        return dateFormat.format(calendar.getTime());
    }

}
