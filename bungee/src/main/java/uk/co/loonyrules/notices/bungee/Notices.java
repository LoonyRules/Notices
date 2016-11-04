package uk.co.loonyrules.notices.bungee;

import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import uk.co.loonyrules.notices.api.MiniNotice;
import uk.co.loonyrules.notices.api.Notice;
import uk.co.loonyrules.notices.api.NoticeAPI;
import uk.co.loonyrules.notices.api.NoticePlayer;
import uk.co.loonyrules.notices.bungee.util.ChatUtil;
import uk.co.loonyrules.notices.core.Core;
import uk.co.loonyrules.notices.core.database.DatabaseEngine;

import java.io.*;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class Notices extends Plugin implements Core, Runnable
{

    private static NoticeAPI api;

    private DatabaseEngine databaseEngine;
    private String serverName = "proxy";

    @Override
    public void onEnable()
    {
        try {
            File dir = getDataFolder();
            if(!dir.exists())
                dir.mkdir();

            File file = new File(dir, "config.yml");
            if(!file.exists())
            {
                file.createNewFile();
                InputStream is = getResourceAsStream("config.yml");
                OutputStream os = new FileOutputStream(file);

                ByteStreams.copy(is, os);
            }

            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

            this.serverName = config.getString("general.server-name");
            this.databaseEngine = new DatabaseEngine(config.getString("mysql.host"), config.getInt("mysql.port"), config.getString("mysql.database"), config.getString("mysql.username"), config.getString("mysql.password"));

            api = new NoticeAPI(this);
            api.loadNotices();

            // Load from Database every 30 seconds because of expiration.
            getProxy().getScheduler().schedule(this, this, 1L, 30L, TimeUnit.SECONDS);
        } catch(IOException e) {
            e.printStackTrace();
        }
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

    @EventHandler
    public void onServerConnectedEvent(ServerConnectedEvent event)
    {
        final ProxiedPlayer player = event.getPlayer();

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

                    player.sendMessage(base);
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

}
