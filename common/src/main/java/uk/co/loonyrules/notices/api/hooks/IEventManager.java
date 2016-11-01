package uk.co.loonyrules.notices.api.hooks;

import uk.co.loonyrules.notices.api.events.Event;

import java.util.List;

public interface IEventManager
{

    void register(Object listener);
    void unregister(Object listener);
    void handle(Event event);

    List<Object> getListeners();
}
