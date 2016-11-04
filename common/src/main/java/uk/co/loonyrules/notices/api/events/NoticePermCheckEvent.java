package uk.co.loonyrules.notices.api.events;

import uk.co.loonyrules.notices.api.Notice;
import uk.co.loonyrules.notices.api.NoticeAPI;

import java.util.UUID;

public class NoticePermCheckEvent extends CancelableEvent
{

    private final UUID uuid;
    public NoticePermCheckEvent(NoticeAPI api, Notice notice, UUID uuid)
    {
        super(api, notice);
        this.uuid = uuid;
    }

    public UUID getUUID()
    {
        return uuid;
    }

}
