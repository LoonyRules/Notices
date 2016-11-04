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
import uk.co.loonyrules.notices.bukkit.utils.ChatUtil;
import uk.co.loonyrules.notices.core.Core;
import uk.co.loonyrules.notices.core.database.DatabaseEngine;

import java.util.Date;
import java.util.NoSuchElementException;

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

            try {
                int id = Parse.toInt(args[1]);

                Notice notice = api.getNotice(id);
                NoticePlayer noticePlayer = api.getPlayer(player.getUniqueId());

                if(notice == null || noticePlayer == null || !api.getNotices().contains(notice) || (noticePlayer.getNotice(id) != null && noticePlayer.getNotice(id).hasDismissed()))
                {
                    player.playSound(player.getLocation(), Sound.ENTITY_RABBIT_DEATH, 0.5f, 1.0f);
                    return true;
                }

                MiniNotice miniNotice = noticePlayer.getNotice(id);

                if(miniNotice == null)
                {
                    player.sendMessage("§cYou cannot dismiss a notice that's not directed to you.");
                    return true;
                }

                miniNotice.setDismissed(true);

                DatabaseEngine.getPool().execute(() ->
                {
                    api.updatePlayer(miniNotice);
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.5f, 1.0f);
                    player.sendMessage("§aNotice dismissed.");
                });
            } catch(NoSuchElementException e) {
                sender.sendMessage(help[1]);
            }
        } else if(args[0].equalsIgnoreCase("create")) {
            if(!(sender instanceof Player))
            {
                sender.sendMessage("§cOnly players can execute that command.");
                return true;
            }

            Player player = (Player) sender;

            if(!(player.hasPermission(Permission.CMD_NOTICE_DELETE)))
            {
                player.sendMessage("§cYou don't have permission to execute this command.");
                return true;
            }

            Notice notice = api.getCreation(player.getUniqueId());

            if(notice != null)
            {
                player.sendMessage("§cYou're already creating a Notice, if you want to cancel this creation type CANCEL in the chat.");
                return true;
            }

            if(args.length != 4)
            {
                player.sendMessage(help[2]);
                return true;
            }

            String typeString = args[1];
            Notice.Type type = typeString.equalsIgnoreCase("all") ? Notice.Type.ALL : (typeString.startsWith("PERM:") ? Notice.Type.PERM : Notice.Type.INDIVIDUAL);

            Date expiration = Parse.getExpiryDate(args[2]);

            if(expiration == null)
            {
                player.sendMessage("§cExpiration date " + args[2] + " couldn't be parsed.");
                return true;
            }

            boolean dismissible = Parse.toBoolean(args[3]);

            notice = new Notice(player.getUniqueId(), type, (expiration.getTime() / 1000), dismissible);

            if(notice == null)
            {
                player.sendMessage("§cAn error occurred when initialising the Notice. Please try again later.");
                return true;
            }

            StringBuilder sb;
            if(notice.getType() == Notice.Type.INDIVIDUAL)
            {
                sb = new StringBuilder("§aPlayer recipients: §r");
                String[] players = typeString.split(":")[1].split(",");

                for(int i = 0; i < players.length; i++)
                {
                    OfflinePlayer offlinePlayer = Bukkit.getPlayer(players[i]);

                    if(offlinePlayer != null && offlinePlayer.hasPlayedBefore())
                    {
                        notice.addUUIDRecipient(offlinePlayer.getUniqueId());
                        sb.append("§a" + players[i] + ", ");
                    }
                }

                player.sendMessage(sb.toString().replaceAll(", $", ""));
            } else if(notice.getType() == Notice.Type.PERM) {
                String perm = typeString.split(":")[1];
                notice.setPerm(perm);

                player.sendMessage("§aAdded permission §e" + perm + " §ato notice.");
            }

            player.sendMessage("§aPlease type the notice message in chat. Type CANCEL to cancel the creation and SAVE to save.");
            api.addCreation(player.getUniqueId(), notice);
        } else if(args[0].equalsIgnoreCase("delete")) {
            if(!(sender.hasPermission(Permission.CMD_NOTICE_DELETE)))
            {
                sender.sendMessage("§cYou don't have permission to execute this command.");
                return true;
            }

            if(args.length == 1)
            {
                sender.sendMessage(help[3]);
                return true;
            }

            int id = Parse.toInt(args[1]);

            try {
                Notice notice = api.getNotice(id);

                if(notice == null)
                {
                    sender.sendMessage("§cNotice with the id §e" + id + " §cdoesn't exist or isn't active.");
                    return true;
                }

                DatabaseEngine.getPool().execute(() ->
                {
                    api.deleteNotice(notice);
                    sender.sendMessage("§cDeleted notice #" + notice.getId());
                });
            } catch(NoSuchElementException e) {
                sender.sendMessage("§cNotice #" + args[1] + " doesn't exist or isn't active.");
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
                sender.sendMessage("§aDisplaying all active notices...");
                api.getNotices().forEach(notice -> sender.sendMessage(" §7» §aNotice §e#" + notice.getId() + ", " + notice.getType().toString().toLowerCase() + "."));
                sender.sendMessage(Core.DIVIDER);
            } else {
                int id = Parse.toInt(args[1]);

                try {
                    ChatUtil.printNoticeInfo(sender, api.getNotice(id));
                } catch(NoSuchElementException e) {
                    sender.sendMessage("§cNotice with the id §e" + id + " §cdoesn't exist or isn't active.");
                }
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
