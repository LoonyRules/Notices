package uk.co.loonyrules.notices.core.database;

import com.zaxxer.hikari.HikariDataSource;
import uk.co.loonyrules.notices.core.Core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseEngine
{

    private static ExecutorService pool;

    private DatabaseEngine instance;
    private HikariDataSource hikariCP;

    public DatabaseEngine(String host, int port)
    {
        this(host, port, null, null, null);
    }

    public DatabaseEngine(String host, int port, String database)
    {
        this(host, port, database, null, null);
    }

    public DatabaseEngine(String host, int port, String database, String username)
    {
        this(host, port, database, username, null);
    }

    public DatabaseEngine(String host, int port, String database, String username, String password)
    {
        instance = this;
        pool = Executors.newCachedThreadPool();

        hikariCP = new HikariDataSource();
        hikariCP.setMaximumPoolSize(10);
        hikariCP.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        hikariCP.addDataSourceProperty("serverName", host);
        hikariCP.addDataSourceProperty("port", port);
        hikariCP.addDataSourceProperty("databaseName", database);
        hikariCP.addDataSourceProperty("user", username);
        hikariCP.addDataSourceProperty("password", password);

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        String query = "CREATE TABLE IF NOT EXISTS `notices` (`id` int(11) NOT NULL AUTO_INCREMENT,`views` int(11) NOT NULL,`creator` varchar(36) NOT NULL,`messages` longtext NOT NULL,`uuidRecipients` longtext NOT NULL,`perm` varchar(32) NOT NULL,`servers` longtext NOT NULL,`type` text NOT NULL,`expiration` bigint(20) NOT NULL,`dismissible` int(1) NOT NULL, PRIMARY KEY (`id`)) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=latin1";

        try {
            connection = hikariCP.getConnection();

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.execute();

            query = "CREATE TABLE IF NOT EXISTS `notices_udv` (`id` int(11) NOT NULL AUTO_INCREMENT,`notice_id` int(11) NOT NULL,`uuid` varchar(36) NOT NULL,`seen` int(1) NOT NULL,`dismissed` int(1) NOT NULL, PRIMARY KEY (`id`)) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=latin1 ";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.execute();
        } catch(SQLException e) {
            System.out.println(Core.PREFIX + ": Error when creating tables.");
            e.printStackTrace();
        } finally {
            try {
                if(connection != null) connection.close();
                if(preparedStatement != null) preparedStatement.close();

                System.out.println(Core.PREFIX + ": MySQL setup correctly.");
            } catch(SQLException e) {
                System.out.println(Core.PREFIX + ": Error when closing connections.");
                e.printStackTrace();
            }
        }
    }

    public HikariDataSource getHikariCP()
    {
        return hikariCP;
    }

    public DatabaseEngine getInstance()
    {
        return instance;
    }

    public static ExecutorService getPool()
    {
        return pool;
    }

}
