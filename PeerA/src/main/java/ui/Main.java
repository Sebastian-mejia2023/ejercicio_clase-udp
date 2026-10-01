package ui;

import client.SensorClient;
import java.net.SocketTimeoutException;
import java.util.Scanner;

/**
 * Punto de entrada interactivo para el Nodo Sensor (PeerA).
 */
public class Main {

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 5000;
    public static final int DEFAULT_TIMEOUT_MS = 2000;

    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }

        SensorClient client = new SensorClient(host, port, DEFAULT_TIMEOUT_MS);
        Scanner scanner = new Scanner(System.in);

        System.out.println("==================================================");
        System.out.println("     NODO SENSOR IoT (PeerA) - CLIENTE UDP       ");
        System.out.println("==================================================");
        System.out.println(" Servidor destino: " + host + ":" + port);
        System.out.println(" Timeout configurado: " + DEFAULT_TIMEOUT_MS + " ms\n");

        boolean salir = false;
        while (!salir) {
            System.out.println("Seleccione una opción a enviar:");
            System.out.println(" 1. Temperatura normal (24.5 °C)");
            System.out.println(" 2. Alerta de temperatura alta (45.0 °C)");
            System.out.println(" 3. Alerta de congelamiento (-8.0 °C)");
            System.out.println(" 4. Humedad normal (55.0 %)");
            System.out.println(" 5. Alerta de batería baja (12.0 %)");
            System.out.println(" 6. Consultar estado del sensor (STATUS;sensor-01)");
            System.out.println(" 7. Enviar trama personalizada");
            System.out.println(" 0. Salir");
            System.out.print("Opción > ");

            String input = scanner.nextLine().trim();

            try {
                String response = null;
                switch (input) {
                    case "1":
                        System.out.println(">> Enviando: sensor-01;TEMP;24.5");
                        response = client.sendTelemetry("sensor-01", "TEMP", 24.5);
                        break;
                    case "2":
                        System.out.println(">> Enviando: sensor-01;TEMP;45.0");
                        response = client.sendTelemetry("sensor-01", "TEMP", 45.0);
                        break;
                    case "3":
                        System.out.println(">> Enviando: sensor-01;TEMP;-8.0");
                        response = client.sendTelemetry("sensor-01", "TEMP", -8.0);
                        break;
                    case "4":
                        System.out.println(">> Enviando: sensor-01;HUMIDITY;55.0");
                        response = client.sendTelemetry("sensor-01", "HUMIDITY", 55.0);
                        break;
                    case "5":
                        System.out.println(">> Enviando: sensor-01;BATTERY;12.0");
                        response = client.sendTelemetry("sensor-01", "BATTERY", 12.0);
                        break;
                    case "6":
                        System.out.println(">> Consultando estado de sensor-01...");
                        response = client.queryStatus("sensor-01");
                        break;
                    case "7":
                        System.out.print("Ingrese la trama UDP completa (ej. drone-9;TEMP;30.0): ");
                        String customMsg = scanner.nextLine().trim();
                        System.out.println(">> Enviando: " + customMsg);
                        response = client.sendAndReceive(customMsg);
                        break;
                    case "0":
                        salir = true;
                        System.out.println("Finalizando cliente.");
                        continue;
                    default:
                        System.out.println("Opción no válida.");
                        continue;
                }

                if (response != null) {
                    System.out.println("<< Respuesta recibida del servidor: [" + response + "]\n");
                }
            } catch (SocketTimeoutException e) {
                System.err.println("<< [ERROR TIMEOUT] El servidor no respondió dentro de los " + DEFAULT_TIMEOUT_MS + " ms.\n");
            } catch (Exception e) {
                System.err.println("<< [ERROR]: " + e.getMessage() + "\n");
            }
        }
    }
}
