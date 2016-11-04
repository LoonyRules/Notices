package uk.co.loonyrules.notices.api.listeners;

import uk.co.loonyrules.notices.api.events.*;
import uk.co.loonyrules.notices.api.hooks.ListenerHook;

public abstract class EventListener implements ListenerHook
{

    public void onNoticeAddEvent(NoticeAddEvent event) {}
    public void onNoticeRemoveEvent(NoticeRemoveEvent event) {}
    public void onNoticeUpdateEvent(NoticeUpdateEvent event) {}
    public void onNoticeSaveEvent(NoticeSaveEvent event) {}

    @Override
    public void onEvent(Event event)
    {
        if(event instanceof NoticeAddEvent)
            onNoticeAddEvent((NoticeAddEvent) event);
        else if(event instanceof NoticeRemoveEvent)
            onNoticeRemoveEvent((NoticeRemoveEvent) event);
        else if(event instanceof NoticeUpdateEvent)
            onNoticeUpdateEvent((NoticeUpdateEvent) event);
        else if(event instanceof NoticeSaveEvent)
            onNoticeSaveEvent((NoticeSaveEvent) event);
    }

}
