import { useEffect, useMemo, useRef, useState } from 'react'
import { io } from 'socket.io-client'

const STORAGE_KEYS = {
  roomCode: 'jukebox_roomCode',
  roomDbId: 'jukebox_roomDbId',
  username: 'jukebox_username',
  sessionToken: 'jukebox_sessionToken',
  votedSongs: 'jukebox_votedSongs',
}

const SERVER_BASE = 'http://localhost:8080/api'
const SOCKET_URL = 'http://localhost:3000'

function readStoredVotes() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.votedSongs) || '{}')
  } catch {
    return {}
  }
}

function App() {
  const [roomCode, setRoomCode] = useState('')
  const [roomDbId, setRoomDbId] = useState('')
  const [username, setUsername] = useState('')
  const [stage, setStage] = useState('login')
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [loadingSearch, setLoadingSearch] = useState(false)
  const [queue, setQueue] = useState([])
  const [statusMessage, setStatusMessage] = useState('')
  const [connected, setConnected] = useState(false)
  const [votedSongs, setVotedSongs] = useState({})
  const socketRef = useRef(null)

  useEffect(() => {
    const storedRoomCode = localStorage.getItem(STORAGE_KEYS.roomCode)
    const storedRoomDbId = localStorage.getItem(STORAGE_KEYS.roomDbId)
    const storedUser = localStorage.getItem(STORAGE_KEYS.username)
    const storedToken = localStorage.getItem(STORAGE_KEYS.sessionToken)

    setVotedSongs(readStoredVotes())

    if (storedRoomCode && storedRoomDbId && storedUser && storedToken) {
      setRoomCode(storedRoomCode)
      setRoomDbId(storedRoomDbId)
      setUsername(storedUser)
      setStage('dashboard')
      setStatusMessage(`Sesion cargada para ${storedUser}`)
    }
  }, [])

  useEffect(() => {
    if (stage !== 'dashboard' || !roomDbId || !username || socketRef.current) return

    const socket = io(SOCKET_URL, { transports: ['websocket'] })
    socketRef.current = socket

    socket.on('connect', () => {
      setConnected(true)
      setStatusMessage('Conectado al realtime service')
      socket.emit('join_room', { roomId: roomDbId, role: 'guest' })
    })

    socket.on('disconnect', () => {
      setConnected(false)
      setStatusMessage('Desconectado del realtime service')
    })

    socket.on('refresh_queue', (message) => {
      setStatusMessage('Cola actualizada automaticamente')
      if (Array.isArray(message)) {
        setQueue(message)
      } else if (Array.isArray(message?.queue)) {
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
  }, [stage, roomDbId, username])

  useEffect(() => {
    if (stage !== 'dashboard' || !roomDbId) return

    fetchQueue()
  }, [stage, roomDbId])

  async function fetchQueue() {
    try {
      const response = await fetch(`${SERVER_BASE}/queue/${roomDbId}`)
      if (!response.ok) return
      const data = await response.json()
      setQueue(Array.isArray(data) ? data : data?.queue || [])
    } catch (error) {
      console.warn('No se pudo cargar la cola', error)
    }
  }

  async function handleLogin(event) {
    event.preventDefault()

    if (!roomCode.trim() || !username.trim()) {
      setStatusMessage('Completa codigo de sala y nombre de usuario.')
      return
    }

    try {
      const response = await fetch(`${SERVER_BASE}/rooms/join`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code: roomCode.trim(), username: username.trim() }),
      })

      if (!response.ok) throw new Error('No se pudo unir a la sala')

      const data = await response.json()
      const nextRoomCode = data.roomCode || roomCode.trim()
      const nextRoomDbId = String(data.roomId)

      localStorage.setItem(STORAGE_KEYS.sessionToken, data.token)
      localStorage.setItem(STORAGE_KEYS.roomDbId, nextRoomDbId)
      localStorage.setItem(STORAGE_KEYS.roomCode, nextRoomCode)
      localStorage.setItem(STORAGE_KEYS.username, username.trim())

      setRoomCode(nextRoomCode)
      setRoomDbId(nextRoomDbId)
      setUsername(username.trim())
      setStage('dashboard')
      setStatusMessage(`Bienvenido ${username.trim()}`)
    } catch {
      setStatusMessage('No se pudo conectar. Verifica el codigo de sala.')
    }
  }

  async function handleSearch(event) {
    event.preventDefault()
    const query = searchQuery.trim()
    if (!query) return

    setLoadingSearch(true)
    setStatusMessage('Buscando canciones...')

    try {
      const response = await fetch(`${SERVER_BASE}/songs/search?q=${encodeURIComponent(query)}`)
      if (!response.ok) throw new Error('Error en la busqueda')
      const data = await response.json()
      const results = Array.isArray(data) ? data : data?.results || []
      setSearchResults(results)
      setStatusMessage(`${results.length} resultados encontrados`)
    } catch (error) {
      console.error(error)
      setStatusMessage('No se pudo buscar canciones. Reintenta.')
    } finally {
      setLoadingSearch(false)
    }
  }

  async function handleAddSong(song) {
    const token = localStorage.getItem(STORAGE_KEYS.sessionToken)
    if (!token || !roomDbId) {
      setStatusMessage('Sesion incompleta. Vuelve a entrar a la sala.')
      return
    }

    setStatusMessage('Agregando cancion...')

    try {
      const response = await fetch(`${SERVER_BASE}/queue/add`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Session-Token': token,
        },
        body: JSON.stringify({
          roomId: Number(roomDbId),
          ytId: song.ytId,
          title: song.title,
          artist: song.artist,
          thumb: song.thumbnail,
        }),
      })

      if (!response.ok) {
        const error = await response.json().catch(() => null)
        throw new Error(error?.message || 'No se pudo agregar la cancion')
      }

      await fetchQueue()
      setStatusMessage('Cancion enviada a la cola.')
    } catch (error) {
      console.error(error)
      setStatusMessage(error.message || 'No se pudo agregar la cancion.')
    }
  }

  async function handleVote(queueItemId) {
    const token = localStorage.getItem(STORAGE_KEYS.sessionToken)
    if (!token) return

    try {
      const response = await fetch(`${SERVER_BASE}/queue/vote/${queueItemId}`, {
        method: 'POST',
        headers: { 'X-Session-Token': token },
      })

      if (!response.ok) {
        const error = await response.json().catch(() => null)
        setStatusMessage(error?.message || 'No se pudo registrar el voto.')
        return
      }

      const nextVotes = { ...votedSongs, [queueItemId]: true }
      setVotedSongs(nextVotes)
      localStorage.setItem(STORAGE_KEYS.votedSongs, JSON.stringify(nextVotes))
      await fetchQueue()
      setStatusMessage('Voto registrado.')
    } catch (error) {
      console.warn('Error al votar', error)
      setStatusMessage('No se pudo registrar el voto.')
    }
  }

  function clearSession() {
    Object.values(STORAGE_KEYS).forEach((key) => localStorage.removeItem(key))
    setStage('login')
    setRoomCode('')
    setRoomDbId('')
    setUsername('')
    setQueue([])
    setVotedSongs({})
    socketRef.current?.disconnect()
    socketRef.current = null
  }

  const nowPlaying = useMemo(() => queue[0] || null, [queue])
  const pendingSongs = useMemo(
    () => queue.slice(1).sort((a, b) => (b.score || 0) - (a.score || 0)),
    [queue],
  )

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="mx-auto max-w-5xl px-4 py-6 sm:px-6 lg:px-8">
        <header className="mb-6 rounded-3xl border border-slate-800 bg-slate-900/80 p-5 shadow-2xl shadow-cyan-500/10 backdrop-blur">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-sm uppercase tracking-[0.32em] text-emerald-400/80">Jukebox User MVP</p>
              <h1 className="mt-2 text-3xl font-semibold text-white">Sala colaborativa</h1>
              <p className="mt-1 max-w-2xl text-sm text-slate-400">
                Busca canciones, agregalas a la cola y vota para subirlas.
              </p>
            </div>
            <div className="grid gap-2">
              <span className="rounded-full bg-emerald-500/10 px-3 py-1 text-sm text-emerald-300">
                Socket: {connected ? 'Conectado' : 'Desconectado'}
              </span>
              <span className="rounded-full bg-slate-800/90 px-3 py-1 text-sm text-slate-300">
                {stage === 'dashboard' ? `Sala: ${roomCode}` : 'Ingresa a una sala'}
              </span>
            </div>
          </div>
          {statusMessage && <p className="mt-4 text-sm text-slate-300">{statusMessage}</p>}
        </header>

        {stage === 'login' ? (
          <main className="space-y-6">
            <section className="rounded-3xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl shadow-cyan-500/10">
              <h2 className="text-xl font-semibold text-white">Acceso rapido</h2>
              <form className="mt-6 space-y-4" onSubmit={handleLogin}>
                <label className="block text-sm text-slate-300">
                  Codigo de sala
                  <input
                    value={roomCode}
                    onChange={(event) => setRoomCode(event.target.value)}
                    className="mt-2 w-full rounded-3xl border border-slate-800 bg-slate-950 px-4 py-3 text-white outline-none transition focus:border-emerald-400/80 focus:ring-2 focus:ring-emerald-500/20"
                    placeholder="Ej. A3F9K2"
                  />
                </label>

                <label className="block text-sm text-slate-300">
                  Nombre de usuario
                  <input
                    value={username}
                    onChange={(event) => setUsername(event.target.value)}
                    className="mt-2 w-full rounded-3xl border border-slate-800 bg-slate-950 px-4 py-3 text-white outline-none transition focus:border-emerald-400/80 focus:ring-2 focus:ring-emerald-500/20"
                    placeholder="Ej. Lucia"
                  />
                </label>

                <button
                  type="submit"
                  className="w-full rounded-3xl bg-gradient-to-r from-emerald-400 to-cyan-400 px-5 py-3 text-sm font-semibold uppercase tracking-[0.16em] text-slate-950 shadow-xl shadow-cyan-500/20 transition hover:-translate-y-0.5 hover:shadow-cyan-500/30"
                >
                  Entrar a la sala
                </button>
              </form>
            </section>
          </main>
        ) : (
          <main className="space-y-6">
            <section className="rounded-3xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl shadow-cyan-500/10">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm uppercase tracking-[0.32em] text-slate-400">Tu sala</p>
                  <h2 className="mt-2 text-2xl font-semibold text-white">{roomCode}</h2>
                  <p className="mt-1 text-sm text-slate-500">Usuario: {username}</p>
                </div>
                <button
                  type="button"
                  onClick={clearSession}
                  className="rounded-3xl border border-slate-700 px-4 py-2 text-sm text-slate-300 transition hover:border-emerald-400 hover:text-emerald-200"
                >
                  Cambiar sala
                </button>
              </div>
            </section>

            <section className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
              <div className="space-y-6">
                <article className="rounded-3xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl shadow-cyan-500/10">
                  <div className="flex items-center justify-between gap-4">
                    <div>
                      <p className="text-sm text-emerald-400">Buscar cancion</p>
                      <h2 className="mt-2 text-xl font-semibold text-white">Encuentra la proxima pista</h2>
                    </div>
                    <span className="rounded-full bg-slate-800 px-3 py-2 text-sm text-slate-300">
                      {connected ? 'Realtime activo' : 'Esperando realtime'}
                    </span>
                  </div>

                  <form className="mt-6 flex flex-col gap-3 sm:flex-row" onSubmit={handleSearch}>
                    <input
                      value={searchQuery}
                      onChange={(event) => setSearchQuery(event.target.value)}
                      className="min-w-0 flex-1 rounded-full border border-slate-800 bg-slate-950 px-4 py-3 text-sm text-white outline-none focus:border-cyan-400 focus:ring-2 focus:ring-cyan-500/20"
                      placeholder="Buscar cancion..."
                    />
                    <button
                      type="submit"
                      className="inline-flex items-center justify-center rounded-full bg-gradient-to-r from-emerald-400 to-cyan-400 px-5 py-3 text-sm font-semibold uppercase tracking-[0.16em] text-slate-950 transition hover:brightness-110"
                    >
                      {loadingSearch ? 'Buscando...' : 'Buscar'}
                    </button>
                  </form>

                  {searchResults.length > 0 && (
                    <div className="mt-6 space-y-4">
                      {searchResults.map((song) => (
                        <div
                          key={song.ytId || song.title}
                          className="flex flex-col gap-3 rounded-3xl border border-slate-800 bg-slate-950 p-4 sm:flex-row sm:items-center sm:justify-between"
                        >
                          <div className="flex items-center gap-3">
                            <img
                              src={song.thumbnail || 'https://via.placeholder.com/100x60?text=Cover'}
                              alt={song.title}
                              className="h-16 w-28 rounded-2xl object-cover"
                            />
                            <div>
                              <p className="font-semibold text-slate-100">{song.title}</p>
                              <p className="text-sm text-slate-400">{song.artist || 'Artista desconocido'}</p>
                            </div>
                          </div>
                          <button
                            type="button"
                            onClick={() => handleAddSong(song)}
                            className="rounded-full bg-emerald-500 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-emerald-400"
                          >
                            Agregar
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </article>

                <article className="rounded-3xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl shadow-cyan-500/10">
                  <div className="mb-4 flex items-center justify-between gap-4">
                    <div>
                      <p className="text-sm uppercase tracking-[0.32em] text-emerald-400">Cola de reproduccion</p>
                      <h2 className="mt-2 text-xl font-semibold text-white">Sonando ahora</h2>
                    </div>
                    <span className="rounded-full bg-slate-800 px-3 py-1 text-sm text-slate-300">
                      {queue.length} canciones
                    </span>
                  </div>

                  {nowPlaying ? (
                    <div className="rounded-[2rem] bg-slate-950 p-5 shadow-inner shadow-cyan-500/10">
                      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                        <div className="flex items-center gap-4">
                          <img
                            src={nowPlaying.thumbnail || 'https://via.placeholder.com/110x70?text=Now+Playing'}
                            alt={nowPlaying.title}
                            className="h-24 w-36 rounded-3xl object-cover"
                          />
                          <div>
                            <p className="text-sm uppercase tracking-[0.24em] text-emerald-300">Ahora suena</p>
                            <h3 className="mt-2 text-xl font-semibold text-white">{nowPlaying.title}</h3>
                            <p className="mt-1 text-sm text-slate-400">{nowPlaying.artist || 'Artista desconocido'}</p>
                          </div>
                        </div>
                        <div className="rounded-3xl border border-emerald-500/20 bg-emerald-500/5 px-4 py-3 text-sm text-emerald-200">
                          {nowPlaying.votes ?? 0} votos
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="rounded-[2rem] border border-dashed border-slate-700 bg-slate-950 p-8 text-center text-slate-400">
                      No hay ninguna cancion sonando ahora. Busca y agrega una pista.
                    </div>
                  )}
                </article>
              </div>

              <aside className="space-y-6">
                <div className="rounded-3xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl shadow-cyan-500/10">
                  <h2 className="text-xl font-semibold text-white">Pendientes</h2>
                  <p className="mt-2 text-sm text-slate-400">Las canciones suben segun votos y tiempo de espera.</p>

                  <div className="mt-6 space-y-4">
                    {pendingSongs.length > 0 ? (
                      pendingSongs.map((song, index) => {
                        const songId = song.id || `${song.title}-${index}`
                        const voted = Boolean(votedSongs[songId])

                        return (
                          <div key={songId} className="rounded-3xl border border-slate-800 bg-slate-950 px-4 py-4">
                            <div className="flex items-start justify-between gap-3">
                              <div>
                                <p className="font-semibold text-slate-100">{song.title}</p>
                                <p className="mt-1 text-sm text-slate-400">{song.artist || 'Artista desconocido'}</p>
                              </div>
                              <div className="text-right">
                                <p className="text-sm text-slate-400">Votos</p>
                                <p className="text-lg font-semibold text-emerald-300">{song.votes ?? 0}</p>
                              </div>
                            </div>
                            <button
                              type="button"
                              onClick={() => handleVote(songId)}
                              disabled={voted}
                              className={`mt-4 inline-flex items-center justify-center rounded-full px-4 py-2 text-sm font-semibold transition ${
                                voted
                                  ? 'cursor-not-allowed bg-slate-700 text-slate-400'
                                  : 'bg-cyan-500 text-slate-950 hover:bg-cyan-400'
                              }`}
                            >
                              {voted ? 'Votado' : 'Votar'}
                            </button>
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
            </section>
          </main>
        )}
      </div>
    </div>
  )
}

export default App
