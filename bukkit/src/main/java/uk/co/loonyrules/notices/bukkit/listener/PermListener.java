package uk.co.loonyrules.notices.bukkit.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import uk.co.loonyrules.notices.api.events.NoticePermCheckEvent;
import uk.co.loonyrules.notices.api.listeners.EventListener;

import java.util.UUID;

public class PermListener extends EventListener
{

    @Override
    public void onNoticePermCheckEvent(NoticePermCheckEvent event)
    {
        final UUID uuid = event.getUUID();
        Player player = Bukkit.getPlayer(uuid);

        event.setCancelled((player == null || !player.hasPermission(event.getNotice().getPerm())));
    }

}
