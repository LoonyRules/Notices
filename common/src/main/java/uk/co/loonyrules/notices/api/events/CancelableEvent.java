package uk.co.loonyrules.notices.api.events;

import uk.co.loonyrules.notices.api.Notice;
import uk.co.loonyrules.notices.api.NoticeAPI;

public abstract class CancelableEvent extends Event
{

    private boolean cancelled = false;

    public CancelableEvent(NoticeAPI api, Notice notice)
    {
        super(api, notice);
    }

    public boolean isCancelled()
    {
        return cancelled;
    }

    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }

}
