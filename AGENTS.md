# AGENTS.md — Lector PDF

Lector PDF Android privado para lectura prolongada y uso con una mano.

## Stack y estructura

- Kotlin, Android, XML/View Binding; `minSdk 24`.
- `PdfRenderer` renderiza; PdfBox-Android extrae texto localmente.
- `specs/NNN-nombre/` contiene contratos de funcionalidades.
- `docs/` contiene guías; `MEMORY.md` resume el estado entre sesiones.

## Comandos y flujo

- Compilar offline: `.\gradlew.bat assembleDebug --offline`.
- Leer `MEMORY.md`, la spec activa y el código afectado antes de editar.
- Leer también `docs/constitution.md` cuando el cambio afecte arquitectura,
  privacidad, dependencias o criterios de validación.
- Actualizar primero la spec, plan y tareas cuando cambie el comportamiento.
- Usar requisitos EARS verificables; preguntar antes de asumir UX.
- Mantener cambios pequeños, claros y compatibles.
- Compilar al terminar y probar manualmente UI, gestos, orientación, insets y
  PDFs grandes en emulador o dispositivo.

## Privacidad y PDF

- Usar solo URIs elegidas por el usuario; no añadir almacenamiento amplio.
- No usar cuentas, anuncios, telemetría, servicios remotos ni red.
- No persistir contenido, consultas o texto extraído sin spec aprobada.
- Ejecutar extracción, indexación y renderizado fuera del hilo principal.
- Limitar bitmaps y memoria; probar documentos de más de 1000 páginas.
- Mostrar errores explícitos; no ocultar fallos con estados de éxito.

## Dependencias y memoria

- Preguntar antes de añadir dependencias, permisos o cambiar formatos.
- Revisar licencia, tamaño, `minSdk`, memoria y modo offline de cada dependencia.
- No reemplazar `PdfRenderer` sin comparación técnica documentada.
- Mantener `MEMORY.md` breve, sin secretos ni datos personales.
- Actualizar `MEMORY.md` con estado, decisiones, errores evitados y siguiente paso.
