package uk.co.loonyrules.notices.api.hooks;

import uk.co.loonyrules.notices.api.events.Event;

public interface ListenerHook
{

    void onEvent(Event event);

}
