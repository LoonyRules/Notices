package uk.co.loonyrules.notices.api.util;

import java.util.Date;

public class Parse
{

    public static boolean isInt(String string)
    {
        try {
            Integer.parseInt(string);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    public static boolean toBoolean(String string)
    {
        try
        {
            return Boolean.parseBoolean(string);
        } catch(Exception e)
        {
            return false;
        }
    }

    public static int toInt(String string)
    {
        return isInt(string) ? Integer.parseInt(string) : -1;
    }

    public static Date getExpiryDate(String arg)
    {
        long number = Integer.parseInt(arg.replaceAll("[^0-9]", ""));
        if(arg.endsWith("m"))
            number *= 60;
        else if(arg.endsWith("h"))
            number *= 60 * 60;
        else if(arg.endsWith("d"))
            number *= 60 * 60 * 24;
        else if(arg.endsWith("w"))
            number *= 60 * 60 * 24 * 7;
        else if(arg.endsWith("M"))
            number *= 60 * 60 * 24 * 30;
        else if(arg.endsWith("y"))
            number *= 60 * 60 * 24 * 365;

        number = Math.min(number, 60 * 60 * 24 * 365 * 2);
        Date date = new Date();
        date.setTime(date.getTime() + number * 1000);
        return date;
    }

    public static String dateDiff(long timestamp, long needle)
    {
        String ret = "";

        long delta = Math.abs(timestamp - needle) / 1000;

        int days = (int) Math.floor(delta / 86400); // 86400
        if(days > 0) ret += days + "d ";
        delta -= days * 86400;

        int hours = (int) Math.floor(delta / 3600) % 24;
        if(hours > 0) ret += hours + "h ";
        delta -= hours * 3600;

        int minutes = (int) Math.floor(delta / 60) % 60;
        if(minutes > 0) ret += minutes + "m ";
        delta -= minutes * 60;

        int seconds = (int) delta % 60;
        ret += Math.floor(seconds) + "s";

        return ret;
    }

}
