import * as AppModule from '../App.jsx'
import { HostPage } from '../views/host/HostPage'

function getHostRoomIdFromPathname(pathname: string): string | null {
  const normalized = pathname.replace(/\/+$/, '')
  const match = normalized.match(/^\/host\/([^/]+)$/)
  if (!match) return null
  return decodeURIComponent(match[1])
}

export default function Root() {
  const roomId = getHostRoomIdFromPathname(window.location.pathname)
  if (roomId) return <HostPage roomId={roomId} />
  const App = (AppModule as any).default ?? AppModule
  return <App />
}

