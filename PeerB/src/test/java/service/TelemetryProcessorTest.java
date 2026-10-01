package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TelemetryProcessorTest {

    private TelemetryProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TelemetryProcessor();
    }

    @Test
    @DisplayName("CP-01: Registro de temperatura en rango normal")
    void testNormalTemperature() {
        String response = processor.process("sensor-01;TEMP;25.0");
        assertEquals("OK;TEMP_RECORDED;25.0", response);
    }

    @Test
    @DisplayName("CP-02: Alerta de temperatura alta (> 40.0)")
    void testHighTemperatureAlert() {
        String response = processor.process("sensor-01;TEMP;42.5");
        assertEquals("ALERT;HIGH_TEMPERATURE;42.5", response);
    }

    @Test
    @DisplayName("CP-03: Alerta de congelamiento / temperatura bajo cero (< 0.0)")
    void testFreezingTemperatureAlert() {
        String response = processor.process("sensor-01;TEMP;-5.0");
        assertEquals("ALERT;FREEZING_TEMPERATURE;-5.0", response);
    }

    @Test
    @DisplayName("CP-04: Registro y alertas de humedad")
    void testHumidityHandling() {
        // Normal (20.0 <= hum <= 90.0)
        assertEquals("OK;HUMIDITY_RECORDED;60.0", processor.process("sensor-02;HUMIDITY;60.0"));

        // Alerta alta (> 90.0)
        assertEquals("ALERT;HIGH_HUMIDITY;93.0", processor.process("sensor-02;HUMIDITY;93.0"));

        // Alerta baja (< 20.0)
        assertEquals("ALERT;LOW_HUMIDITY;18.0", processor.process("sensor-02;HUMIDITY;18.0"));
    }

    @Test
    @DisplayName("CP-05: Registro y alertas de batería")
    void testBatteryHandling() {
        // Normal (>= 20.0)
        assertEquals("OK;BATTERY_OK;80.0", processor.process("sensor-03;BATTERY;80.0"));

        // Alerta batería baja (< 20.0)
        assertEquals("ALERT;LOW_BATTERY;15.0", processor.process("sensor-03;BATTERY;15.0"));
    }

    @Test
    @DisplayName("CP-06: Manejo de formatos inválidos y errores de parseo")
    void testInvalidFormats() {
        assertEquals("ERROR;INVALID_FORMAT", processor.process(null));
        assertEquals("ERROR;INVALID_FORMAT", processor.process(""));
        assertEquals("ERROR;INVALID_FORMAT", processor.process("   "));
        assertEquals("ERROR;INVALID_FORMAT", processor.process("soloUnTexto"));
        assertEquals("ERROR;INVALID_FORMAT", processor.process("sensor-01;TEMP"));
        assertEquals("ERROR;INVALID_FORMAT", processor.process("sensor-01;TEMP;textoNoNumerico"));
        assertEquals("ERROR;INVALID_FORMAT", processor.process(";;"));
    }

    @Test
    @DisplayName("CP-07: Error por tipo de sensor no soportado")
    void testUnknownSensorType() {
        String response = processor.process("sensor-01;PRESSURE;1013.2");
        assertEquals("ERROR;UNKNOWN_SENSOR_TYPE", response);
    }

    @Test
    @DisplayName("CP-08: Consulta de estado con comando STATUS")
    void testStatusQuery() {
        // Consulta dispositivo inexistente
        assertEquals("ERROR;DEVICE_NOT_FOUND", processor.process("STATUS;sensor-desconocido"));

        // Registrar lectura previa
        processor.process("sensor-42;TEMP;31.0");

        // Consultar dispositivo existente
        assertEquals("STATUS_OK;sensor-42;TEMP;31.0", processor.process("STATUS;sensor-42"));

        // Consulta mal formada
        assertEquals("ERROR;INVALID_FORMAT", processor.process("STATUS"));
        assertEquals("ERROR;INVALID_FORMAT", processor.process("STATUS;"));
    }
}
