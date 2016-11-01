package uk.co.loonyrules.notices.api.hooks;

import uk.co.loonyrules.notices.api.events.Event;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager implements IEventManager
{

    private final CopyOnWriteArrayList<ListenerHook> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void register(Object listener)
    {
        if(!(listener instanceof ListenerHook))
            throw new IllegalArgumentException("Listener must implement ListenerHook.");

        listeners.add((ListenerHook) listener);
    }

    @Override
    public void unregister(Object listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void handle(Event event)
    {
        listeners.forEach(listener ->
        {
            try {
                listener.onEvent(event);
            } catch(Throwable throwable) {
                System.out.println(throwable);
            }
        });
    }

    @Override
    public List<Object> getListeners()
    {
        return Collections.unmodifiableList(new LinkedList<>(listeners));
    }
}
