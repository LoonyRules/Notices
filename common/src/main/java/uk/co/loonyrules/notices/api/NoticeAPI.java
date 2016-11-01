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
    private final ConcurrentMap<UUID, NoticePlayer> player = new ConcurrentHashMap<>();
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

    public NoticePlayer getPlayer(UUID uuid)
    {
        return player.get(uuid);
    }

    public NoticePlayer cachePlayer(UUID uuid, boolean toSave)
    {
        NoticePlayer noticePlayer = getPlayer(uuid);

        if(noticePlayer != null)
            return noticePlayer;

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        String QUERY = "SELECT * FROM `notices_udv` WHERE uuid=?";

        try {
            connection = core.getDatabaseEngine().getHikariCP().getConnection();

            preparedStatement = connection.prepareStatement(QUERY);
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.execute();

            noticePlayer = new NoticePlayer(uuid, resultSet = preparedStatement.getResultSet());
        } catch(SQLException e) {
            System.out.println(Core.PREFIX + ": Error when loading NoticePlayer from the database.");
            e.printStackTrace();
        } finally {
            try {
                if(connection != null) connection.close();
                if(preparedStatement != null) preparedStatement.close();
                if(resultSet != null) resultSet.close();
            } catch(SQLException e) {
                System.out.println(Core.PREFIX + ": Error when closing connections.");
                e.printStackTrace();
            }
        }

        if(toSave)
            player.putIfAbsent(uuid, noticePlayer);

        return noticePlayer;
    }

    public void removePlayer(UUID uuid)
    {
        player.remove(uuid);
    }

    public void updatePlayer(MiniNotice miniNotice)
    {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        String QUERY = "SELECT * FROM `notices_udv` WHERE notice_id=? AND uuid=?";
        String UPDATE = "UPDATE `notices_udv` SET seen=?, dismissed=? WHERE notice_id=? AND uuid=?";
        String INSERT = "INSERT INTO `notices_udv` (`notice_id`, `uuid`, `seen`, `dismissed`) VALUES (?, ?, ?, ?)";

        try {
            connection = core.getDatabaseEngine().getHikariCP().getConnection();

            preparedStatement = connection.prepareStatement(QUERY);
            preparedStatement.setInt(1, miniNotice.getId());
            preparedStatement.setString(2, miniNotice.getUUID().toString());
            preparedStatement.execute();

            resultSet = preparedStatement.getResultSet();

            if(resultSet.next())
            {
                // update
                preparedStatement = connection.prepareStatement(UPDATE);
                preparedStatement.setInt(1, miniNotice.hasSeen() ? 1 : 0);
                preparedStatement.setInt(2, miniNotice.hasDismissed() ? 1 : 0);
                preparedStatement.setInt(3, miniNotice.getId());
                preparedStatement.setString(4, miniNotice.getUUID().toString());
                preparedStatement.execute();
            } else {
                // insert
                preparedStatement = connection.prepareStatement(INSERT);
                preparedStatement.setInt(1, miniNotice.getId());
                preparedStatement.setString(2, miniNotice.getUUID().toString());
                preparedStatement.setInt(3, miniNotice.hasSeen() ? 1 : 0);
                preparedStatement.setInt(4, miniNotice.hasDismissed() ? 1 : 0);
                preparedStatement.execute();
            }
        } catch(SQLException e) {
            System.out.println(Core.PREFIX + ": Error when saving MiniNotice to the database.");
            e.printStackTrace();
        } finally {
            try {
                if(connection != null) connection.close();
                if(preparedStatement != null) preparedStatement.close();
                if(resultSet != null) resultSet.close();
            } catch(SQLException e) {
                System.out.println(Core.PREFIX + ": Error when closing connections.");
                e.printStackTrace();
            }
        }
    }

    public Collection<Notice> getNotices()
    {
        return notices.values();
    }

    public Collection<Notice> getNotices(UUID uuid)
    {
        return getNotices(uuid, player.get(uuid));
    }

    public Collection<Notice> getNotices(UUID uuid, NoticePlayer noticePlayer)
    {
        return getNotices().stream().filter(notice ->
                (notice.getType() == Notice.Type.ALL
                        ? (noticePlayer.getNotice(notice.getId()) == null || !noticePlayer.getNotice(notice.getId()).hasDismissed())
                        : (notice.getType() == Notice.Type.INDIVIDUAL && (notice.getUUIDRecipients().contains(uuid) && (noticePlayer.getNotice(notice.getId()) == null || !noticePlayer.getNotice(notice.getId()).hasDismissed())))
                )
        ).collect(Collectors.toList());
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
