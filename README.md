# Taller Práctico: Sistema de Telemetría IoT sobre UDP

Bienvenido(a) a la actividad práctica individual de programación de sockets UDP en Java.

- **Tiempo estimado:** 1 hora a 1 hora y media (máximo 2 horas).
- **Modalidad:** Individual.
- **Enfoque:** Desarrollo Guiado por Pruebas (TDD - Test-Driven Development).

---

## 1. Contexto del Problema

Una empresa de monitoreo ambiental despliega sensores autónomos en campo (**PeerA - SensorNode**). Cada nodo recopila mediciones de temperatura, humedad y nivel de batería, y las transmite a una estación base centralizada (**PeerB - BaseStation**) mediante datagramas **UDP**.

La estación base debe procesar cada paquete entrante en tiempo real, validar la coherencia de los datos, almacenar el último estado de cada dispositivo y responder inmediatamente al sensor confirmando la recepción o emitiendo alertas de emergencia (por ejemplo, temperaturas extremas o batería crítica).

Dado que **UDP es un protocolo no orientado a conexión y no confiable**, el sensor debe implementar un mecanismo de tiempo de espera (**timeout**) para no quedarse bloqueado indefinidamente si un paquete se extravía en la red.

---

## 2. Arquitectura del Proyecto

El repositorio está organizado como un proyecto multi-módulo de Gradle:

```text
├── PeerA/                                # Subproyecto Cliente (Nodo Sensor)
│   └── src/
│       ├── main/java/
│       │   ├── client/SensorClient.java  # [TODO Paso 3] Cliente UDP con timeout
│       │   └── ui/Main.java              # Interfaz interactiva de consola
│       └── test/java/
│           └── client/SensorClientTest.java # Pruebas automatizadas del cliente
│
├── PeerB/                                # Subproyecto Servidor (Estación Base)
│   └── src/
│       ├── main/java/
│       │   ├── model/TelemetryData.java  # Modelo de datos de telemetría
│       │   ├── service/TelemetryProcessor.java # [TODO Paso 1] Lógica del protocolo
│       │   ├── service/BaseStationServer.java  # [TODO Paso 2] Servidor UDP
│       │   └── ui/Main.java              # Servidor ejecutable en consola
│       └── test/java/
│           └── service/
│               ├── TelemetryProcessorTest.java # Pruebas unitarias del protocolo
│               └── BaseStationServerTest.java  # Pruebas de integración del socket
│
├── build.gradle                          # Configuración raíz de dependencias y JUnit 5
└── settings.gradle                       # Módulos incluidos (PeerA, PeerB)
```

---

## 3. Especificación del Protocolo de Telemetría

Todos los mensajes se transmiten en cadenas de texto codificadas en **UTF-8**, delimitadas por punto y coma (`;`).

### 3.1. Mensajes del Sensor al Servidor (Peticiones)

| Tipo de Mensaje | Formato de Trama | Ejemplo |
|---|---|---|
| **Registro de Telemetría** | `DEVICE_ID;SENSOR_TYPE;VALUE` | `sensor-01;TEMP;25.5` |
| **Consulta de Estado** | `STATUS;DEVICE_ID` | `STATUS;sensor-01` |

Tipos de sensores admitidos:
- `TEMP`: Temperatura en grados Celsius (°C).
- `HUMIDITY`: Porcentaje de humedad relativa (%).
- `BATTERY`: Porcentaje de batería restante (%).

---

### 3.2. Respuestas de la Estación Base (Servidor al Sensor)

| Caso / Condición | Regla | Formato de Respuesta | Ejemplo de Respuesta |
|---|---|---|---|
| **Temperatura Alta** | `TEMP > 40.0` | `ALERT;HIGH_TEMPERATURE;<valor>` | `ALERT;HIGH_TEMPERATURE;42.5` |
| **Temperatura Bajo Cero** | `TEMP < 0.0` | `ALERT;FREEZING_TEMPERATURE;<valor>` | `ALERT;FREEZING_TEMPERATURE;-5.0` |
| **Temperatura Normal** | `0.0 <= TEMP <= 40.0` | `OK;TEMP_RECORDED;<valor>` | `OK;TEMP_RECORDED;25.0` |
| **Humedad Crítica Alta** | `HUMIDITY > 90.0` | `ALERT;HIGH_HUMIDITY;<valor>` | `ALERT;HIGH_HUMIDITY;93.0` |
| **Humedad Crítica Baja** | `HUMIDITY < 20.0` | `ALERT;LOW_HUMIDITY;<valor>` | `ALERT;LOW_HUMIDITY;18.0` |
| **Humedad Normal** | `20.0 <= HUMIDITY <= 90.0`| `OK;HUMIDITY_RECORDED;<valor>` | `OK;HUMIDITY_RECORDED;60.0` |
| **Batería Crítica** | `BATTERY < 20.0` | `ALERT;LOW_BATTERY;<valor>` | `ALERT;LOW_BATTERY;15.0` |
| **Batería Normal** | `BATTERY >= 20.0` | `OK;BATTERY_OK;<valor>` | `OK;BATTERY_OK;80.0` |
| **Consulta Estado Exitosa**| Dispositivo registrado previamente | `STATUS_OK;<id>;<tipo>;<valor>` | `STATUS_OK;sensor-01;TEMP;25.5`|
| **Dispositivo no Encontrado**| Dispositivo sin lecturas previas | `ERROR;DEVICE_NOT_FOUND` | `ERROR;DEVICE_NOT_FOUND` |
| **Sensor no Soportado** | Tipo diferente a TEMP/HUMIDITY/BATTERY | `ERROR;UNKNOWN_SENSOR_TYPE` | `ERROR;UNKNOWN_SENSOR_TYPE` |
| **Formato Inválido** | Nulo, vacío, campos faltantes o valor no numérico | `ERROR;INVALID_FORMAT` | `ERROR;INVALID_FORMAT` |

