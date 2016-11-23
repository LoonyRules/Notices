package uk.co.loonyrules.notices.bungee;

import java.util.UUID;

public class OfflinePlayer
{

    private UUID uuid;
    private String lastName;

    public OfflinePlayer(UUID uuid, String lastName)
    {
        this.uuid = uuid;
        this.lastName = lastName;
    }

    public UUID getUUID()
    {
        return uuid;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }
}
