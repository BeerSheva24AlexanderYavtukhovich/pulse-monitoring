package telran.monitoring;

import telran.monitoring.api.LatestValuesSaver;
import telran.monitoring.logging.Logger;

public abstract class DataSaverLogger implements LatestValuesSaver {
    protected Logger logger;

    protected DataSaverLogger(Logger logger) {
        this.logger = logger;
    }
}
