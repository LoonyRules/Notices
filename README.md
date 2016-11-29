# Notices
Send a dismissible notice to specific players on your server/proxy.

### Downloads

* Bukkit - https://www.spigotmc.org/resources/dismissible-notices.31290/
* BungeeCord - https://www.spigotmc.org/resources/dismissible-notices.32197/

### Documentation

I'm sorry for the lack of documentation, it's simply code to follow for the API side of things so you should be able to follow along. But I will gradually add documentation to it at some point.

When retrieving the API instance, simply do
```Java
NoticeAPI api = Notices.getAPI();
```

We don't use the Bukkit/BungeeCord event system. We wanted a system that would work in both plugins, so I made my own. Extend our [EventListener](https://github.com/LoonyRules/Notices/blob/master/common/src/main/java/uk/co/loonyrules/notices/api/listeners/EventListener.java) class in your listener class and you'll be able to override our [event methods](https://github.com/LoonyRules/Notices/tree/master/common/src/main/java/uk/co/loonyrules/notices/api/events). 
```Java
public class Listener extends EventListener
{
    
    // Override event methods in here.

}
```

That's the basic stuff. Now onto the NoticeAPI class itself. 

#### Get the Notice a player is creating.
```Java
    api.getCreation(uuid);
```

#### Discard a player's creation Notice.
```Java
    api.removeCreation(uuid);
```

#### Insert a player's Notice creation. (This will start the creation process).
```Java
    api.addCreation(uuid, notice);
```

#### Get a player's data.
```Java
    api.getPlayer(uuid);
```

#### Cache a player's data (automatically managed for you).
```Java
    api.cachePlayer(uuid, toSave);
```

#### Remove a player's data from the cache
```Java
    api.removePlayer(uuid);
```

#### Update a MiniNotice (assigned to a player via their UUID).
```Java
    api.updatePlayer(miniNotice);
```

#### Get all cached Notices.
```Java
   api.getNotices()
```


#### Get notices associated via a player's UUID.
```Java
    api.getNotices(uuid);
```

#### Get notices associated via a player's NoticePlayer data
```Java
    api.getNoticed(uuid, noticePlayer);
```

#### Get a notice from the cache via its ID. - Throws NoSuchElementException if no cache found.
```Java
    api.getNotice(id);
```

#### Add a notice to the cache.
```Java
    api.addNotice(notice);
```

#### Remove a notice from the cache.
```Java
    api.removeNotice(notice);
```

#### Save a notice's data.
```Java
    api.saveNotice(notice);
```

#### Delete a notice's data. (From the database)
```Java
    api.deleteNotice(notice);
```

#### Update notice (Get data from the database)
```Java
    api.updateNotice(notice);
```

#### Clear notices cache
```Java
    api.clearNotices();
```

#### Clear notices without calling events
```Java
    api.forceClearNotices();
```

#### Load notices from the database (automatically done every 30 seconds Asynchronously).
```Java
    api.loadNotices();
```

#### Add event listener
```Java
    api.addEventListener(eventListenerInstance);
```

#### Remove event listener
```Java
    api.removeEventListener(eventListenerInstance);
```
