import { useEffect, useMemo, useRef, useState } from 'react'
import { io } from 'socket.io-client'
import { useYouTubeIFramePlayer } from './hooks/useYouTubeIFramePlayer'

const SERVER_BASE = 'http://localhost:8080/api'
const SOCKET_URL = 'http://localhost:3000'

type QueueItem = {
  id?: number
  title?: string
  artist?: string
  ytId?: string
  thumbnail?: string
  votes?: number
  status?: string
  score?: number
}

type QueueRefreshMessage = QueueItem[] | { queue?: QueueItem[] }

async function fetchQueue(roomId: string): Promise<QueueItem[]> {
  const response = await fetch(`${SERVER_BASE}/queue/${encodeURIComponent(roomId)}`)
  if (!response.ok) return []
  const data = await response.json()
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.queue)) return data.queue
  return []
}

export function HostPage({ roomId }: { roomId: string }) {
  const [queue, setQueue] = useState<QueueItem[]>([])
  const [connected, setConnected] = useState(false)
  const [statusMessage, setStatusMessage] = useState('Inicializando host…')
  const socketRef = useRef<ReturnType<typeof io> | null>(null)
  const advancingRef = useRef(false)

  const nowPlaying = useMemo(() => queue[0] || null, [queue])
  const pendingSongs = useMemo(
    () => queue.slice(1).sort((a, b) => (b.score || 0) - (a.score || 0)),
    [queue],
  )

  const { containerRef, status: playerStatus } = useYouTubeIFramePlayer({
    videoId: nowPlaying?.ytId || null,
    autoplay: true,
    onEnded: async () => {
      if (advancingRef.current) return
      advancingRef.current = true
      setStatusMessage('Terminó la canción. Pidiendo siguiente…')

      try {
        const nextResp = await fetch(`${SERVER_BASE}/queue/next-track/${encodeURIComponent(roomId)}`, {
          method: 'PATCH',
        })
        if (!nextResp.ok) {
          setStatusMessage('No se pudo avanzar a la siguiente canción.')
          return
        }

        const updated = await fetchQueue(roomId)
        setQueue(updated)
        setStatusMessage('Reproduciendo siguiente canción.')
      } catch {
        setStatusMessage('Error al pedir la siguiente canción.')
      } finally {
        advancingRef.current = false
      }
    },
  })

  useEffect(() => {
    if (!roomId) return
    if (socketRef.current) return

    const socket = io(SOCKET_URL, {
      transports: ['websocket'],
    })
    socketRef.current = socket

    socket.on('connect', () => {
      setConnected(true)
      setStatusMessage('Host conectado al realtime service')
      socket.emit('join_room', { roomId, role: 'host' })
    })

    socket.on('disconnect', () => {
      setConnected(false)
      setStatusMessage('Host desconectado del realtime service')
    })

    socket.on('refresh_queue', (message: QueueRefreshMessage) => {
      setStatusMessage('Cola actualizada automáticamente')
      if (Array.isArray(message)) {
        setQueue(message)
        return
      }
      if (message?.queue && Array.isArray(message.queue)) {
        setQueue(message.queue)
      }
    })

    socket.on('connect_error', () => {
      setStatusMessage('No se pudo conectar al realtime service')
    })

    return () => {
      socket.disconnect()
      socketRef.current = null
    }
  }, [roomId])

  useEffect(() => {
    if (!roomId) return
    let cancelled = false

    setStatusMessage('Cargando cola…')
    fetchQueue(roomId)
      .then((data) => {
        if (cancelled) return
        setQueue(data)
        setStatusMessage('Host listo.')
      })
      .catch(() => {
        if (cancelled) return
        setStatusMessage('No se pudo cargar la cola inicial.')
      })

    return () => {
      cancelled = true
    }
  }, [roomId])

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="mx-auto max-w-6xl px-4 py-6 sm:px-6 lg:px-8">
        <header className="mb-6 rounded-3xl border border-slate-800 bg-slate-900/80 p-5 shadow-2xl shadow-cyan-500/10 backdrop-blur">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-sm uppercase tracking-[0.32em] text-emerald-400/80">Jukebox Host</p>
              <h1 className="mt-2 text-3xl font-semibold text-white">Reproductor de sala</h1>
              <p className="mt-1 max-w-2xl text-sm text-slate-400">Reproducción automática con YouTube IFrame + control de cola.</p>
            </div>
            <div className="grid gap-2">
              <span className="rounded-full bg-emerald-500/10 px-3 py-1 text-sm text-emerald-300">
                Socket: {connected ? 'Conectado' : 'Desconectado'}
              </span>
              <span className="rounded-full bg-slate-800/90 px-3 py-1 text-sm text-slate-300">Sala: {roomId}</span>
              <span className="rounded-full bg-slate-800/90 px-3 py-1 text-sm text-slate-300">Player: {playerStatus}</span>
            </div>
          </div>
          {statusMessage && <p className="mt-4 text-sm text-slate-300">{statusMessage}</p>}
        </header>

        <main className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
          <section className="space-y-6">
            <article className="rounded-3xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl shadow-cyan-500/10">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm uppercase tracking-[0.32em] text-emerald-400">Ahora suena</p>
                  <h2 className="mt-2 text-2xl font-semibold text-white">{nowPlaying?.title || 'Sin reproducción'}</h2>
                  <p className="mt-1 text-sm text-slate-400">{nowPlaying?.artist || (nowPlaying ? 'Artista desconocido' : 'Agrega canciones desde el client')}</p>
                </div>
                <div className="rounded-3xl border border-emerald-500/20 bg-emerald-500/5 px-4 py-3 text-sm text-emerald-200">
                  {(nowPlaying?.votes ?? 0).toString()} votos
                </div>
              </div>

              <div className="mt-6 overflow-hidden rounded-[2rem] border border-slate-800 bg-slate-950">
                <div className="aspect-video w-full">
                  <div ref={containerRef} className="h-full w-full" />
                </div>
              </div>

              <div className="mt-4 flex flex-wrap gap-3 text-sm text-slate-400">
                <span className="rounded-full bg-slate-800 px-3 py-2">Autoplay: ON</span>
                <span className="rounded-full bg-slate-800 px-3 py-2">Avance: onStateChange === 0</span>
                <span className="rounded-full bg-slate-800 px-3 py-2">Endpoint: PATCH /api/queue/next-track/{'{roomId}'}</span>
              </div>
            </article>
          </section>

          <aside className="space-y-6">
            <div className="rounded-3xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl shadow-cyan-500/10">
              <div className="mb-4 flex items-center justify-between gap-4">
                <div>
                  <p className="text-sm uppercase tracking-[0.32em] text-emerald-400">Próximas canciones</p>
                  <h3 className="mt-2 text-xl font-semibold text-white">En cola</h3>
                </div>
                <span className="rounded-full bg-slate-800 px-3 py-1 text-sm text-slate-300">{pendingSongs.length} pendientes</span>
              </div>

              <div className="space-y-4">
                {pendingSongs.length > 0 ? (
                  pendingSongs.map((song, index) => {
                    const key = song.id ?? `${song.ytId ?? song.title ?? 'song'}-${index}`
                    return (
                      <div key={key} className="rounded-3xl border border-slate-800 bg-slate-950 px-4 py-4">
                        <div className="flex items-start gap-3">
                          <img
                            src={song.thumbnail || 'https://via.placeholder.com/90x60?text=Cover'}
                            alt={song.title || 'thumb'}
                            className="h-14 w-24 rounded-2xl object-cover"
                          />
                          <div className="min-w-0 flex-1">
                            <p className="truncate font-semibold text-slate-100">{song.title || 'Sin título'}</p>
                            <p className="mt-1 truncate text-sm text-slate-400">{song.artist || 'Artista desconocido'}</p>
                            <div className="mt-3 flex items-center justify-between">
                              <span className="rounded-full bg-slate-800 px-3 py-1 text-xs text-slate-300">{song.status || 'PENDING'}</span>
                              <span className="text-sm font-semibold text-emerald-300">{song.votes ?? 0} votos</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    )
                  })
                ) : (
                  <div className="rounded-3xl border border-dashed border-slate-700 bg-slate-950 p-6 text-center text-slate-500">
                    No hay canciones pendientes.
                  </div>
                )}
              </div>
            </div>
          </aside>
        </main>
      </div>
    </div>
  )
}

