package uk.co.loonyrules.notices.bungee.listener;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import uk.co.loonyrules.notices.api.events.NoticePermCheckEvent;
import uk.co.loonyrules.notices.api.listeners.EventListener;

import java.util.UUID;

public class PermListener extends EventListener
{

    @Override
    public void onNoticePermCheckEvent(NoticePermCheckEvent event)
    {
        final UUID uuid = event.getUUID();
        final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);

        event.setCancelled((player ==  null || !player.hasPermission(event.getNotice().getPerm())));
    }

}
