package ui;

import service.BaseStationServer;
import service.TelemetryProcessor;

/**
 * Punto de entrada principal para ejecutar la Estación Base (PeerB).
 */
public class Main {
    public static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        TelemetryProcessor processor = new TelemetryProcessor();
        BaseStationServer server = new BaseStationServer(port, processor);

        try {
            server.start();
            System.out.println("==================================================");
            System.out.println("  ESTACIÓN BASE UDP (PeerB) INICIADA CON ÉXITO   ");
            System.out.println("==================================================");
            System.out.println(" Escuchando en el puerto UDP: " + server.getPort());
            System.out.println(" Esperando datagramas de sensores (PeerA)...");
            System.out.println(" Presione Ctrl+C para detener el servidor.\n");

            // Mantener el hilo principal vivo mientras el servidor esté activo
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nCerrando Estación Base UDP...");
                server.stop();
                System.out.println("Servidor detenido.");
            }));

            while (server.isRunning()) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.err.println("Error iniciando la Estación Base: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
