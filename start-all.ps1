# Script para iniciar todos los servicios de JukeBox

Write-Host "Iniciando servicios de JukeBox..."

function Test-PortInUse {
    param([int]$Port)
    try {
        return (Test-NetConnection -ComputerName "localhost" -Port $Port -InformationLevel Quiet)
    } catch {
        return $false
    }
}

function Pick-FreePort {
    param([int[]]$Candidates)
    foreach ($p in $Candidates) {
        if (-not (Test-PortInUse -Port $p)) { return $p }
    }
    return $null
}

# Iniciar Backend (Spring Boot)
if (Test-Path "Backend\mvnw.cmd") {
    # Backend requiere datasource configurado (no hay application.properties en el repo)
    if (-not $env:SPRING_DATASOURCE_URL) {
        Write-Host "Backend NO iniciado: falta configurar DB (SPRING_DATASOURCE_URL)."
    } else {
        Write-Host "Iniciando Backend..."
        Start-Process ".\Backend\mvnw.cmd" "spring-boot:run" -WorkingDirectory ".\Backend" -NoNewWindow
    }
} else {
    Write-Host "mvnw.cmd no encontrado en Backend. Asegúrate de tener Java y Maven."
}

# Iniciar Realtime Service
if (Test-Path "realtime-service\package.json") {
    if (Test-PortInUse -Port 3000) {
        Write-Host "Realtime Service ya está corriendo en http://localhost:3000 (salteando)."
    } else {
        Write-Host "Iniciando Realtime Service..."
        Start-Process "cmd.exe" "/c npm start" -WorkingDirectory ".\realtime-service" -NoNewWindow
    }
} else {
    Write-Host "package.json no encontrado en realtime-service."
}

# Iniciar Frontend Client
if (Test-Path "frontend-client\package.json") {
    $frontendPort = Pick-FreePort -Candidates @(5173, 5174, 5175)
    if (-not $frontendPort) {
        Write-Host "Frontend Client NO iniciado: puertos 5173-5175 ocupados."
    } else {
        Write-Host "Iniciando Frontend Client (port $frontendPort)..."
        Start-Process "cmd.exe" "/c npm run dev -- --port $frontendPort" -WorkingDirectory ".\frontend-client" -NoNewWindow
    }
} else {
    Write-Host "package.json no encontrado en frontend-client."
}

Write-Host "Todos los servicios están iniciándose en segundo plano."
Write-Host "Backend: http://localhost:8080"
Write-Host "Realtime Service: http://localhost:3000"
Write-Host "Frontend Client: http://localhost:5173 (o siguiente puerto libre)"
Write-Host "Host (Reproductor): http://localhost:<puerto-frontend>/host/<roomId>"