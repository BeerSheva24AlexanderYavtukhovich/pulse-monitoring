package telran.monitoring.api;

import java.util.List;

import telran.monitoring.logging.Logger;

public interface LatestValuesSaver {
   void addValue(SensorData sensorData);

   List<SensorData> getAllValues(long patientId);

   SensorData getLastValue(long patientId);

   void clearValues(long patientId);

   void clearAndAddValue(long patientId, SensorData sensorData);

   static LatestValuesSaver getLatestValuesSaver(String latestValuesSaverClassName, Logger logger) {
      try {
         logger.log("info", "Creating instance of " + latestValuesSaverClassName);
         return (LatestValuesSaver) Class.forName(latestValuesSaverClassName).getDeclaredConstructor(Logger.class).newInstance(logger);
      } catch (Exception e) {
         logger.log("error", "Error creating LatestValuesSaver instance: " + e.getMessage());
         throw new RuntimeException("Error creating LatestValuesSaver instance", e);
      }
   }
}