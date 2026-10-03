---
name: pdf-android-reader
description: Reglas para modificar, depurar o validar el lector PDF Android.
---

# Skill: lector PDF Android

Usa estas reglas cuando trabajes en apertura, renderizado, navegación, búsqueda,
resaltado, orientación, temas, recientes o marcadores.

## Arquitectura que se debe conservar

- Abrir documentos mediante URI concedida por el selector del sistema.
- Usar `PdfRenderer` para pintar páginas bajo demanda.
- Usar PdfBox-Android solo para extracción local, búsqueda o índice aprobado.
- Mantener el estado por URI completa y no modificar el archivo original.

## Rendimiento y memoria

- No cargar todas las páginas como bitmaps permanentes.
- Limitar el tamaño de cada bitmap y liberar recursos en ciclo de vida.
- Ejecutar extracción e indexación fuera del hilo principal.
- Informar progreso en documentos grandes y comprobar cancelación antes de
  añadirla como comportamiento.
- Probar al menos un PDF de más de 1000 páginas.

## Privacidad

- No realizar conexiones de red para abrir, leer, buscar o guardar estado.
- No añadir permisos amplios de almacenamiento.
- No guardar texto extraído, consultas ni datos del PDF sin una spec aprobada.
- No incluir credenciales, telemetría o identificadores en logs.

## Resaltado y coordenadas

- Asociar cada carácter buscado con su `TextPosition`.
- Considerar `cropBox`, rotación y dimensiones reales de la página.
- Usar la línea base y la altura del glifo para calcular el rectángulo.
- Validar el resaltado visualmente sobre palabras de distintas longitudes.

## Depuración

1. Reproducir el caso con el mismo PDF y dispositivo.
2. Registrar página, URI, estado de ciclo de vida y acción del usuario.
3. Aislar callbacks tardíos, rotación, pausa, scroll y tareas asíncronas.
4. Formular una hipótesis verificable y cambiar lo mínimo necesario.
5. Compilar offline y repetir el caso original, además de un caso límite.

## Validación mínima

- URI válida, URI revocada y archivo no compatible.
- PDF con texto, PDF escaneado y consulta sin resultados.
- Documento grande, rotación y recreación de actividad.
- Restauración de página, zoom, temas, inversión y pantalla encendida.
- Marcadores personales: guardar, actualizar, saltar, eliminar y persistir.
