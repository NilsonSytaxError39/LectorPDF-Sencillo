# Constitución — Lector PDF

Principios innegociables para specs, planes, código y validaciones.

1. **Privacidad local:** el sistema trabaja con URIs elegidas por el usuario y
   no usa cuentas, anuncios, telemetría, red ni almacenamiento amplio.
2. **La spec manda:** todo cambio de comportamiento se define primero en la
   spec activa y se valida contra sus requisitos EARS.
3. **Simplicidad y compatibilidad:** se conserva Kotlin, XML/View Binding,
   `PdfRenderer`, `minSdk 24` y el funcionamiento offline.
4. **Recursos controlados:** extracción, indexación y renderizado no bloquean
   el hilo principal; bitmaps, documentos y tareas se liberan explícitamente.
5. **Datos mínimos:** solo se persisten los datos locales necesarios para
   restauración, recientes y marcadores; nunca el contenido del PDF sin spec.
6. **Errores visibles:** los fallos se informan claramente y nunca se presentan
   como estados de éxito ni se resuelven ocultando datos.
7. **Validación real:** compilar no basta; se prueban UI, gestos, orientación,
   insets, privacidad, PDFs escaneados y documentos grandes.
8. **Cambios reversibles:** no se cambian dependencias, permisos, formatos
   persistidos ni el motor PDF sin justificar licencia, memoria y compatibilidad.
