package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Cliente UDP que simula un dispositivo/sensor IoT (PeerA).
 * Envía lecturas de telemetría y consultas de estado a la Estación Base (PeerB),
 * esperando una respuesta dentro de una ventana de tiempo (timeout).
 */
public class SensorClient {

    private final String serverHost;
    private final int serverPort;
    private final int timeoutMs;

    public SensorClient(String serverHost, int serverPort, int timeoutMs) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Envía una lectura de telemetría al servidor y espera la confirmación o alerta.
     * 
     * @param deviceId Identificador único del dispositivo (ej. "sensor-01").
     * @param sensorType Tipo de sensor ("TEMP", "HUMIDITY", "BATTERY").
     * @param value Valor numérico de la lectura.
     * @return Respuesta recibida de la estación base.
     * @throws IOException Si ocurre un error de red o timeout.
     */
    public String sendTelemetry(String deviceId, String sensorType, double value) throws IOException {
        String message = deviceId + ";" + sensorType + ";" + value;
        return sendAndReceive(message);
    }

    /**
     * Consulta el último estado registrado de un dispositivo en la estación base.
     * 
     * @param deviceId Identificador del dispositivo a consultar.
     * @return Respuesta de estado recibida del servidor.
     * @throws IOException Si ocurre un error de red o timeout.
     */
    public String queryStatus(String deviceId) throws IOException {
        String message = "STATUS;" + deviceId;
        return sendAndReceive(message);
    }

    /**
     * Envía un mensaje en texto plano a través de UDP y espera la respuesta del servidor.
     * 
     * @param message Cadena de texto a transmitir.
     * @return Cadena de texto recibida en la respuesta.
     * @throws SocketTimeoutException Si transcurre el tiempo límite sin recibir respuesta.
     * @throws IOException Si ocurre un error en el socket o resolución de red.
     */
    public String sendAndReceive(String message) throws IOException {
        // Paso 3.1: socket con try-with-resources
        try (DatagramSocket socket = new DatagramSocket()) {
            // Paso 3.2: timeout de recepción
            socket.setSoTimeout(this.timeoutMs);

            // Paso 3.3: empaquetar el mensaje hacia el servidor
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
                    InetAddress.getByName(this.serverHost),
                    this.serverPort);

            // Paso 3.4: enviar
            socket.send(packet);

            // Paso 3.5: preparar buffer y paquete de respuesta
            byte[] buffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

            // Paso 3.6: recibir (lanza SocketTimeoutException si vence el timeout)
            socket.receive(responsePacket);

            // Paso 3.7: decodificar respetando offset y length
            return new String(
                    responsePacket.getData(),
                    responsePacket.getOffset(),
                    responsePacket.getLength(),
                    StandardCharsets.UTF_8).trim();
        }
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }
}
