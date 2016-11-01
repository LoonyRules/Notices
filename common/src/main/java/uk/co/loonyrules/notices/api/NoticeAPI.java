package uk.co.loonyrules.notices.api;

import uk.co.loonyrules.notices.api.events.NoticeAddEvent;
import uk.co.loonyrules.notices.api.events.NoticeRemoveEvent;
import uk.co.loonyrules.notices.api.events.NoticeUpdateEvent;
import uk.co.loonyrules.notices.api.hooks.EventManager;
import uk.co.loonyrules.notices.api.util.Callback;
import uk.co.loonyrules.notices.core.Core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class NoticeAPI
{

    private final Core core;
    private final ConcurrentMap<Integer, Notice> notices = new ConcurrentHashMap<>();
    private final EventManager eventManager;

    public NoticeAPI(Core core)
    {
        this.core = core;
        this.eventManager = new EventManager();
    }

    public Core getCore()
    {
        return core;
    }

    public Collection<Notice> getNotices()
    {
        return notices.values();
    }

    public Collection<Notice> getNotices(UUID uuid)
    {
        return getNotices().stream().filter(notice -> (notice.getType() == Notice.Type.ALL && notice.isDismissible() && !notice.getDismissed().contains(uuid))).collect(Collectors.toList());
    }

    public Notice getNotice(int id)
    {
        return getNotices().stream().filter(notice -> notice.getId() == id).findFirst().get();
    }

    public Notice addNotice(Notice notice)
    {
        NoticeAddEvent event = new NoticeAddEvent(this, notice);
        eventManager.handle(event);

        if(event.isCancelled())
            return null;

        notices.put(notice.getId(), notice);
        return notice;
    }

    public Notice removeNotice(Notice notice)
    {
        NoticeRemoveEvent event = new NoticeRemoveEvent(this, notice);
        eventManager.handle(event);

        if(event.isCancelled())
            return null;

        notices.remove(notice.getId());
        return notice;
    }

    public Notice updateNotice(Notice notice)
    {
        NoticeUpdateEvent event = new NoticeUpdateEvent(this, notice);
        eventManager.handle(event);

        if(event.isCancelled())
            return null;

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String query = "SELECT * FROM (SELECT * FROM `notices` ORDER BY id DESC) x WHERE x.expiration >= UNIX_TIMESTAMP() AND id=? LIMIT 1;";

        try {
            connection = core.getDatabaseEngine().getHikariCP().getConnection();

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, notice.getId());
            preparedStatement.execute();

            resultSet = preparedStatement.getResultSet();

            // If gets result, update, else null it
            notice = resultSet.next() ? new Notice(resultSet) : null;
        } catch(SQLException e) {
            System.out.println(Core.PREFIX + ": Error when loading Notices from the Database.");
            e.printStackTrace();
        } finally {
            try {
                if(connection != null) connection.close();
                if(preparedStatement != null) preparedStatement.close();
                if(resultSet != null) resultSet.close();

                System.out.println(Core.PREFIX + ": Loading Notices was successful.");
            } catch(SQLException e) {
                System.out.println(Core.PREFIX + ": Error when closing connections.");
                e.printStackTrace();
            }
        }

        return notice;
    }

    public void clearNotices()
    {
        notices.forEach((integer, notice) -> removeNotice(notice));
    }

    public void forceClearNotices()
    {
        notices.clear();
    }

    public void loadNotices()
    {
        clearNotices();

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String query = "SELECT * FROM (SELECT * FROM `notices` ORDER BY id DESC) x WHERE x.expiration >= UNIX_TIMESTAMP();";

        try {
            connection = core.getDatabaseEngine().getHikariCP().getConnection();

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.execute();

            resultSet = preparedStatement.getResultSet();

            while(resultSet.next())
                addNotice(new Notice(resultSet));
        } catch(SQLException e) {
            System.out.println(Core.PREFIX + ": Error when loading Notices from the Database.");
            e.printStackTrace();
        } finally {
            try {
                if(connection != null) connection.close();
                if(preparedStatement != null) preparedStatement.close();
                if(resultSet != null) resultSet.close();

                System.out.println(Core.PREFIX + ": Loading Notices was successful.");
            } catch(SQLException e) {
                System.out.println(Core.PREFIX + ": Error when closing connections.");
                e.printStackTrace();
            }
        }
    }

    public void loadNotices(Callback<Collection<Notice>> callback)
    {
        loadNotices();
        callback.call(getNotices());
    }

    public void addEventListener(Object... listeners)
    {
        for(Object listener : listeners)
            eventManager.register(listener);
    }

    public void removeEventListener(Object... listeners)
    {
        for(Object listener : listeners)
            eventManager.unregister(listener);
    }

}
