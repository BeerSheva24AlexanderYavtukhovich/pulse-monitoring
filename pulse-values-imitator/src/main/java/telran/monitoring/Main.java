package telran.monitoring;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.IntStream;

import telran.monitoring.api.SensorData;
import telran.monitoring.logging.Logger;
import telran.monitoring.logging.LoggerStandard;

public class Main {
    static Logger logger = new LoggerStandard("imitator");
    static final int MIN_PULSE_VALUE = 25;
    static final int MAX_PULSE_VALUE = 295;
    static final long TIMEOUT_SEND = 500;
    static final int TIMEOUT_RESPONSE = 5000;
    static final String DEFAULT_HOST = "localhost";
    static final int DEFAULT_PORT = 5000;
    static final int DEFAULT_N_PACKETS = 50;
    static final int JUMP_PROB = 10;
    static final int MIN_JUMP_PERCENT = 10;
    static final int MAX_JUMP_PERCENT = 100;
    static final int JUMP_POSITIVE_PROB = 70;
    static final long PATIENT_ID_FOR_INFO_LOGGING = 1;
    private static final int PATIENTS_RANGE_ID_MIN = 1;
    private static final int PATIENTS_RANGE_ID_MAX = 10;
    static HashMap<Long, Integer> patientIdPulseValue = new HashMap<>();
    static DatagramSocket socket = null;

    public static void main(String[] args) throws Exception {
        socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT_RESPONSE);
        IntStream.rangeClosed(1, DEFAULT_N_PACKETS).forEach(Main::send);
    }

    static void send(int i) {
        SensorData sensor = getRandomSensorData(i);
        logger.log("finest", sensor.toString());
        if (sensor.patientId() == PATIENT_ID_FOR_INFO_LOGGING) {
            logger.log("info", String.format("Pulse value for patient %d is %d",
                    PATIENT_ID_FOR_INFO_LOGGING, sensor.value()));
        }
        String jsonStr = sensor.toString();
        try {
            udpSend(jsonStr);
            Thread.sleep(TIMEOUT_SEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void udpSend(String jsonStr) throws Exception {
        logger.log("finest", String.format("data to be sent is %s", jsonStr));
        byte[] bufferSend = jsonStr.getBytes();
        DatagramPacket packet = new DatagramPacket(bufferSend, bufferSend.length,
                InetAddress.getByName(DEFAULT_HOST), DEFAULT_PORT);
        socket.send(packet);
        socket.receive(packet);
        if (!jsonStr.equals(new String(packet.getData()))) {
            throw new Exception("received packet doesn't equal the sent one");
        }
    }

    static SensorData getRandomSensorData(int i) {
        long patientId = getRandomNumber(PATIENTS_RANGE_ID_MIN, PATIENTS_RANGE_ID_MAX);
        int value = getRandomPulseValue(patientId);
        long timestamp = System.currentTimeMillis();
        SensorData res = new SensorData(patientId, value, timestamp);
        return res;
    }

    private static int getRandomPulseValue(long patientId) {
        int valueRes = patientIdPulseValue.computeIfAbsent(patientId,
                k -> getRandomNumber(MIN_PULSE_VALUE, MAX_PULSE_VALUE));
        if (chance(JUMP_PROB)) {
            valueRes = getValueWithJump(valueRes);
            patientIdPulseValue.put(patientId, valueRes);
        }

        return valueRes;
    }

    static int getRandomNumber(int minValue, int maxValue) {
        return new Random().nextInt(minValue, maxValue + 1);
    }

    private static boolean chance(int prob) {
        return getRandomNumber(0, 99) < prob;
    }

    private static int getValueWithJump(int previousValue) {
        int jumpPercent = getRandomNumber(MIN_JUMP_PERCENT, MAX_JUMP_PERCENT);
        int jumpValue = previousValue * jumpPercent / 100;
        if (!chance(JUMP_POSITIVE_PROB)) {
            jumpValue = -jumpValue;
        }
        int res = previousValue + jumpValue;
        if (res < MIN_PULSE_VALUE) {
            res = MIN_PULSE_VALUE;
        } else if (res > MAX_PULSE_VALUE) {
            res = MAX_PULSE_VALUE;
        }
        return res;
    }
}