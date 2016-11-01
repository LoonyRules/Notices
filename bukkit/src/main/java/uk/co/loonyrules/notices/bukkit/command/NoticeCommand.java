package uk.co.loonyrules.notices.bukkit.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uk.co.loonyrules.notices.api.*;
import uk.co.loonyrules.notices.api.util.Parse;
import uk.co.loonyrules.notices.core.Core;
import uk.co.loonyrules.notices.core.database.DatabaseEngine;

import java.util.Date;

public class NoticeCommand implements CommandExecutor
{

    private final String[] help = new String[] {
        "§aDisplaying help for §e/notice §acommand.", " §7» §a/notice dismiss <id> §7- §eDismiss a notice.", " §7» §a/notice create <target> <expireIn> <isDismissible> §7- §eCreate a new notice.",
            " §7» §a/notice delete <id> §7- §eDelete a notice via id.", " §7» §a/notice info <id> §7- §eDisplay a specific notice's info."
    };

    private final NoticeAPI api;
    public NoticeCommand(NoticeAPI api)
    {
        this.api = api;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if(!(sender.hasPermission(Permission.CMD_NOTICE)))
        {
            sender.sendMessage("§cYou don't have permission to execute this command.");
            return true;
        }

        if(args.length == 0)
        {
            help(sender);
            return true;
        }

        if(args[0].equalsIgnoreCase("dismiss"))
        {
            if(!(sender instanceof Player))
            {
                sender.sendMessage("§cOnly players can execute that command.");
                return true;
            }

            Player player = (Player) sender;

            if(!(player.hasPermission(Permission.CMD_NOTICE_DISMISS)))
            {
                player.sendMessage("§cYou don't have permission to execute this command.");
                return true;
            }

            int id = Parse.toInt(args[1]);

            if(id< 0)
            {
                sender.sendMessage(help[1]);
                return true;
            }

            Notice notice = api.getNotice(id);
            NoticePlayer noticePlayer = api.getPlayer(player.getUniqueId());

            if(notice == null || noticePlayer == null || !api.getNotices().contains(notice) || (noticePlayer.getNotice(id) != null && noticePlayer.getNotice(id).hasDismissed()))
            {
                player.playSound(player.getLocation(), Sound.ENTITY_RABBIT_DEATH, 0.5f, 1.0f);
                return true;
            }

            MiniNotice miniNotice = noticePlayer.getNotice(id);
            miniNotice.setDismissed(true);

            DatabaseEngine.getPool().execute(() ->
            {
                api.updatePlayer(miniNotice);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.5f, 1.0f);
            });
        } else if(args[0].equalsIgnoreCase("create")) {
            if(!(sender.hasPermission(Permission.CMD_NOTICE_DELETE)))
            {
                sender.sendMessage("§cYou don't have permission to execute this command.");
                return true;
            }
        } else if(args[0].equalsIgnoreCase("delete")) {
            if(!(sender.hasPermission(Permission.CMD_NOTICE_DELETE)))
            {
                sender.sendMessage("§cYou don't have permission to execute this command.");
                return true;
            }
        } else if(args[0].equalsIgnoreCase("info")) {
            if(!(sender.hasPermission(Permission.CMD_NOTICE_INFO)))
            {
                sender.sendMessage("§cYou don't have permission to execute this command.");
                return true;
            }

            if(args.length == 1)
            {
                sender.sendMessage(Core.DIVIDER);
                api.getNotices().forEach(notice -> sender.sendMessage(" §7» §aNotice §e#" + notice.getId() + ", " + notice.getType().toString().toLowerCase() + "."));
                sender.sendMessage(Core.DIVIDER);
            } else {
                int id = Parse.toInt(args[1]);

                Notice notice = api.getNotice(id);

                if(notice == null)
                {
                    sender.sendMessage("§cNotice with the id §e" + id + " §cdoesn't exist or isn't active.");
                    return true;
                }

                sender.sendMessage(Core.DIVIDER);
                sender.sendMessage("§aDisplaying data for notice §e#" + notice.getId() + "§a.");
                sender.sendMessage(" §7» §6Type: §e" + notice.getType().toString().toLowerCase());

                OfflinePlayer op = Bukkit.getOfflinePlayer(notice.getCreator());
                sender.sendMessage(" §7» §6Creator: §e" + (op != null && op.hasPlayedBefore() && op.getName() != null ? op.getName() : notice.getCreator().toString()));
                sender.sendMessage(" §7» §6UUID recipients: §e" + (notice.getType() == Notice.Type.ALL ? "Anyone" : ""));

                if(notice.getType() == Notice.Type.INDIVIDUAL)
                    notice.getUUIDRecipients().forEach(uuid -> {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(notice.getCreator());
                        sender.sendMessage("   §7• §e" + (offlinePlayer != null && offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null ? offlinePlayer.getName() : notice.getCreator().toString()));
                    });

                sender.sendMessage(Core.DIVIDER);
            }
        } else help(sender);

        return true;
    }

    public void help(CommandSender sender)
    {
        sender.sendMessage(Core.DIVIDER);
        sender.sendMessage(help[0]);

        if(sender.hasPermission(Permission.CMD_NOTICE_DISMISS))
            sender.sendMessage(help[1]);
        if(sender.hasPermission(Permission.CMD_NOTICE_CREATE))
            sender.sendMessage(help[2]);
        if(sender.hasPermission(Permission.CMD_NOTICE_DELETE))
            sender.sendMessage(help[3]);
        if(sender.hasPermission(Permission.CMD_NOTICE_INFO))
            sender.sendMessage(help[4]);

        sender.sendMessage(Core.DIVIDER);
    }

}
