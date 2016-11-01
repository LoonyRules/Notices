package uk.co.loonyrules.notices.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

public class MiniNotice
{

    private int id = 0;
    private UUID uuid;
    private boolean seen = false, dismissed = false;

    public MiniNotice(int id, UUID uuid, boolean seen, boolean dismissed)
    {
        this.id = id;
        this.uuid = uuid;
        this.seen = seen;
        this.dismissed = dismissed;
    }

    public MiniNotice(ResultSet resultSet)
    {
        try
        {
            this.id = resultSet.getInt("notice_id");
            this.uuid = UUID.fromString(resultSet.getString("uuid"));
            this.seen = resultSet.getInt("seen") == 1;
            this.dismissed = resultSet.getInt("dismissed") == 1;
        } catch(SQLException e) {
            e.printStackTrace();
        }
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

    public void setSeen(boolean seen)
    {
        this.seen = seen;
    }

    public void setDismissed(boolean dismissed)
    {
        this.dismissed = dismissed;
    }

}
