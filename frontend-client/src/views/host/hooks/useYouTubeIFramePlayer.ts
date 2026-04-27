import { useCallback, useEffect, useRef, useState } from 'react'

declare global {
  interface Window {
    YT?: YouTubeNamespace
    onYouTubeIframeAPIReady?: () => void
  }
}

type YouTubePlayer = {
  destroy?: () => void
  mute?: () => void
  playVideo?: () => void
  loadVideoById?: (videoId: string) => void
}

type YouTubePlayerEvent = {
  data?: number
}

type YouTubePlayerOptions = {
  height: string
  width: string
  videoId?: string
  playerVars: Record<string, number>
  events: {
    onReady: () => void
    onStateChange: (event: YouTubePlayerEvent) => void
    onError: () => void
  }
}

type YouTubeNamespace = {
  Player: new (element: HTMLElement, options: YouTubePlayerOptions) => YouTubePlayer
}

type UseYouTubeIFramePlayerArgs = {
  videoId: string | null
  autoplay?: boolean
  onEnded?: () => void
}

type PlayerStatus = 'idle' | 'loading' | 'ready' | 'error'

function loadYouTubeIFrameApi(): Promise<void> {
  if (typeof window === 'undefined') return Promise.reject(new Error('No window'))
  if (window.YT?.Player) return Promise.resolve()

  const existing = document.querySelector<HTMLScriptElement>('script[src="https://www.youtube.com/iframe_api"]')
  if (existing) {
    return new Promise((resolve) => {
      const prev = window.onYouTubeIframeAPIReady
      window.onYouTubeIframeAPIReady = () => {
        prev?.()
        resolve()
      }
    })
  }

  return new Promise((resolve, reject) => {
    window.onYouTubeIframeAPIReady = () => resolve()
    const script = document.createElement('script')
    script.src = 'https://www.youtube.com/iframe_api'
    script.async = true
    script.onerror = () => reject(new Error('Failed to load YouTube IFrame API'))
    document.body.appendChild(script)
  })
}

export function useYouTubeIFramePlayer({ videoId, autoplay = true, onEnded }: UseYouTubeIFramePlayerArgs) {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const playerRef = useRef<YouTubePlayer | null>(null)
  const [status, setStatus] = useState<PlayerStatus>('idle')

  const destroy = useCallback(() => {
    try {
      playerRef.current?.destroy?.()
    } catch {
      // ignore
    } finally {
      playerRef.current = null
    }
  }, [])

  useEffect(() => {
    if (!containerRef.current) return
    setStatus('loading')

    let cancelled = false

    loadYouTubeIFrameApi()
      .then(() => {
        if (cancelled) return
        if (!containerRef.current) return
        if (!window.YT) return

        destroy()

        playerRef.current = new window.YT.Player(containerRef.current, {
          height: '100%',
          width: '100%',
          videoId: videoId ?? undefined,
          playerVars: {
            autoplay: autoplay ? 1 : 0,
            controls: 1,
            rel: 0,
            playsinline: 1,
            modestbranding: 1,
          },
          events: {
            onReady: () => {
              if (cancelled) return
              setStatus('ready')
              if (autoplay && videoId) {
                try {
                  playerRef.current?.mute?.()
                  playerRef.current?.playVideo?.()
                } catch {
                  // ignore
                }
              }
            },
            onStateChange: (event: YouTubePlayerEvent) => {
              if (event?.data === 0) onEnded?.()
            },
            onError: () => {
              if (cancelled) return
              setStatus('error')
            },
          },
        })
      })
      .catch(() => {
        if (cancelled) return
        setStatus('error')
      })

    return () => {
      cancelled = true
      destroy()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [destroy])

  useEffect(() => {
    if (!videoId) return
    if (!playerRef.current) return

    try {
      playerRef.current?.loadVideoById?.(videoId)
      if (autoplay) {
        playerRef.current?.mute?.()
        playerRef.current?.playVideo?.()
      }
    } catch {
      // ignore
    }
  }, [videoId, autoplay])

  return { containerRef, status }
}

