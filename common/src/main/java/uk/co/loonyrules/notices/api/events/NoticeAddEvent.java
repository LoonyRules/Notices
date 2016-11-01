package uk.co.loonyrules.notices.api.events;

import uk.co.loonyrules.notices.api.Notice;
import uk.co.loonyrules.notices.api.NoticeAPI;

public class NoticeAddEvent extends CancelableEvent
{

    public NoticeAddEvent(NoticeAPI api, Notice notice)
    {
        super(api, notice);
    }

}
