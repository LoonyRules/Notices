package uk.co.loonyrules.notices.core;

import uk.co.loonyrules.notices.core.database.DatabaseEngine;

public interface Core
{

    String DISMISS = "§7§l[X] §f";
    String DIVIDER = "§e§m---------------------------------------------";
    String NAME = "Notices", PREFIX = "[" + NAME + "]";

    DatabaseEngine getDatabaseEngine();

}
