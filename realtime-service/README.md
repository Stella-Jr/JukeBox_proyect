# Realtime Service - Jukebox Project

Este microservicio actúa como puente de comunicación en tiempo real entre:
- el backend Spring Boot,
- los clientes web de usuario,
- el Host PC.

El backend envía eventos a este servicio y este servicio los envía a los clientes dentro de la misma sala (`roomId`).

---

## 1. Pasos para iniciar el servicio

1. Abre una terminal en `realtime-service`.
2. Instala dependencias:

```bash
npm install
```

3. Define la variable de entorno obligatoria `INTERNAL_API_KEY`.

En Windows PowerShell:

```powershell
$env:INTERNAL_API_KEY = "tu_api_key_secreta"
```

Si quieres que sea permanente, puedes usar:

```powershell
setx INTERNAL_API_KEY "tu_api_key_secreta"
```

4. Inicia el servicio:

```bash
npm start
```

5. Verifica que el servicio esté corriendo:

```bash
curl http://localhost:3000/
```

Deberías ver: `Realtime service is running`

---

## 2. Qué debe hacer el backend Spring Boot

### 2.1. Objetivo principal

Cada vez que ocurra un evento importante para una sala, el backend Spring Boot debe llamar a este endpoint:

- `POST http://localhost:3000/internal/notify`

### 2.2. Qué enviar

El backend debe enviar un JSON con:

- `roomId`: el identificador de la sala.
- `event`: nombre del evento que debe llegar a los clientes.
- `payload`: los datos que el cliente necesita para actualizar su estado.

Ejemplo:

```json
{
  "roomId": "room123",
  "event": "queue_updated",
  "payload": {
    "song": "Never Gonna Give You Up",
    "position": 4
  }
}
```

### 2.3. Headers obligatorios

El backend debe enviar:

- `Content-Type: application/json`
- `x-api-key: <INTERNAL_API_KEY>`

Si el header `x-api-key` no coincide con el valor en `INTERNAL_API_KEY`, el servicio responderá `403 Forbidden`.

---

## 3. Ejemplo de petición desde Spring Boot

### 3.1. Usando `RestTemplate`

```java
RestTemplate restTemplate = new RestTemplate();

HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
headers.set("x-api-key", "tu_api_key_secreta");

Map<String, Object> payload = Map.of(
    "song", "Never Gonna Give You Up",
    "position", 4
);

Map<String, Object> body = Map.of(
    "roomId", "room123",
    "event", "queue_updated",
    "payload", payload
);

HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

ResponseEntity<String> response = restTemplate.postForEntity(
    "http://localhost:3000/internal/notify",
    request,
    String.class
);

System.out.println("Status: " + response.getStatusCode());
System.out.println("Body: " + response.getBody());
```

### 3.2. Usando `WebClient`

```java
WebClient webClient = WebClient.builder()
    .baseUrl("http://localhost:3000")
    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    .defaultHeader("x-api-key", "tu_api_key_secreta")
    .build();

Map<String, Object> body = Map.of(
    "roomId", "room123",
    "event", "queue_updated",
    "payload", Map.of("song", "Never Gonna Give You Up", "position", 4)
);

String result = webClient.post()
    .uri("/internal/notify")
    .bodyValue(body)
    .retrieve()
    .bodyToMono(String.class)
    .block();

System.out.println(result);
```

---

## 4. Qué hace este servicio cuando recibe la petición

1. Verifica el header `x-api-key`.
2. Verifica que el body tenga `roomId`, `event` y `payload`.
3. Reenvía el evento a los sockets conectados en esa sala usando Socket.io.
4. Devuelve una respuesta al backend.

---

## 5. Respuestas que puede devolver el servicio

- `200 OK`: todo correcto y el evento se emitió a la sala.
- `400 Bad Request`: falta `roomId`, `event` o `payload`, o alguno está mal formado.
- `403 Forbidden`: `x-api-key` inválido o faltante.
- `500 Internal Server Error`: el servicio no tiene configurada la clave interna.

---

## 6. Uso de Socket.io en los clientes

### 6.1. Evento `join_room`

Cuando un cliente se conecta, debe enviar este evento para unirse a la sala:

```js
socket.emit('join_room', {
  roomId: 'room123',
  role: 'host' // opcional; usar 'host' para el PC Host
});
```

- Si `role` es `host`, este socket queda registrado como Host de la sala.
- Si no se envía `role`, el socket se considera guest.

### 6.2. Evento `request_sync`

Cuando un nuevo cliente necesita sincronizar su estado con el Host:

```js
socket.emit('request_sync', {
  roomId: 'room123'
});
```

- Esta petición se envía únicamente al socket Host de la sala.
- El Host debe responder con el estado actual de la cola.

---

## 7. Pruebas rápidas para el backend junior

1. Asegúrate de que `npm start` esté corriendo en `realtime-service`.
2. Prueba con `curl`:

```bash
curl -X POST http://localhost:3000/internal/notify \
  -H "Content-Type: application/json" \
  -H "x-api-key: tu_api_key_secreta" \
  -d '{"roomId":"room123","event":"queue_updated","payload":{"song":"Never Gonna Give You Up","position":4}}'
```

3. Si ves `200 OK`, el backend está enviando el evento correctamente.
4. Si ves `403`, revisa el valor de `x-api-key` y la variable `INTERNAL_API_KEY`.
5. Si ves `400`, revisa que el JSON tenga todas las propiedades requeridas.

---

## 8. Ejemplo de prueba con Java

Pega este método en cualquier clase de prueba o controlador temporal para validar el flujo:

```java
public void testRealtimeNotify() {
    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-api-key", "tu_api_key_secreta");

    Map<String, Object> body = Map.of(
        "roomId", "room123",
        "event", "queue_updated",
        "payload", Map.of("song", "Never Gonna Give You Up", "position", 4)
    );

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
    ResponseEntity<String> response = restTemplate.postForEntity(
        "http://localhost:3000/internal/notify",
        request,
        String.class
    );

    System.out.println("Status: " + response.getStatusCode());
    System.out.println("Body: " + response.getBody());
}
```

---

## 9. Consejos extras

- Usa siempre la misma `INTERNAL_API_KEY` desde Spring Boot y desde el servicio.
- No cambies el nombre del endpoint: debe ser `/internal/notify`.
- Si no conoces `roomId`, pide al frontend que te confirme el identificador usado.
- No envíes `payload` como texto plano; debe ser un objeto JSON.
