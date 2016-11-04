package uk.co.loonyrules.notices.bukkit.utils;

import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import uk.co.loonyrules.notices.api.Notice;
import uk.co.loonyrules.notices.api.util.Parse;
import uk.co.loonyrules.notices.core.Core;

import java.util.Date;
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

    public static void printNoticeInfo(CommandSender sender, Notice notice)
    {
        sender.sendMessage(Core.DIVIDER);
        sender.sendMessage("§aDisplaying data for notice §e#" + notice.getId() + "§a.");
        sender.sendMessage(" §7» §6Type: §e" + notice.getType().toString().toLowerCase());

        OfflinePlayer op = Bukkit.getOfflinePlayer(notice.getCreator());
        sender.sendMessage(" §7» §6Creator: §e" + (op != null && op.hasPlayedBefore() && op.getName() != null ? op.getName() : notice.getCreator().toString()));
        sender.sendMessage(" §7» §6Expiration: §e" + new Date((notice.getExpiration() * 1000)).toLocaleString() + " (" + Parse.dateDiff(System.currentTimeMillis(), (notice.getExpiration() * 1000)) + ")");

        if(notice.getType() == Notice.Type.INDIVIDUAL)
        {
            sender.sendMessage(" §7» §6UUID recipients: §e" + (notice.getType() == Notice.Type.ALL ? "Anyone" : ""));

            if(notice.getType() == Notice.Type.INDIVIDUAL)
                notice.getUUIDRecipients().forEach(uuid -> {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(notice.getCreator());
                    sender.sendMessage("   §7• §e" + (offlinePlayer != null && offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null ? offlinePlayer.getName() : notice.getCreator().toString()));
                });

        } else if(notice.getType() == Notice.Type.PERM)
            sender.sendMessage(" §7» §6Permission: §e" + notice.getPerm());

        sender.sendMessage(Core.DIVIDER);
    }
}
