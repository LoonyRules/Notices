package uk.co.loonyrules.notices.bungee.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import uk.co.loonyrules.notices.api.*;
import uk.co.loonyrules.notices.api.util.Parse;
import uk.co.loonyrules.notices.bungee.Notices;
import uk.co.loonyrules.notices.bungee.OfflinePlayer;
import uk.co.loonyrules.notices.bungee.util.ChatUtil;
import uk.co.loonyrules.notices.core.Core;
import uk.co.loonyrules.notices.core.database.DatabaseEngine;

import java.util.Date;
import java.util.NoSuchElementException;

public class NoticeCommand extends Command
{

    private final String[] help = new String[] {
            "§aDisplaying help for §e/notice §acommand.", " §7» §a/notice dismiss <id> §7- §eDismiss a notice.", " §7» §a/notice create <target> <expireIn> <isDismissible> §7- §eCreate a new notice.",
            " §7» §a/notice delete <id> §7- §eDelete a notice via id.", " §7» §a/notice info <id> §7- §eDisplay a specific notice's info."
    };

    private NoticeAPI api;

    public NoticeCommand(NoticeAPI api, String name)
    {
        super(name);
        this.api = api;
    }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        if(!(sender.hasPermission(Permission.CMD_NOTICE)))
        {
            sender.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission to execute this command."));
            return;
        }

        if(args.length == 0)
        {
            help(sender);
            return;
        }

        if(args[0].equalsIgnoreCase("dismiss"))
        {
            if(!(sender instanceof ProxiedPlayer))
            {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Only players can execute that command."));
                return;
            }

            ProxiedPlayer player = (ProxiedPlayer) sender;

            if(!(player.hasPermission(Permission.CMD_NOTICE_DISMISS)))
            {
                player.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission to execute this command."));
                return;
            }

            if(args.length != 2)
                player.sendMessage(new TextComponent(help[1]));
            else {
                try {
                    int id = Parse.toInt(args[1]);

                    Notice notice = api.getNotice(id);
                    NoticePlayer noticePlayer = api.getPlayer(player.getUniqueId());

                    if(notice == null || noticePlayer == null || !api.getNotices().contains(notice) || (noticePlayer.getNotice(id) != null && noticePlayer.getNotice(id).hasDismissed()))
                    {
                        player.sendMessage(new TextComponent(ChatColor.RED + "A notice with that id doesn't exist."));
                        return;
                    }

                    MiniNotice miniNotice = noticePlayer.getNotice(id);

                    if(miniNotice == null)
                    {
                        player.sendMessage("§cYou cannot dismiss a notice that's not directed to you.");
                        return;
                    }

                    miniNotice.setDismissed(true);

                    DatabaseEngine.getPool().execute(() ->
                    {
                        api.updatePlayer(miniNotice);
                        player.sendMessage(new TextComponent(ChatColor.GREEN + "Notice dismissed."));
                    });
                } catch(NoSuchElementException e) {
                    sender.sendMessage(help[1]);
                }
            }
        } else if(args[0].equalsIgnoreCase("create")) {
            if(!(sender instanceof ProxiedPlayer))
            {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Only players can execute that command."));
                return;
            }

            ProxiedPlayer player = (ProxiedPlayer) sender;

            if(!(player.hasPermission(Permission.CMD_NOTICE_CREATE)))
            {
                player.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission to execute this command."));
                return;
            }

            Notice notice = api.getCreation(player.getUniqueId());

            if(notice != null)
            {
                player.sendMessage(new TextComponent(ChatColor.RED + "You're already creating a Notice, if you want to cancel this creation type CANCEL in the chat."));
                return;
            }

            if(args.length != 4)
            {
                player.sendMessage(new TextComponent(help[2]));
                return;
            }

            String typeString = args[1];
            Notice.Type type = typeString.equalsIgnoreCase("all") ? Notice.Type.ALL : (typeString.toLowerCase().startsWith("perm:") ? Notice.Type.PERM : (typeString.toLowerCase().startsWith("server:") ? Notice.Type.SERVER : Notice.Type.INDIVIDUAL));

            Date expiration = Parse.getExpiryDate(args[2]);

            if(expiration == null)
            {
                player.sendMessage(new TextComponent(ChatColor.RED + "Expiration date " + args[2] + " couldn't be parsed."));
                return;
            }

            boolean dismissible = Parse.toBoolean(args[3]);

            notice = new Notice(player.getUniqueId(), type, (expiration.getTime() / 1000), dismissible);

            if(notice == null)
            {
                player.sendMessage(new TextComponent(ChatColor.RED + "An error occurred when initialising the Notice. Please try again later."));
                return;
            }

            StringBuilder sb;
            if(notice.getType() == Notice.Type.INDIVIDUAL)
            {
                sb = new StringBuilder(ChatColor.GRAY + "Player recipients: " + ChatColor.RESET);
                String[] players = typeString.split(":")[1].split(",");

                for(int i = 0; i < players.length; i++)
                {
                    OfflinePlayer offlinePlayer = Notices.getInstance().getOfflinePlayer(players[i]);

                    if(offlinePlayer != null)
                    {
                        notice.addUUIDRecipient(offlinePlayer.getUUID());
                        sb.append(ChatColor.GREEN + players[i] + ", ");
                    }
                }

                TextComponent tc = new TextComponent(sb.toString().replaceAll(", $", ""));

                if(notice.getUUIDRecipients().size() != players.length)
                     tc.addExtra(new TextComponent(". Couldn't add " + (players.length - notice.getUUIDRecipients().size()) + " player(s), have they joined before?"));

                player.sendMessage(tc);
            } else if(notice.getType() == Notice.Type.PERM) {
                String perm = typeString.split(":")[1];
                notice.setPerm(perm);

                player.sendMessage(new TextComponent(ChatColor.GREEN + "Added permission " + ChatColor.YELLOW + perm + ChatColor.GREEN + " to notice."));
            } else if(notice.getType() == Notice.Type.SERVER) {
                sb = new StringBuilder(ChatColor.GRAY + "Server recipients: " + ChatColor.RESET);
                String[] servers = typeString.split(":")[1].split(",");

                for(int i = 0; i < servers.length; i++)
                {
                    ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(servers[i]);

                    if(serverInfo != null)
                    {
                        notice.addServer(serverInfo.getName());
                        sb.append(ChatColor.GREEN + servers[i] + ", ");
                    }
                }

                TextComponent tc = new TextComponent(sb.toString().replaceAll(", $", ""));

                if(notice.getServers().size() != servers.length)
                    tc.addExtra(new TextComponent(". Couldn't add " + (servers.length - notice.getUUIDRecipients().size()) + " servers(s), do they exists?"));

                player.sendMessage(tc);
            }

            player.sendMessage(ChatColor.GREEN + "Please type the notice message in chat. Type CANCEL to cancel the creation and SAVE to save.");
            api.addCreation(player.getUniqueId(), notice);
        } else if(args[0].equalsIgnoreCase("delete")) {
            if(!(sender.hasPermission(Permission.CMD_NOTICE_DELETE)))
            {
                sender.sendMessage("§cYou don't have permission to execute this command.");
                return;
            }

            if(args.length == 1)
            {
                sender.sendMessage(help[3]);
                return;
            }

            int id = Parse.toInt(args[1]);

            try {
                Notice notice = api.getNotice(id);

                if(notice == null)
                {
                    sender.sendMessage("§cNotice with the id §e" + id + " §cdoesn't exist or isn't active.");
                    return;
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
                return;
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
