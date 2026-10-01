package service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class BaseStationServerTest {

    private BaseStationServer server;
    private TelemetryProcessor processor;

    @BeforeEach
    void setUp() throws Exception {
        processor = new TelemetryProcessor();
        // Puerto 0 para que el sistema operativo asigne automáticamente un puerto libre
        server = new BaseStationServer(0, processor);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    @DisplayName("IT-01: El servidor recibe datagrama UDP y responde correctamente al remitente")
    void testServerReceivesAndRepliesViaUdp() throws IOException {
        int serverPort = server.getPort();
        assertTrue(serverPort > 0, "El servidor debe tener asignado un puerto válido.");

        try (DatagramSocket testClientSocket = new DatagramSocket()) {
            testClientSocket.setSoTimeout(3000);

            String requestMsg = "drone-01;TEMP;45.0";
            byte[] sendData = requestMsg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket sendPacket = new DatagramPacket(
                    sendData,
                    sendData.length,
                    InetAddress.getByName("127.0.0.1"),
                    serverPort
            );

            // Enviar datagrama al servidor
            testClientSocket.send(sendPacket);

            // Esperar y recibir la respuesta del servidor
            byte[] recvBuffer = new byte[1024];
            DatagramPacket recvPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
            testClientSocket.receive(recvPacket);

            String responseMsg = new String(
                    recvPacket.getData(),
                    recvPacket.getOffset(),
                    recvPacket.getLength(),
                    StandardCharsets.UTF_8
            );

            assertEquals("ALERT;HIGH_TEMPERATURE;45.0", responseMsg);
        }
    }

    @Test
    @DisplayName("IT-02: El servidor procesa múltiples solicitudes de diferentes dispositivos")
    void testServerMultipleRequests() throws IOException {
        int serverPort = server.getPort();

        try (DatagramSocket testClientSocket = new DatagramSocket()) {
            testClientSocket.setSoTimeout(3000);

            // Primera solicitud: registrar telemetría
            String msg1 = "sensor-A;BATTERY;10.0";
            byte[] bytes1 = msg1.getBytes(StandardCharsets.UTF_8);
            testClientSocket.send(new DatagramPacket(bytes1, bytes1.length, InetAddress.getByName("127.0.0.1"), serverPort));

            byte[] buf1 = new byte[1024];
            DatagramPacket packet1 = new DatagramPacket(buf1, buf1.length);
            testClientSocket.receive(packet1);
            String res1 = new String(packet1.getData(), packet1.getOffset(), packet1.getLength(), StandardCharsets.UTF_8);
            assertEquals("ALERT;LOW_BATTERY;10.0", res1);

            // Segunda solicitud: consultar status del sensor recién registrado
            String msg2 = "STATUS;sensor-A";
            byte[] bytes2 = msg2.getBytes(StandardCharsets.UTF_8);
            testClientSocket.send(new DatagramPacket(bytes2, bytes2.length, InetAddress.getByName("127.0.0.1"), serverPort));

            byte[] buf2 = new byte[1024];
            DatagramPacket packet2 = new DatagramPacket(buf2, buf2.length);
            testClientSocket.receive(packet2);
            String res2 = new String(packet2.getData(), packet2.getOffset(), packet2.getLength(), StandardCharsets.UTF_8);
            assertEquals("STATUS_OK;sensor-A;BATTERY;10.0", res2);
        }
    }
}
