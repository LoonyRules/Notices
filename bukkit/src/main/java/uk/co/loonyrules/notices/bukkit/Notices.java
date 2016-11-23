package uk.co.loonyrules.notices.bukkit;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.loonyrules.notices.api.MiniNotice;
import uk.co.loonyrules.notices.api.Notice;
import uk.co.loonyrules.notices.api.NoticeAPI;
import uk.co.loonyrules.notices.api.NoticePlayer;
import uk.co.loonyrules.notices.api.listeners.EventListener;
import uk.co.loonyrules.notices.bukkit.command.NoticeCommand;
import uk.co.loonyrules.notices.bukkit.listener.PermListener;
import uk.co.loonyrules.notices.bukkit.utils.ChatUtil;
import uk.co.loonyrules.notices.core.Core;
import uk.co.loonyrules.notices.core.database.DatabaseEngine;

import java.io.File;
import java.util.Collection;

public class Notices extends JavaPlugin implements Core, Runnable, Listener
{

    private static NoticeAPI api;

    private PermListener permListener;
    private DatabaseEngine databaseEngine;

    @Override
    public void onEnable()
    {
        File dir = getDataFolder();
        if(!dir.exists())
            dir.mkdir();

        File file = new File(dir, "config.yml");
        if(!file.exists())
            saveDefaultConfig();

        FileConfiguration config = getConfig();

        this.databaseEngine = new DatabaseEngine(config.getString("mysql.host"), config.getInt("mysql.port"), config.getString("mysql.database"), config.getString("mysql.username"), config.getString("mysql.password"));

        api = new NoticeAPI(this);
        api.loadNotices();
        api.addEventListener(permListener = new PermListener());

        // Load from Database every 30 seconds because of expiration.
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this, 0L, 20 * 30L);

        getCommand("notice").setExecutor(new NoticeCommand(api));
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable()
    {
        api.removeEventListener(permListener);
    }

    @Override
    public DatabaseEngine getDatabaseEngine()
    {
        return databaseEngine;
    }

    public static NoticeAPI getAPI()
    {
        return api;
    }

    @Override
    public void run()
    {
        api.loadNotices();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent event)
    {
        final Player player = event.getPlayer();

        Notice notice = api.getCreation(player.getUniqueId());

        if(notice == null)
            return;

        event.setCancelled(true);

        if(event.getMessage().toLowerCase().equals("cancel"))
        {
            api.removeCreation(player.getUniqueId());
            player.sendMessage("§aNotice creation has been cancelled. You can type normally now.");
            return;
        } else if(event.getMessage().toLowerCase().equals("save")) {
            DatabaseEngine.getPool().execute(() ->
            {
                Notice updated = api.saveNotice(notice);

                player.sendMessage("§aSaved notice to the database with the following data:");
                ChatUtil.printNoticeInfo(player, updated);
            });

            api.removeCreation(player.getUniqueId());
            return;
        }

        notice.addMessage(event.getMessage());
        player.sendMessage("§aAdded the following message to your notice creation.");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', event.getMessage()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinEvent(PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();

        DatabaseEngine.getPool().execute(() ->
        {
            NoticePlayer noticePlayer = api.cachePlayer(player.getUniqueId(), true);
            Collection<Notice> notices = api.getNotices(player.getUniqueId(), noticePlayer);

            if(notices.size() == 0)
                return;

            notices.forEach(notice ->
            {
                player.sendMessage(Core.DIVIDER);

                notice.getMessages().forEach(message ->
                {
                    TextComponent base = ChatUtil.textComponent("");
                    TextComponent interact = notice.isDismissible() ? ChatUtil.runCommandHover(DISMISS, "§eClick to dismiss this notice.", "/notice dismiss " + notice.getId()) : ChatUtil.hover(DISMISS, "§eNotice cannot be dismissed.");
                    TextComponent fm = ChatUtil.uri(ChatColor.translateAlternateColorCodes('&', message));

                    base.addExtra(interact);
                    base.addExtra(fm);

                    player.spigot().sendMessage(base);
                });

                notice.addView();
                api.saveNotice(notice);

                MiniNotice miniNotice = noticePlayer.getNotice(notice.getId());

                if(miniNotice == null)
                {
                    miniNotice = new MiniNotice(notice.getId(), player.getUniqueId(), true, false);
                    noticePlayer.addNotice(miniNotice);
                    api.updatePlayer(miniNotice);
                }

                player.sendMessage(Core.DIVIDER);
            });

        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuitEvent(PlayerQuitEvent event)
    {
        final Player player = event.getPlayer();
        api.removePlayer(player.getUniqueId());
    }

}
