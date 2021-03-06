package uk.co.loonyrules.notices.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Notice
{

    /**
     * The unique id that was assigned.
     */
    private int id;

    /**
     * Views this Notice has.
     */
    private int views = 0;

    /**
     * Who created this Notice.
     */
    private UUID creator;

    /**
     * The messages the notice has.
     */
    private List<String> messages = new ArrayList<>();

    /**
     * List of recipient UUID's.
     */
    private List<UUID> uuidRecipients = new ArrayList<>();

    /**
     * List of servers
     */
    private List<String> servers = new ArrayList<>();

    /**
     * Permission if Type.PERM
     */
    private String perm = "";

    /**
     * Notice type.
     */
    private Type type = Type.ALL;

    /**
     * Expiration timestamp
     */
    private long expiration;

    /**
     * Whether or not the Notice is dismissible.
     */
    private boolean dismissible = false;

    public Notice(ResultSet resultSet) throws SQLException, NumberFormatException
    {
        this.id = resultSet.getInt("id");
        this.views = resultSet.getInt("views");
        this.creator = UUID.fromString(resultSet.getString("creator"));

        String rawMessages = resultSet.getString("messages");
        this.messages = new ArrayList<>(Arrays.asList(rawMessages.substring(1, rawMessages.length() -1).split(", ")));

        String rawUR = resultSet.getString("uuidRecipients");
        if(!(rawUR.isEmpty() || rawUR.length() == 0 || rawUR.equals("[]")))
            Arrays.asList(rawUR.substring(1, rawUR.length() -1).split(", ")).forEach(uuid -> uuidRecipients.add(UUID.fromString(uuid)));

        String rawSE = resultSet.getString("servers");
        if(!(rawSE.isEmpty() || rawSE.length() == 0 || rawSE.equals("[]")))
            Arrays.asList(rawSE.substring(1, rawSE.length() -1).split(", ")).forEach(servers::add);

        this.perm = resultSet.getString("perm");
        this.type = Type.valueOf(resultSet.getString("type"));
        this.expiration = Long.parseLong(resultSet.getInt("expiration") + "");
        this.dismissible = resultSet.getInt("dismissible") == 1;
    }

    /**
     *
     * Initialise a new Notice instance, this does not register the Notice.
     *  @param creator is who made this Notice.
     * @param expiration is when the Notice expires.
     * @param dismissible whether or not the Notice is dismissible.
     * @param messages a String[] (String...) of messages to display.
     */
    public Notice(UUID creator, long expiration, boolean dismissible, String... messages)
    {
        this(creator, Type.ALL, expiration, dismissible, messages);
    }

    /**
     *
     * Initialise a new Notice instance, this does not register the Notice.
     *
     * @param creator is who made this Notice.
     * @param type is the Notice$Type.
     * @param expiration is when the Notice expires.
     * @param dismissible whether or not the Notice is dismissible.
     * @param messages a String[] (String...) of messages to display.
     */
    public Notice(UUID creator, Type type, long expiration, boolean dismissible, String... messages)
    {
        this.creator = creator;
        this.type = type;
        this.expiration = expiration;
        this.dismissible = dismissible;
        this.messages = new ArrayList<>(Arrays.asList(messages));
    }

    /**
     * Get the Notice unique id.
     * @return the unique id of this Notice.
     */
    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    /**
     * Get the views this Notice has.
     * @return the total views.
     */
    public int getViews()
    {
        return views;
    }

    /**
     * Get the creator of this Notice.
     * @return uuid of the creator.
     */
    public UUID getCreator()
    {
        return creator;
    }

    /**
     * Get the messages to send.
     * @return messages to send.
     */
    public List<String> getMessages()
    {
        return messages;
    }

    /**
     * Get the UUID recipients.
     * @return UUIDs to send to (if Notice$Type.INDIVIDUAL).
     */
    public List<UUID> getUUIDRecipients()
    {
        return uuidRecipients;
    }

    /**
     * Get the Notice$Type of this Notice.
     * @return type of the notice.
     */
    public Type getType()
    {
        return type;
    }

    /**
     * Get the expiration timestamp.
     * @return expiration timestamp.
     */
    public long getExpiration()
    {
        return expiration;
    }

    public String getPerm()
    {
        return perm;
    }

    /**
     * Get whether Notice is dismissible.
     * @return is dismissible.
     */
    public boolean isDismissible()
    {
        return dismissible;
    }

    public void setPerm(String perm)
    {
        this.perm = perm;
    }

    public void addView()
    {
        views += 1;
    }

    public void addMessage(String message)
    {
        messages.add(message);
    }

    public void removeMessage(int id)
    {
        messages.remove(id);
    }

    public void addUUIDRecipient(UUID uniqueId)
    {
        uuidRecipients.add(uniqueId);
    }

    public void removeUUIDRecipient(UUID uniqueId)
    {
        uuidRecipients.remove(uniqueId);
    }

    public List<String> getServers()
    {
        return servers;
    }

    public void addServer(String name)
    {
        servers.add(name);
    }

    /**
     * Notice$Type
     */
    public enum Type
    {
        /**
         * Target is everyone that joins.
         */
        ALL,

        /**
         * Target is individual players.
         */
        INDIVIDUAL,

        /**
         * Target is a specific server.
         */
        SERVER,

        /**
         * Target is a specific rank group.
         */
        PERM
    }

}
