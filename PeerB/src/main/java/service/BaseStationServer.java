package service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * Servidor UDP de la Estación Base (PeerB).
 * Escucha paquetes de telemetría de sensores, los procesa y envía respuestas
 * de vuelta al remitente.
 */
public class BaseStationServer {

    private final int port;
    private final TelemetryProcessor processor;
    private DatagramSocket socket;
    private volatile boolean running = false;
    private Thread listenerThread;

    public BaseStationServer(int port, TelemetryProcessor processor) {
        this.port = port;
        this.processor = processor;
    }

    /**
     * Inicia el servidor en un hilo secundario para escuchar datagramas entrantes.
     */
    public synchronized void start() throws SocketException {
        if (running) {
            return;
        }

        this.socket = new DatagramSocket(this.port);
        this.running = true;

        this.listenerThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (running && !socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    handlePacket(socket, packet);
                } catch (SocketException e) {
                    // Socket cerrado intencionalmente al detener el servidor
                    if (!running) {
                        break;
                    }
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error procesando datagrama: " + e.getMessage());
                    }
                }
            }
        }, "BaseStationServer-Thread");

        this.listenerThread.start();
    }

    /**
     * Procesa un datagrama UDP recibido y envía la respuesta al remitente original.
     * 
     * @param socket Socket UDP activo.
     * @param packet Paquete recibido con datos, IP y puerto del emisor.
     * @throws IOException Si ocurre un error de red al responder.
     */
    public void handlePacket(DatagramSocket socket, DatagramPacket packet) throws IOException {
        // TODO Paso 2.1: Convertir los bytes del paquete recibido a un String usando UTF-8.
        // ¡Importante!: Usar packet.getOffset() y packet.getLength() para leer únicamente
        // los bytes válidos del paquete y no la totalidad del buffer:
        // String message = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);

        // TODO Paso 2.2: Procesar el mensaje con el TelemetryProcessor para obtener la respuesta:
        // String response = this.processor.process(message);

        // TODO Paso 2.3: Convertir la respuesta a bytes (UTF-8) y construir el DatagramPacket de respuesta
        // dirigido al remitente (packet.getAddress() y packet.getPort()).

        // TODO Paso 2.4: Enviar el paquete de respuesta a través del socket usando socket.send(...).
    }

    /**
     * Detiene el servidor y libera el socket.
     */
    public synchronized void stop() {
        this.running = false;
        if (this.socket != null && !this.socket.isClosed()) {
            this.socket.close();
        }
        if (this.listenerThread != null) {
            try {
                this.listenerThread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Devuelve el puerto local en el que está escuchando el socket
     * (útil si se inicializó con puerto 0 para pruebas).
     */
    public int getPort() {
        return (socket != null && !socket.isClosed()) ? socket.getLocalPort() : port;
    }

    public TelemetryProcessor getProcessor() {
        return processor;
    }
}
