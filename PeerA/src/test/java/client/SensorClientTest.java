package client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class SensorClientTest {

    private DatagramSocket mockServerSocket;
    private Thread mockServerThread;
    private final AtomicBoolean mockRunning = new AtomicBoolean(false);

    @BeforeEach
    void setUp() throws Exception {
        // Mock server escucha en un puerto efímero asignado por el SO
        mockServerSocket = new DatagramSocket(0);
        mockRunning.set(true);
    }

    @AfterEach
    void tearDown() {
        mockRunning.set(false);
        if (mockServerSocket != null && !mockServerSocket.isClosed()) {
            mockServerSocket.close();
        }
        if (mockServerThread != null) {
            try {
                mockServerThread.join(1000);
            } catch (InterruptedException ignored) {}
        }
    }

    @Test
    @DisplayName("CL-01: El cliente envía telemetría formateada y recibe respuesta")
    void testSendTelemetrySuccess() throws IOException {
        int mockPort = mockServerSocket.getLocalPort();

        // Arrancar mock responder que devuelve un ACK predefinido
        mockServerThread = new Thread(() -> {
            try {
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                mockServerSocket.receive(packet);

                String received = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                assertEquals("sensor-x;TEMP;22.5", received);

                // Responder
                byte[] responseBytes = "OK;TEMP_RECORDED;22.5".getBytes(StandardCharsets.UTF_8);
                DatagramPacket responsePacket = new DatagramPacket(
                        responseBytes, responseBytes.length, packet.getAddress(), packet.getPort());
                mockServerSocket.send(responsePacket);
            } catch (IOException ignored) {}
        });
        mockServerThread.start();

        SensorClient client = new SensorClient("127.0.0.1", mockPort, 2000);
        String response = client.sendTelemetry("sensor-x", "TEMP", 22.5);

        assertEquals("OK;TEMP_RECORDED;22.5", response);
    }

    @Test
    @DisplayName("CL-02: El cliente consulta estado con comando STATUS")
    void testQueryStatusSuccess() throws IOException {
        int mockPort = mockServerSocket.getLocalPort();

        mockServerThread = new Thread(() -> {
            try {
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                mockServerSocket.receive(packet);

                String received = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                assertEquals("STATUS;sensor-x", received);

                byte[] responseBytes = "STATUS_OK;sensor-x;TEMP;22.5".getBytes(StandardCharsets.UTF_8);
                DatagramPacket responsePacket = new DatagramPacket(
                        responseBytes, responseBytes.length, packet.getAddress(), packet.getPort());
                mockServerSocket.send(responsePacket);
            } catch (IOException ignored) {}
        });
        mockServerThread.start();

        SensorClient client = new SensorClient("127.0.0.1", mockPort, 2000);
        String response = client.queryStatus("sensor-x");

        assertEquals("STATUS_OK;sensor-x;TEMP;22.5", response);
    }

    @Test
    @DisplayName("CL-03: El cliente lanza SocketTimeoutException cuando el servidor no responde")
    void testTimeoutWhenServerDoesNotReply() {
        int mockPort = mockServerSocket.getLocalPort();

        // El mock server recibe el paquete pero NUNCA responde (simula caída o pérdida de paquete)
        mockServerThread = new Thread(() -> {
            try {
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                mockServerSocket.receive(packet);
                // No responde intencionalmente
            } catch (IOException ignored) {}
        });
        mockServerThread.start();

        int timeoutMs = 400;
        SensorClient client = new SensorClient("127.0.0.1", mockPort, timeoutMs);

        long start = System.currentTimeMillis();
        assertThrows(SocketTimeoutException.class, () -> {
            client.sendTelemetry("sensor-timeout", "TEMP", 30.0);
        }, "Debe lanzar SocketTimeoutException al transcurrir el timeout.");

        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= timeoutMs, "El tiempo transcurrido debe ser al menos el timeout configurado.");
    }
}
