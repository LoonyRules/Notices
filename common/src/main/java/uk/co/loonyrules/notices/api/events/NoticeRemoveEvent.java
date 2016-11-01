package uk.co.loonyrules.notices.api.events;

import uk.co.loonyrules.notices.api.Notice;
import uk.co.loonyrules.notices.api.NoticeAPI;

public class NoticeRemoveEvent extends CancelableEvent
{

    public NoticeRemoveEvent(NoticeAPI api, Notice notice)
    {
        super(api, notice);
    }

}
