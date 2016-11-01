package uk.co.loonyrules.notices.api.events;

import uk.co.loonyrules.notices.api.Notice;
import uk.co.loonyrules.notices.api.NoticeAPI;

public abstract class Event
{

    private final NoticeAPI api;
    private final Notice notice;

    protected Event(NoticeAPI api, Notice notice)
    {
        this.api = api;
        this.notice = notice;
    }

    public NoticeAPI getAPI()
    {
        return api;
    }

    public Notice getNotice()
    {
        return notice;
    }

}
