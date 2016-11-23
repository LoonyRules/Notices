package uk.co.loonyrules.notices.bungee;

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import io.netty.util.internal.ConcurrentSet;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import uk.co.loonyrules.notices.api.MiniNotice;
import uk.co.loonyrules.notices.api.Notice;
import uk.co.loonyrules.notices.api.NoticeAPI;
import uk.co.loonyrules.notices.api.NoticePlayer;
import uk.co.loonyrules.notices.bungee.command.NoticeCommand;
import uk.co.loonyrules.notices.bungee.listener.PermListener;
import uk.co.loonyrules.notices.bungee.util.ChatUtil;
import uk.co.loonyrules.notices.core.Core;
import uk.co.loonyrules.notices.core.database.DatabaseEngine;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Notices extends Plugin implements Core, Runnable, Listener
{

    private static NoticeAPI api;
    private static Notices instance;

    private PermListener permListener;
    private DatabaseEngine databaseEngine;

    @Override
    public void onEnable()
    {
        instance = this;

        Configuration config = getConfig();

        if(config == null)
            throw new NullPointerException("Couldn't load the config.yml file, is it there?");

        this.databaseEngine = new DatabaseEngine(config.getString("mysql.host"), config.getInt("mysql.port"), config.getString("mysql.database"), config.getString("mysql.username"), config.getString("mysql.password"));

        api = new NoticeAPI(this);
        api.loadNotices();
        api.addEventListener(permListener = new PermListener());

        // Load from Database every 30 seconds because of expiration.
        getProxy().getScheduler().schedule(this, this, 1L, 30L, TimeUnit.SECONDS);

        final PluginManager pluginManager = getProxy().getPluginManager();

        pluginManager.registerCommand(this, new NoticeCommand(api, "notice"));
        pluginManager.registerListener(this, this);
    }

    private File configFile;
    public Configuration getConfig()
    {
        try {
            File dir = getDataFolder();
            if(!dir.exists())
                dir.mkdir();

            configFile = new File(dir, "config.yml");
            if(!configFile.exists())
            {
                configFile.createNewFile();
                InputStream is = getResourceAsStream(configFile.getName());
                OutputStream os = new FileOutputStream(configFile);

                ByteStreams.copy(is, os);
            }

            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch(IOException e) {
            return null;
        }
    }

    @Override
    public void onDisable()
    {
        api.removeEventListener(permListener);
        getProxy().getPluginManager().unregisterListener(this);
    }

    @Override
    public DatabaseEngine getDatabaseEngine()
    {
        return databaseEngine;
    }

    public static Notices getInstance()
    {
        return instance;
    }

    public OfflinePlayer getOfflinePlayer(UUID uuid)
    {
        Configuration config = getConfig();

        if(config == null)
            throw null;

        Set<OfflinePlayer> result = Sets.newHashSet();

        config.getStringList("storage").forEach(entry ->
        {
            String[] split = entry.split(":");
            UUID uuidFromConf = UUID.fromString(split[0]);
            String name = split[1];

            result.add(new OfflinePlayer(uuidFromConf, name));
        });

        if(result.size() == 0 || !result.iterator().hasNext())
            return null;
        else return result.iterator().next();
    }

    public OfflinePlayer getOfflinePlayer(String lastName)
    {
        Configuration config = getConfig();

        if(config == null)
            throw null;

        Set<OfflinePlayer> result = Sets.newHashSet();

        config.getStringList("storage").forEach(entry ->
        {
            String[] split = entry.split(":");
            UUID uuidFromConf = UUID.fromString(split[0]);
            String name = split[1];

            if(name.equals(lastName))
                result.add(new OfflinePlayer(uuidFromConf, name));
        });

        if(result.size() == 0 || !result.iterator().hasNext())
            return null;
        else return result.iterator().next();
    }

    private void updateOfflinePlayer(OfflinePlayer offlinePlayer)
    {
        Configuration config = getConfig();

        if(config == null)
            return;

        List<String> entries = config.getStringList("storage");

        synchronized (entries)
        {
            Iterator<String> it = entries.iterator();

            while(it.hasNext())
            {
                String entry = it.next();
                String[] split = entry.split(":");

                if(split[0].equals(offlinePlayer.getUUID().toString()))
                    it.remove();
            }

            entries.add(offlinePlayer.getUUID().toString() + ":" + offlinePlayer.getLastName());
            config.set("storage", entries);

            try
            {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
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

        OfflinePlayer offlinePlayer = getOfflinePlayer(player.getUniqueId());

        if(offlinePlayer == null)
            offlinePlayer = new OfflinePlayer(player.getUniqueId(), player.getName());

        if(!offlinePlayer.getLastName().equals(player.getName()))
            offlinePlayer.setLastName(player.getName());

        updateOfflinePlayer(offlinePlayer);

        DatabaseEngine.getPool().execute(() ->
        {
            NoticePlayer noticePlayer = api.cachePlayer(player.getUniqueId(), true);
            Collection<Notice> notices = api.getNotices(player.getUniqueId(), noticePlayer);

            // Get per-server notices
            List<Integer> allIDs = new ArrayList<>();
            notices.forEach(notice -> allIDs.add(notice.getId()));

            api.getNotices().stream().filter(notice -> !allIDs.contains(notice.getId()) && notice.getType() == Notice.Type.SERVER && notice.getServers().contains(event.getServer().getInfo().getName())).forEach(notice ->
            {
                if(noticePlayer.getNotice(notice.getId()) == null || !noticePlayer.getNotice(notice.getId()).hasDismissed())
                    notices.add(notice);
            });
            //

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

    @EventHandler
    public void onChatEvent(ChatEvent event)
    {
        if(!(event.getSender() instanceof ProxiedPlayer))
            return;

        final ProxiedPlayer sender = (ProxiedPlayer) event.getSender();

        Notice notice = api.getCreation(sender.getUniqueId());

        if(notice == null)
            return;

        event.setCancelled(true);

        if(event.getMessage().toLowerCase().equals("cancel"))
        {
            api.removeCreation(sender.getUniqueId());
            sender.sendMessage("§aNotice creation has been cancelled. You can type normally now.");
            return;
        } else if(event.getMessage().toLowerCase().equals("save")) {
            DatabaseEngine.getPool().execute(() ->
            {
                Notice updated = api.saveNotice(notice);

                sender.sendMessage("§aSaved notice to the database with the following data:");
                ChatUtil.printNoticeInfo(sender, updated);
            });

            api.removeCreation(sender.getUniqueId());
            return;
        }

        notice.addMessage(event.getMessage());
        sender.sendMessage("§aAdded the following message to your notice creation.");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', event.getMessage()));
    }

}
