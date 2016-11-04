package uk.co.loonyrules.notices.api.events;

import uk.co.loonyrules.notices.api.Notice;
import uk.co.loonyrules.notices.api.NoticeAPI;

public class NoticeSaveEvent extends CancelableEvent
{

    public NoticeSaveEvent(NoticeAPI api, Notice notice)
    {
        super(api, notice);
    }

}
