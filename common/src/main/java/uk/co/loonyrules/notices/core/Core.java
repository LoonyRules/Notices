package uk.co.loonyrules.notices.core;

import uk.co.loonyrules.notices.core.database.DatabaseEngine;

public interface Core
{

    String NAME = "Notices", PREFIX = "[" + NAME + "]";

    DatabaseEngine getDatabaseEngine();

}