---

## 4. Metodología de Trabajo Paso a Paso (TDD)

### Verificación Inicial
Antes de comenzar a programar, verifique el estado inicial del proyecto ejecutando:

```bash
./gradlew test
```

Observará que el proyecto compila correctamente pero las pruebas fallan indicando los métodos pendientes (`TODO`). Su objetivo es lograr que **todas las pruebas pasen (100% verde)**.

---

### Paso 1: Lógica del Protocolo (25 minutos)
- **Archivo a modificar:** `PeerB/src/main/java/service/TelemetryProcessor.java`
- **Tareas:**
  1. Revise los comentarios `// TODO Paso 1.1` a `1.6`.
  2. Implemente la validación de formato, el parseo de campos y las reglas de negocio de la tabla anterior.
  3. Guarde cada lectura válida en el mapa `lastReadings`.
- **Comprobación:**
  ```bash
  ./gradlew :PeerB:test --tests "service.TelemetryProcessorTest"
  ```
  *Debe superar los 8 casos de prueba unitarios (CP-01 a CP-08).*

---

### Paso 2: Servidor UDP de la Estación Base (20 minutos)
- **Archivo a modificar:** `PeerB/src/main/java/service/BaseStationServer.java`
- **Tareas:**
  1. Localice el método `handlePacket(DatagramSocket socket, DatagramPacket packet)`.
  2. Convierta los datos recibidos a `String` utilizando `packet.getOffset()` y `packet.getLength()` con `StandardCharsets.UTF_8`.
  3. Procese el mensaje con `this.processor.process(...)`.
  4. Construya un `DatagramPacket` de respuesta dirigido a la dirección y puerto de origen del emisor (`packet.getAddress()`, `packet.getPort()`).
  5. Envíe la respuesta con `socket.send(...)`.
- **Comprobación:**
  ```bash
  ./gradlew :PeerB:test --tests "service.BaseStationServerTest"
  ```
  *Debe superar las 2 pruebas de integración UDP (IT-01 e IT-02).*

---

### Paso 3: Cliente UDP con Timeout en el Sensor (20 minutos)
- **Archivo a modificar:** `PeerA/src/main/java/client/SensorClient.java`
- **Tareas:**
  1. Localice el método `sendAndReceive(String message)`.
  2. Abra un `DatagramSocket` (preferiblemente en un bloque `try-with-resources`).
  3. Configure el timeout con `socket.setSoTimeout(this.timeoutMs)`.
  4. Empaquete el mensaje en un `DatagramPacket` hacia `InetAddress.getByName(this.serverHost)` y `this.serverPort`.
  5. Envíe el datagrama con `socket.send(...)`.
  6. Reciba la respuesta con `socket.receive(...)` y decodifíquela a `String` respetando offset y length.
- **Comprobación:**
  ```bash
  ./gradlew :PeerA:test
  ```
  *Debe superar las 3 pruebas del cliente (CL-01 a CL-03, incluyendo la verificación de timeout).*

---

### Paso 4: Verificación Integral
Ejecute la suite completa de pruebas:

```bash
./gradlew test
```

Debe obtener el resultado:
```text
BUILD SUCCESSFUL
13 passed tests
```

---

### Paso 5: Prueba Interactiva en Vivo (15 minutos)

Abra dos terminales independientes:

1. **Terminal 1 (Estación Base - PeerB):**
   ```bash
   ./gradlew :PeerB:run --console=plain
   ```
   Verá el mensaje indicando que el servidor está escuchando en el puerto UDP 5000.

2. **Terminal 2 (Nodo Sensor - PeerA):**
   ```bash
   ./gradlew :PeerA:run --console=plain
   ```
   Aparecerá un menú interactivo en consola con opciones para enviar telemetrías normales, alertas y consultas `STATUS`. Observe cómo las respuestas llegan inmediatamente y se imprimen en pantalla.

3. **Prueba de Timeout:**
   Detenga el servidor en la Terminal 1 (Ctrl+C) e intente enviar una opción desde la Terminal 2. Notará que tras 2000 ms el cliente captura `SocketTimeoutException` y muestra el aviso de tiempo de espera agotado.

---

## 5. Criterios de Evaluación

| Componente | Peso | Evidencia |
|---|---|---|
| **Protocolo de Telemetría** | 35% | 8 pruebas de `TelemetryProcessorTest` aprobadas |
| **Servidor UDP BaseStation** | 25% | 2 pruebas de `BaseStationServerTest` aprobadas |
| **Cliente UDP con Timeout** | 25% | 3 pruebas de `SensorClientTest` aprobadas |
| **Demostración Interactiva** | 15% | Ejecución funcional entre terminales sin errores |
| **Total** | **100%** | **13 pruebas automatizadas + Demo en vivo** |

---
