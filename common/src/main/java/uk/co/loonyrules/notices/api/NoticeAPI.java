package uk.co.loonyrules.notices.api;

import uk.co.loonyrules.notices.api.events.*;
import uk.co.loonyrules.notices.api.hooks.EventManager;
import uk.co.loonyrules.notices.api.util.Callback;
import uk.co.loonyrules.notices.core.Core;

import java.sql.*;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class NoticeAPI
{

    private final Core core;
    private final ConcurrentMap<Integer, Notice> notices = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, NoticePlayer> player = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Notice> creation = new ConcurrentHashMap<>();
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

    public Notice getCreation(UUID uuid)
    {
        return creation.get(uuid);
    }

    public void removeCreation(UUID uuid)
    {
        creation.remove(uuid);
    }

    public void addCreation(UUID uuid, Notice notice)
    {
        creation.put(uuid, notice);
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
        List<Notice> returning =  getNotices().stream().filter(notice ->
                (notice.getType() == Notice.Type.ALL
                        ? (noticePlayer.getNotice(notice.getId()) == null || !noticePlayer.getNotice(notice.getId()).hasDismissed())
                        : (notice.getType() == Notice.Type.INDIVIDUAL && (notice.getUUIDRecipients().contains(uuid) && (noticePlayer.getNotice(notice.getId()) == null || !noticePlayer.getNotice(notice.getId()).hasDismissed())))
                )
        ).collect(Collectors.toList());

        getNotices().stream().filter(notice -> !returning.contains(notice)).forEach(notice ->
        {
            if(notice.getType() == Notice.Type.PERM)
            {
                NoticePermCheckEvent event = new NoticePermCheckEvent(this, notice, uuid);
                eventManager.handle(event);

                if(!event.isCancelled())
                    returning.add(notice);
            }
        });

        return returning;
    }

    public Notice getNotice(int id) throws NoSuchElementException
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

    public Notice saveNotice(Notice notice)
    {
        NoticeSaveEvent event = new NoticeSaveEvent(this, notice);
        eventManager.handle(event);

        if(event.isCancelled())
            return null;

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        String QUERY = "SELECT * FROM `notices` WHERE id=?";
        String UPDATE = "UPDATE `notices` SET views=?, messages=?, uuidRecipients=?, perm=?, servers=?, type=?, expiration=?, dismissible=? WHERE id=? AND creator=?";
        String INSERT = "INSERT INTO `notices` (`views`, `creator`, `messages`, `uuidRecipients`, `perm`, `servers`, `type`, `expiration`, `dismissible`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            connection = core.getDatabaseEngine().getHikariCP().getConnection();

            preparedStatement = connection.prepareStatement(QUERY);
            preparedStatement.setInt(1, notice.getId());
            preparedStatement.execute();

            resultSet = preparedStatement.getResultSet();

            if(resultSet.next())
            {
                // update
                preparedStatement = connection.prepareStatement(UPDATE);
                preparedStatement.setInt(1, notice.getViews());
                preparedStatement.setString(2, notice.getMessages().toString());
                preparedStatement.setString(3, notice.getUUIDRecipients().toString());
                preparedStatement.setString(4, notice.getPerm());
                preparedStatement.setString(5, notice.getServers().toString());
                preparedStatement.setString(6, notice.getType().toString());
                preparedStatement.setLong(7, notice.getExpiration());
                preparedStatement.setInt(8, notice.isDismissible() ? 1 : 0);
                preparedStatement.setInt(9, notice.getId());
                preparedStatement.setString(10, notice.getCreator().toString());
                preparedStatement.execute();

                notice = updateNotice(notice);
            } else {
                // insert
                preparedStatement = connection.prepareStatement(INSERT);
                preparedStatement.setInt(1, notice.getViews());
                preparedStatement.setString(2, notice.getCreator().toString());
                preparedStatement.setString(3, notice.getMessages().toString());
                preparedStatement.setString(4, notice.getUUIDRecipients().toString());
                preparedStatement.setString(5, notice.getPerm());
                preparedStatement.setString(6, notice.getServers().toString());
                preparedStatement.setString(7, notice.getType().toString());
                preparedStatement.setLong(8, notice.getExpiration());
                preparedStatement.setInt(9, notice.isDismissible() ? 1 : 0);
                preparedStatement.execute();

                preparedStatement = connection.prepareStatement("SELECT LAST_INSERT_ID();");
                preparedStatement.execute();

                resultSet = preparedStatement.getResultSet();

                if(resultSet.next())
                    notice.setId(resultSet.getInt(1));

                addNotice(notice);
            }
        } catch(SQLException e) {
            System.out.println(Core.PREFIX + ": Error when inserting/updating Notice to the database.");
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

    public void deleteNotice(Notice notice)
    {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        String data = "DELETE FROM `notices` WHERE id=?;";
        String udv = "DELETE FROM `notices_udv` WHERE notice_id=?";

        try {
            connection = core.getDatabaseEngine().getHikariCP().getConnection();

            preparedStatement = connection.prepareStatement(data);
            preparedStatement.setInt(1, notice.getId());
            preparedStatement.execute();

            preparedStatement = connection.prepareStatement(udv);
            preparedStatement.setInt(1, notice.getId());
            preparedStatement.execute();
        } catch(SQLException e) {
            System.out.println(Core.PREFIX + ": Error when deleting notice from Database.");
            e.printStackTrace();
        } finally {
            try {
                if(connection != null) connection.close();
                if(preparedStatement != null) preparedStatement.close();
            } catch(SQLException e) {
                System.out.println(Core.PREFIX + ": Error when closing connections.");
                e.printStackTrace();
            }
        }

        notices.remove(notice.getId());
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
