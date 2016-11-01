package uk.co.loonyrules.notices.bukkit.utils;

import net.md_5.bungee.api.chat.*;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtil
{

    public static BaseComponent[] baseComponent(String text)
    {
        return new ComponentBuilder(text).create();
    }

    public static TextComponent textComponent(String text)
    {
        return new TextComponent(text);
    }

    public static TextComponent runCommandHover(String text, String hoverText, String command)
    {
        return clickHover(text, hoverText, command, ClickEvent.Action.RUN_COMMAND, HoverEvent.Action.SHOW_TEXT);
    }

    public static TextComponent clickHover(String text, String hoverText, String command, ClickEvent.Action action, HoverEvent.Action hover)
    {
        TextComponent tc = textComponent(text);
        tc.setClickEvent(new ClickEvent(action, command));
        tc.setHoverEvent(new HoverEvent(hover, baseComponent(hoverText)));
        return tc;
    }

    public static TextComponent hover(String text, String hover)
    {
        TextComponent tc = textComponent(text);
        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, baseComponent(hover)));
        return tc;
    }

    public static TextComponent uri(String text)
    {
        if(!text.contains("[url=\""))
            return textComponent(text);

        String uri = "";

        Pattern p = Pattern.compile("\"([^\"]*)\"");
        Matcher m = p.matcher(text);

        while (m.find())
            uri = m.group(1);

        text = text.replaceAll("\\[url=\"", "").replaceAll("\"]", "");

        String[] split = text.split(uri);
        String beginning = split[0];
        String end = split.length >= 2 ? split[1] : "";

        TextComponent base = textComponent(beginning);
        TextComponent url = clickHover(ChatColor.AQUA + uri, "§eClick to visit link.", (uri.toLowerCase().startsWith("http://") || uri.toLowerCase().startsWith("https://") ? "" : "http://") + uri, ClickEvent.Action.OPEN_URL, HoverEvent.Action.SHOW_TEXT);
        TextComponent endtc = textComponent(end);

        base.addExtra(url);
        base.addExtra(endtc);

        return base;
    }
}
