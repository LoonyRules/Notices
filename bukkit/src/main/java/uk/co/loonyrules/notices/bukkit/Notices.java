package uk.co.loonyrules.notices.bukkit;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.loonyrules.notices.api.Notice;
import uk.co.loonyrules.notices.api.NoticeAPI;
import uk.co.loonyrules.notices.api.NoticePlayer;
import uk.co.loonyrules.notices.bukkit.utils.ChatUtil;
import uk.co.loonyrules.notices.core.Core;
import uk.co.loonyrules.notices.core.database.DatabaseEngine;

import java.io.File;
import java.util.Collection;

public class Notices extends JavaPlugin implements Core, Runnable, Listener
{

    private static NoticeAPI api;

    private DatabaseEngine databaseEngine;
    private String serverName;

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

        this.serverName = config.getString("general.server-name");
        this.databaseEngine = new DatabaseEngine(config.getString("mysql.host"), config.getInt("mysql.port"), config.getString("mysql.database"), config.getString("mysql.username"), config.getString("mysql.password"));

        api = new NoticeAPI(this);
        api.loadNotices();

        // Load from Database every 30 seconds because of expiration.
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this, 0L, 20 * 30L);

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable()
    {

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
    public void onPlayerJoinEvent(PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();

        DatabaseEngine.getPool().execute(() ->
        {
            NoticePlayer noticePlayer = api.cachePlayer(player.getUniqueId(), true);
            Collection<Notice> notices = api.getNotices(player.getUniqueId(), noticePlayer);

            if(notices.size() == 0)
                return;

            player.sendMessage(Core.DIVIDER);
            notices.forEach(notice -> notice.getMessages().forEach(message ->
            {
                TextComponent base = ChatUtil.textComponent("");
                TextComponent interact = notice.isDismissible() ? ChatUtil.runCommandHover(DISMISS, "§eClick to dismiss this notice.", "/notice dismiss " + notice.getId()) : ChatUtil.hover(DISMISS, "§eNotice cannot be dismissed.");
                TextComponent fm = ChatUtil.uri(ChatColor.translateAlternateColorCodes('&', message));

                base.addExtra(interact);
                base.addExtra(fm);

                player.spigot().sendMessage(base);
            }));
            player.sendMessage(Core.DIVIDER);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuitEvent(PlayerQuitEvent event)
    {
        final Player player = event.getPlayer();
        api.removePlayer(player.getUniqueId());
    }

}
