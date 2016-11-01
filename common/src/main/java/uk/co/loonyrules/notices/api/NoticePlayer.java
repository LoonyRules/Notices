package uk.co.loonyrules.notices.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NoticePlayer
{

    private final UUID uuid;
    private final ConcurrentMap<Integer, MiniNotice> notices = new ConcurrentHashMap<>();

    public NoticePlayer(UUID uuid, ResultSet resultSet)
    {
        this.uuid = uuid;

        try
        {
            while(resultSet.next())
                addNotice(new MiniNotice(resultSet));
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    public UUID getUUID()
    {
        return uuid;
    }

    public Collection<MiniNotice> getNotices()
    {
        return notices.values();
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
