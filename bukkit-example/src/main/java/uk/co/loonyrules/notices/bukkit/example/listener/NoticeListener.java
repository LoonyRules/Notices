package uk.co.loonyrules.notices.bukkit.example.listener;

import com.google.gson.Gson;
import uk.co.loonyrules.notices.api.events.NoticeAddEvent;
import uk.co.loonyrules.notices.api.events.NoticeRemoveEvent;
import uk.co.loonyrules.notices.api.events.NoticeUpdateEvent;
import uk.co.loonyrules.notices.api.listeners.EventListener;

public class NoticeListener extends EventListener
{

    private Gson gson = new Gson();

    @Override
    public void onNoticeAddEvent(NoticeAddEvent event)
    {
        System.out.println("Adding: " + gson.toJson(event.getNotice()));
    }

    @Override
    public void onNoticeRemoveEvent(NoticeRemoveEvent event)
    {
        System.out.println("Removing: " + gson.toJson(event.getNotice()));
    }

    @Override
    public void onNoticeUpdateEvent(NoticeUpdateEvent event)
    {
        System.out.println("Updating: " + gson.toJson(event.getNotice()));
    }

}
