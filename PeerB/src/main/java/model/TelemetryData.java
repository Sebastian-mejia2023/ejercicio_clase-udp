package model;

import java.time.Instant;

/**
 * Representa una lectura de telemetría reportada por un nodo sensor IoT.
 */
public class TelemetryData {
    private final String deviceId;
    private final String sensorType;
    private final double value;
    private final Instant timestamp;

    public TelemetryData(String deviceId, String sensorType, double value) {
        this.deviceId = deviceId;
        this.sensorType = sensorType;
        this.value = value;
        this.timestamp = Instant.now();
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getSensorType() {
        return sensorType;
    }

    public double getValue() {
        return value;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "TelemetryData{" +
                "deviceId='" + deviceId + '\'' +
                ", sensorType='" + sensorType + '\'' +
                ", value=" + value +
                ", timestamp=" + timestamp +
                '}';
    }
}
