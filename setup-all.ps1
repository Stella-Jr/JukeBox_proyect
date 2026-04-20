# Script para instalar dependencias de todo el proyecto Jukebox

Write-Host "Instalando dependencias del Backend (Spring Boot, Maven)..."
Write-Host "(Asegúrate de tener Java y Maven instalados)"

if (Test-Path "Backend\pom.xml") {
    Push-Location Backend
    mvnw.cmd clean install
    Pop-Location
} else {
    Write-Host "No se encontró Backend/pom.xml. Salta Backend."
}

Write-Host "\nInstalando dependencias del realtime-service (Node.js)..."
if (Test-Path "realtime-service\package.json") {
    Push-Location realtime-service
    npm install
    Pop-Location
} else {
    Write-Host "No se encontró realtime-service/package.json. Salta realtime-service."
}

Write-Host "\nInstalando dependencias del frontend-client (Node.js + React)..."
if (Test-Path "frontend-client\package.json") {
    Push-Location frontend-client
    npm install
    Pop-Location
} else {
    Write-Host "No se encontró frontend-client/package.json. Salta frontend-client."
}

Write-Host "\nInstalando dependencias del host-player (Node.js + React)..."
if (Test-Path "host-player\package.json") {
    Push-Location host-player
    npm install
    Pop-Location
} else {
    Write-Host "No se encontró host-player/package.json. Salta host-player."
}

Write-Host "\nInstalando dependencias del realtime-service (por si hay cambios nuevos)..."
if (Test-Path "realtime-service\package.json") {
    Push-Location realtime-service
    npm install
    Pop-Location
}

Write-Host "\n¡Listo! Todas las dependencias fueron instaladas."
