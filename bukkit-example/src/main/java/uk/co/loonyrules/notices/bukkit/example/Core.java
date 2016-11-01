package uk.co.loonyrules.notices.bukkit.example;

import org.bukkit.plugin.java.JavaPlugin;
import uk.co.loonyrules.notices.api.NoticeAPI;
import uk.co.loonyrules.notices.bukkit.Notices;
import uk.co.loonyrules.notices.bukkit.example.listener.NoticeListener;

public class Core extends JavaPlugin
{

    private NoticeAPI api;
    private NoticeListener noticeListener;

    @Override
    public void onEnable()
    {
        // Get API instance.
        api = Notices.getAPI();

        // Register your listeners (this will ignore the first load)
        api.addEventListener(noticeListener = new NoticeListener());
    }

    @Override
    public void onDisable()
    {
        api.removeEventListener(noticeListener);
    }

}
