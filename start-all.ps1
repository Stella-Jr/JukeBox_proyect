# Script para iniciar todos los servicios de JukeBox

Write-Host "Iniciando servicios de JukeBox..."

# Iniciar Backend (Spring Boot)
if (Test-Path "Backend\mvnw.cmd") {
    Write-Host "Iniciando Backend..."
    Start-Process ".\Backend\mvnw.cmd" "spring-boot:run" -WorkingDirectory ".\Backend" -NoNewWindow
} else {
    Write-Host "mvnw.cmd no encontrado en Backend. Asegúrate de tener Java y Maven."
}

# Iniciar Realtime Service
if (Test-Path "realtime-service\package.json") {
    Write-Host "Iniciando Realtime Service..."
    Start-Process "npm" "start" -WorkingDirectory ".\realtime-service" -NoNewWindow
} else {
    Write-Host "package.json no encontrado en realtime-service."
}

# Iniciar Frontend Client
if (Test-Path "frontend-client\package.json") {
    Write-Host "Iniciando Frontend Client..."
    Start-Process "npm" "run dev" -WorkingDirectory ".\frontend-client" -NoNewWindow
} else {
    Write-Host "package.json no encontrado en frontend-client."
}

# Iniciar Host Player
if (Test-Path "host-player\package.json") {
    Write-Host "Iniciando Host Player..."
    Start-Process "npm" "run dev" -WorkingDirectory ".\host-player" -NoNewWindow
} else {
    Write-Host "package.json no encontrado en host-player."
}

Write-Host "Todos los servicios están iniciándose en segundo plano."
Write-Host "Backend: http://localhost:8080"
Write-Host "Realtime Service: http://localhost:3000"
Write-Host "Frontend Client: http://localhost:5173"
Write-Host "Host Player: http://localhost:5174 (asumiendo puerto por defecto)"