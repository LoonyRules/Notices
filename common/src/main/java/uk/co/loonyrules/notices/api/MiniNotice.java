package uk.co.loonyrules.notices.api;

import java.util.UUID;

public class MiniNotice
{

    private int id = 0;
    private UUID uuid;
    private boolean seen = false, dismissed = false;

    public MiniNotice(int id)
    {

    }

    public MiniNotice(int id, UUID uuid, boolean seen, boolean dismissed)
    {
        this.id = id;
        this.uuid = uuid;
        this.seen = seen;
        this.dismissed = dismissed;
    }

    public int getId()
    {
        return id;
    }

    public UUID getUUID()
    {
        return uuid;
    }

    public boolean hasSeen()
    {
        return seen;
    }

    public boolean hasDismissed()
    {
        return dismissed;
    }

}
