package telran.monitoring;

import java.util.List;

import telran.monitoring.api.LatestValuesSaver;
import telran.monitoring.api.SensorData;

public class LatestDataSaverS3 implements LatestValuesSaver {

    @Override
    public void addValue(SensorData sensorData) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addValue'");
    }

    @Override
    public List<SensorData> getAllValues(long patientId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllValues'");
    }

    @Override
    public SensorData getLastValue(long patientId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLastValue'");
    }

    @Override
    public void clearValues(long patientId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'clearValues'");
    }

    @Override
    public void clearAndAddValue(long patientId, SensorData sensorData) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'clearAndAddValue'");
    }

}
