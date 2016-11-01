package uk.co.loonyrules.notices.api.events;

import uk.co.loonyrules.notices.api.Notice;
import uk.co.loonyrules.notices.api.NoticeAPI;

public class NoticeUpdateEvent extends CancelableEvent
{

    public NoticeUpdateEvent(NoticeAPI api, Notice notice)
    {
        super(api, notice);
    }

}
