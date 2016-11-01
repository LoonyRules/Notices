package uk.co.loonyrules.notices.api.util;

public class Parse
{

    public static boolean isInt(String string)
    {
        try {
            int i = Integer.parseInt(string);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    public static int toInt(String string)
    {
        return isInt(string) ? Integer.parseInt(string) : -1;
    }

    public static String dateDiff(long timestamp, long needle)
    {
        String ret = "";

        long delta = Math.abs(timestamp - needle) / 1000;

        double days = Math.floor(delta / 86400); // 86400
        if(days > 0) ret += days + "d ";
        delta -= days * 86400;

        double hours = Math.floor(delta / 3600) % 24;
        if(hours > 0) ret += hours + "h ";
        delta -= hours * 3600;

        double minutes = Math.floor(delta / 60) % 60;
        if(minutes > 0) ret += minutes + "m ";
        delta -= minutes * 60;

        double seconds = delta % 60;
        ret += Math.floor(seconds) + "s";

        return ret;
    }

}
