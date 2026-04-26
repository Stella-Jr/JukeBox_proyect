import { useEffect, useMemo, useRef, useState } from 'react'
import { io } from 'socket.io-client'

const STORAGE_KEYS = {
  roomId: 'jukebox_roomId',
  username: 'jukebox_username',
  votedSongs: 'jukebox_votedSongs',
}

const SERVER_BASE = "http://localhost:8080/api";
const SOCKET_URL = 'http://localhost:3000'

function App() {
  const [roomId, setRoomId] = useState('')
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
    const storedRoom = localStorage.getItem(STORAGE_KEYS.roomId)
    const storedUser = localStorage.getItem(STORAGE_KEYS.username)
    const storedVotes = localStorage.getItem(STORAGE_KEYS.votedSongs)

    if (storedRoom && storedUser) {
      setRoomId(storedRoom)
      setUsername(storedUser)
      setStage('dashboard')
      setStatusMessage(`Sesión cargada para ${storedUser}`)
    }

    if (storedVotes) {
      try {
        setVotedSongs(JSON.parse(storedVotes))
      } catch (error) {
        console.warn('No se pudo leer votos guardados', error)
      }
    }
  }, [])

  useEffect(() => {
    if (stage !== 'dashboard') return
    if (!roomId || !username) return
    if (socketRef.current) return

    const socket = io(SOCKET_URL, {
      transports: ['websocket'],
    })

    socketRef.current = socket

    socket.on('connect', () => {
      setConnected(true)
      setStatusMessage('Conectado al realtime service')
      socket.emit('join_room', {
        roomId,
        role: 'guest',
      })
    })

    socket.on('disconnect', () => {
      setConnected(false)
      setStatusMessage('Desconectado del realtime service')
    })

    socket.on('refresh_queue', (message) => {
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
  }, [stage, roomId, username])

  useEffect(() => {
    if (stage !== 'dashboard') return
    if (!roomId) return

    const fetchQueue = async () => {
      try {
        const response = await fetch(`${SERVER_BASE}/queue/${roomId}`)
        if (!response.ok) return
        const data = await response.json()
        if (Array.isArray(data)) {
          setQueue(data)
        } else if (Array.isArray(data?.queue)) {
          setQueue(data.queue)
        }
      } catch (error) {
        console.warn('No se pudo cargar la cola inicial', error)
      }
    }

    fetchQueue()
  }, [stage, roomId])

  const saveSession = () => {
    localStorage.setItem(STORAGE_KEYS.roomId, roomId)
    localStorage.setItem(STORAGE_KEYS.username, username)
    setStage('dashboard')
    setStatusMessage('Bienvenido, conectando...')
  }

  const handleLogin = async (event) => {
    event.preventDefault()

    if (!roomId.trim() || !username.trim()) {
        setStatusMessage('Completa código de sala y nombre de usuario.')
        return
    }

    try {
        const response = await fetch(`${SERVER_BASE}/rooms/join`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ code: roomId, username })
        })

        if (!response.ok) throw new Error('No se pudo unir a la sala')

        const data = await response.json()
        
        localStorage.setItem('jukebox_userId', data.userId)
        localStorage.setItem('jukebox_roomDbId', data.roomId)
        localStorage.setItem(STORAGE_KEYS.roomId, roomId)
        localStorage.setItem(STORAGE_KEYS.username, username)
        
        setStage('dashboard')
        setStatusMessage(`Bienvenido ${username}`)
    } catch (error) {
        setStatusMessage('No se pudo conectar. Verificá el código de sala.')
    }
}

  const handleSearch = async (event) => {
    event.preventDefault()
    const query = searchQuery.trim()
    if (!query) return

    setLoadingSearch(true)
    setStatusMessage('Buscando canciones...')

    try {
      const response = await fetch(`${SERVER_BASE}/songs/search?q=${encodeURIComponent(query)}`)
      if (!response.ok) {
        throw new Error('Error en la búsqueda')
      }

      const data = await response.json()
      const results = Array.isArray(data)
        ? data
        : Array.isArray(data?.results)
        ? data.results
        : []

      setSearchResults(results)
      setStatusMessage(`${results.length} resultados encontrados`)
    } catch (error) {
      console.error(error)
      setStatusMessage('No se pudo buscar canciones. Reintenta.')
    } finally {
      setLoadingSearch(false)
    }
  }

 const handleAddSong = async (song) => {
  setStatusMessage('Agregando canción...')

  try {
    await fetch(`${SERVER_BASE}/queue/add`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        roomId: Number(roomId),
        userId: 1,
        ytId: song.ytId,
        title: song.title,
        artist: song.artist,
        thumb: song.thumb
      }),
    })

    setStatusMessage('Canción enviada. Espera la actualización en vivo.')

  } catch (error) {
    console.error(error)
    setStatusMessage('No se pudo agregar la canción. Comprueba el backend.')
  }
}

 const handleVote = async (queueItemId) => {
    const userId = localStorage.getItem('jukebox_userId')
    if (!userId) return

    try {
        const response = await fetch(`${SERVER_BASE}/queue/vote/${queueItemId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: Number(userId) })
        })

        if (response.ok) {
            setStatusMessage('Voto registrado.')
        }
    } catch (error) {
        console.warn('Error al votar', error)
    }
}

  const nowPlaying = useMemo(() => queue[0] || null, [queue])
  const pendingSongs = useMemo(
    () => queue.slice(1).sort((a, b) => (b.votes || 0) - (a.votes || 0)),
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
                Busca canciones, agrégalas a la cola y vota para subirlas.
              </p>
            </div>
            <div className="grid gap-2">
              <span className="rounded-full bg-emerald-500/10 px-3 py-1 text-sm text-emerald-300">
                Socket: {connected ? 'Conectado' : 'Desconectado'}
              </span>
              <span className="rounded-full bg-slate-800/90 px-3 py-1 text-sm text-slate-300">
                {stage === 'dashboard' ? `Sala: ${roomId}` : 'Ingresa a una sala'}
              </span>
            </div>
          </div>
          {statusMessage && (
            <p className="mt-4 text-sm text-slate-300">{statusMessage}</p>
          )}
        </header>

        {stage === 'login' ? (
          <main className="space-y-6">
            <section className="rounded-3xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl shadow-cyan-500/10">
              <h2 className="text-xl font-semibold text-white">Acceso rápido</h2>
              <p className="mt-2 text-sm text-slate-400">
                Ingresa tu Código de Sala y tu Nombre de Usuario para comenzar.
              </p>

              <form className="mt-6 space-y-4" onSubmit={handleLogin}>
                <label className="block text-sm text-slate-300">
                  Código de Sala
                  <input
                    value={roomId}
                    onChange={(event) => setRoomId(event.target.value)}
                    className="mt-2 w-full rounded-3xl border border-slate-800 bg-slate-950 px-4 py-3 text-white outline-none transition focus:border-emerald-400/80 focus:ring-2 focus:ring-emerald-500/20"
                    placeholder="Ej. gym-room-01"
                  />
                </label>

                <label className="block text-sm text-slate-300">
                  Nombre de Usuario
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

            <section className="rounded-3xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl shadow-cyan-500/10">
              <h2 className="text-lg font-semibold text-white">Guía rápida</h2>
              <ul className="mt-4 space-y-3 text-sm text-slate-400">
                <li>1. Escribe el código de sala compartido por el Host.</li>
                <li>2. Usa el buscador para encontrar canciones de YouTube.</li>
                <li>3. Agrega canciones a la cola y vota por tus favoritas.</li>
              </ul>
            </section>
          </main>
        ) : (
          <main className="space-y-6">
            <section className="rounded-3xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl shadow-cyan-500/10">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm uppercase tracking-[0.32em] text-slate-400">Tu sala</p>
                  <h2 className="mt-2 text-2xl font-semibold text-white">{roomId}</h2>
                  <p className="mt-1 text-sm text-slate-500">Usuario: {username}</p>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    localStorage.removeItem(STORAGE_KEYS.roomId)
                    localStorage.removeItem(STORAGE_KEYS.username)
                    setStage('login')
                    setRoomId('')
                    setUsername('')
                    setQueue([])
                    socketRef.current?.disconnect()
                    socketRef.current = null
                  }}
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
                      <p className="text-sm text-emerald-400">Buscar canción</p>
                      <h2 className="mt-2 text-xl font-semibold text-white">Encuentra la próxima pista</h2>
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
                      placeholder="Buscar canción..."
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
                      {searchResults.map((song) => {
                        const songId = song.id || song.videoId || song.title
                        return (
                          <div
                            key={songId}
                            className="flex flex-col gap-3 rounded-3xl border border-slate-800 bg-slate-950 p-4 sm:flex-row sm:items-center sm:justify-between"
                          >
                            <div className="flex items-center gap-3">
                              <img
                                src={song.thumbnail || song.image || 'https://via.placeholder.com/100x60?text=Cover'}
                                alt={song.title}
                                className="h-16 w-28 rounded-2xl object-cover"
                              />
                              <div>
                                <p className="font-semibold text-slate-100">{song.title}</p>
                                <p className="text-sm text-slate-400">{song.artist || song.channel || 'Artista desconocido'}</p>
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
                        )
                      })}
                    </div>
                  )}
                </article>

                <article className="rounded-3xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl shadow-cyan-500/10">
                  <div className="mb-4 flex items-center justify-between gap-4">
                    <div>
                      <p className="text-sm uppercase tracking-[0.32em] text-emerald-400">Cola de reproducción</p>
                      <h2 className="mt-2 text-xl font-semibold text-white">Sonando Ahora</h2>
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
                            src={nowPlaying.thumbnail || nowPlaying.image || 'https://via.placeholder.com/110x70?text=Now+Playing'}
                            alt={nowPlaying.title}
                            className="h-24 w-36 rounded-3xl object-cover"
                          />
                          <div>
                            <p className="text-sm uppercase tracking-[0.24em] text-emerald-300">Ahora suena</p>
                            <h3 className="mt-2 text-xl font-semibold text-white">{nowPlaying.title}</h3>
                            <p className="mt-1 text-sm text-slate-400">{nowPlaying.artist || nowPlaying.channel || 'Artista desconocido'}</p>
                          </div>
                        </div>
                        <div className="rounded-3xl border border-emerald-500/20 bg-emerald-500/5 px-4 py-3 text-sm text-emerald-200">
                          {nowPlaying.votes ?? 0} votos
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="rounded-[2rem] border border-dashed border-slate-700 bg-slate-950 p-8 text-center text-slate-400">
                      No hay ninguna canción sonando ahora. Busca y agrega una pista.
                    </div>
                  )}
                </article>
              </div>

              <aside className="space-y-6">
                <div className="rounded-3xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl shadow-cyan-500/10">
                  <h2 className="text-xl font-semibold text-white">Pendientes</h2>
                  <p className="mt-2 text-sm text-slate-400">Las canciones suben de posición según los votos.</p>

                  <div className="mt-6 space-y-4">
                    {pendingSongs.length > 0 ? (
                      pendingSongs.map((song, index) => {
                        const songId = song.id || song.videoId || `${song.title}-${index}`
                        const voted = Boolean(votedSongs[songId])

                        return (
                          <div
                            key={songId}
                            className="rounded-3xl border border-slate-800 bg-slate-950 px-4 py-4"
                          >
                            <div className="flex items-start justify-between gap-3">
                              <div>
                                <p className="font-semibold text-slate-100">{song.title}</p>
                                <p className="mt-1 text-sm text-slate-400">{song.artist || song.channel || 'Artista desconocido'}</p>
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
                        No hay canciones pendientes. Agrega una desde el buscador.
                      </div>
                    )}
                  </div>
                </div>

                <div className="rounded-3xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl shadow-cyan-500/10">
                  <h3 className="text-lg font-semibold text-white">Atajos</h3>
                  <ul className="mt-4 space-y-3 text-sm text-slate-400">
                    <li>Busca por artista, título o fragmento de YouTube.</li>
                    <li>Presiona Agregar para enviar la canción al backend.</li>
                    <li>Vota una sola vez por canción; el botón cambia de estado.</li>
                    <li>La lista se actualiza automáticamente con `refresh_queue`.</li>
                  </ul>
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
