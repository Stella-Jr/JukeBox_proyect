const express = require('express');
const http = require('http');
const cors = require('cors');
const axios = require('axios');
const { Server } = require('socket.io');

const app = express();
const server = http.createServer(app);

const INTERNAL_API_KEY = process.env.INTERNAL_API_KEY;
const roomHosts = new Map();

const log = (message, meta = '') => {
  const timestamp = new Date().toISOString();
  console.log(`[${timestamp}] ${message}${meta ? ` | ${meta}` : ''}`);
};

app.use(cors());
app.use(express.json());

app.get('/', (req, res) => {
  res.send('Realtime service is running');
});

const validateInternalRequest = (req, res, next) => {
  const apiKey = req.header('x-api-key');

  if (!INTERNAL_API_KEY) {
    log('INTERNAL_API_KEY no configurada, denegando acceso interno');
    return res.status(500).json({ error: 'Internal server configuration missing.' });
  }

  if (!apiKey || apiKey !== INTERNAL_API_KEY) {
    log('Acceso no autorizado a /internal/notify', `clientIp=${req.ip}`);
    return res.status(403).json({ error: 'Forbidden. Invalid API key.' });
  }

  return next();
};

app.post('/internal/notify', validateInternalRequest, (req, res) => {
  const { roomId, event, payload } = req.body;

  if (!roomId || typeof roomId !== 'string' || !roomId.trim()) {
    return res.status(400).json({ error: 'roomId es obligatorio y debe ser un string no vacío.' });
  }

  if (!event || typeof event !== 'string' || !event.trim()) {
    return res.status(400).json({ error: 'event es obligatorio y debe ser un string no vacío.' });
  }

  if (payload === undefined || payload === null || typeof payload !== 'object' || Array.isArray(payload)) {
    return res.status(400).json({ error: 'payload es obligatorio y debe ser un objeto válido.' });
  }

  log('Notificación interna recibida', `roomId=${roomId} event=${event}`);
  io.to(roomId).emit(event, payload);

  return res.status(200).json({ success: true, roomId, event });
});

const io = new Server(server, {
  cors: {
    origin: '*',
  },
});

io.on('connection', (socket) => {
  log('Socket conectado', `socketId=${socket.id}`);

  socket.on('join_room', ({ roomId, role }) => {
    if (!roomId || typeof roomId !== 'string' || !roomId.trim()) {
      log('join_room inválido', `socketId=${socket.id}`);
      return socket.emit('error', { message: 'roomId es obligatorio para unirse a una sala.' });
    }

    socket.join(roomId);
    socket.data.roomId = roomId;
    socket.data.role = role || 'guest';

    if (role === 'host') {
      roomHosts.set(roomId, socket.id);
      log('Host registrado', `roomId=${roomId} hostId=${socket.id}`);
    }

    log('Socket se unió a la sala', `socketId=${socket.id} roomId=${roomId} role=${socket.data.role}`);
  });

  socket.on('request_sync', ({ roomId }) => {
    if (!roomId || typeof roomId !== 'string' || !roomId.trim()) {
      log('request_sync inválido', `socketId=${socket.id}`);
      return socket.emit('sync_error', { message: 'roomId es obligatorio para request_sync.' });
    }

    const hostId = roomHosts.get(roomId);

    if (!hostId) {
      log('Host no encontrado para request_sync', `socketId=${socket.id} roomId=${roomId}`);
      return socket.emit('sync_error', { message: 'No hay host registrado para esta sala.' });
    }

    if (hostId === socket.id) {
      log('request_sync ignorado porque el host solicitó sync sobre sí mismo', `socketId=${socket.id} roomId=${roomId}`);
      return;
    }

    log('Solicitud de sync enviada al host', `requesterId=${socket.id} hostId=${hostId} roomId=${roomId}`);
    io.to(hostId).emit('request_sync', {
      requesterId: socket.id,
      roomId,
    });
  });

  socket.on('disconnect', (reason) => {
    const rooms = Array.from(socket.rooms).filter((room) => room !== socket.id);

    rooms.forEach((roomId) => {
      if (roomHosts.get(roomId) === socket.id) {
        roomHosts.delete(roomId);
        log('Host desconectado y eliminado del mapeo', `roomId=${roomId} hostId=${socket.id}`);
      }
    });

    log('Socket desconectado', `socketId=${socket.id} rooms=${rooms.join(', ') || 'ninguna'} reason=${reason}`);
  });
});

const port = process.env.PORT || 3000;
server.listen(port, () => {
  log('Realtime service listening on port', port);
});
