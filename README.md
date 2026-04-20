# Jukebox Project - Guía de Instalación y Uso

## Estructura del proyecto

- **Backend/**: API en Spring Boot (Java) para lógica de negocio y búsqueda de canciones.
- **realtime-service/**: Microservicio Node.js (Express + Socket.io) para comunicación en tiempo real entre backend y clientes.
- **frontend-client/**: Web app para usuarios (React + Tailwind CSS). Permite buscar, agregar y votar canciones.
- **host-player/**: Web app para el host (PC del gimnasio).

## Instalación rápida (Windows)

1. Abre PowerShell en la carpeta raíz del proyecto.
2. Ejecuta el script de instalación:

```powershell
./setup-all.ps1
```

Esto instalará todas las dependencias necesarias en cada carpeta.

## ¿Qué hace cada módulo?

### Backend (Spring Boot)
- Expone endpoints REST para buscar canciones (`/api/songs/search`), manejar la cola y votos.
- Debe estar corriendo para que el frontend y el realtime-service funcionen correctamente.

### realtime-service (Node.js)
- Recibe notificaciones del backend vía `POST /internal/notify`.
- Usa Socket.io para enviar eventos en tiempo real a los clientes conectados a cada sala.
- Requiere definir la variable de entorno `INTERNAL_API_KEY` antes de iniciar.

### frontend-client (React)
- Permite a los usuarios ingresar a una sala, buscar canciones, agregarlas a la cola y votar.
- Se conecta automáticamente al realtime-service para recibir actualizaciones en vivo.
- Usa Tailwind CSS para un diseño moderno y responsivo.

### host-player (React)
- Pensado para el PC del gimnasio que reproduce la música.
- Puede compartir lógica con frontend-client.

## ¿Cómo inicio cada módulo?

### Backend

```powershell
cd Backend
./mvnw spring-boot:run
```

### realtime-service

```powershell
cd realtime-service
$env:INTERNAL_API_KEY = "pon-tu-api-key"
npm start
```

### frontend-client

```powershell
cd frontend-client
npm run dev
```

### host-player

```powershell
cd host-player
npm run dev
```