package telran.monitoring;

import telran.monitoring.api.Range;

public interface RangeProviderClient {
    Range getRange(long patientId);
}

