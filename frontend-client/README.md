# Frontend Client - Jukebox Project

Esta es la aplicación web para usuarios del proyecto Jukebox, construida con React, TypeScript y Vite. Permite buscar canciones, agregarlas a la cola y votar.

## Instalación

1. Asegúrate de tener Node.js instalado.
2. Instala las dependencias:

```bash
npm install
```

## Uso en desarrollo

Para ejecutar la aplicación en modo desarrollo (con hot reload):

```bash
npm run dev
```

Esto iniciará el servidor de desarrollo. Por defecto, estará disponible en http://localhost:5173/

## Build para producción

Para buildear la aplicación para producción:

```bash
npm run build
```

Los archivos optimizados se generarán en la carpeta `dist/`.

## Ver la web

Después de buildear, para ver la versión de producción localmente:

```bash
npm run preview
```

Esto iniciará un servidor local que servirá los archivos de `dist/`. Por defecto, estará en http://localhost:4173/

## Linting

Para ejecutar el linter:

```bash
npm run lint
```
