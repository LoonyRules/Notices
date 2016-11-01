package uk.co.loonyrules.notices.api;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NoticePlayer
{

    private final UUID uuid;
    private final ConcurrentMap<Integer, MiniNotice> notices = new ConcurrentHashMap<>();

    public NoticePlayer(UUID uuid)
    {
        this.uuid = uuid;
    }

    public UUID getUUID()
    {
        return uuid;
    }

    public MiniNotice getNotice(int id)
    {
        return notices.get(id);
    }

    public void removeNotice(int id)
    {
        notices.remove(id);
    }

    public void addNotice(MiniNotice miniNotice)
    {
        notices.put(miniNotice.getId(), miniNotice);
    }

}
