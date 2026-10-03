# Guía de desarrollo con IA para Lector PDF

## Propósito

Esta guía adapta las prácticas de los apuntes del curso de desarrollo con IA al
lector PDF Android. La IA puede ejecutar trabajo repetitivo, pero la
arquitectura, la veracidad de los resultados, la privacidad y la aprobación de
los cambios siguen bajo control humano.

## Principios aplicables

1. **Ingeniería, no aceptación ciega:** cada cambio debe tener una razón,
   límites, pruebas y revisión; no se acepta código solo porque compile.
2. **La spec es el contrato vivo:** una funcionalidad se define primero en
   `specs/NNN-nombre/spec.md`, se aclara, se implementa y se valida contra sus
   requisitos.
3. **Requisitos verificables:** usar EARS en español:
   `CUANDO`, `SI... ENTONCES`, `MIENTRAS` o comportamiento permanente.
   Evitar términos subjetivos como “rápido” sin umbral medible.
4. **Cambios controlados:** ante una nueva decisión, actualizar primero la
   spec; después el plan y las tareas; por último el código.
5. **Privacidad por diseño:** el lector permanece local, sin cuentas, anuncios,
   telemetría ni tráfico de red para abrir, leer, buscar o guardar estado.
6. **Simplicidad del stack:** conservar XML/View Binding,
   `android.graphics.pdf.PdfRenderer` para renderizado y añadir dependencias
   solo cuando exista una necesidad documentada y su licencia sea aceptable.

## Flujo SDD adaptado

1. Leer `MEMORY.md`, la spec activa, el código afectado y las instrucciones del
   proyecto.
2. Identificar ambigüedades y preguntar una decisión cada vez; no asumir
   comportamiento de UX, límites o errores.
3. Redactar o actualizar la spec antes de tocar código.
4. Preparar plan y tareas pequeñas, ordenadas por dependencia.
5. Implementar una tarea coherente, con cambios quirúrgicos.
6. Ejecutar diagnósticos y la compilación offline:
   `.\gradlew.bat assembleDebug --offline`.
7. Validar cada requisito: pruebas automatizadas cuando existan y pruebas
   manuales en el dispositivo para interfaz, gestos, insets, rotación y PDFs
   grandes.
8. Registrar en `MEMORY.md` el estado, decisiones, errores evitados y siguiente
   paso; mantenerlo breve y sin datos sensibles.

## Guardarraíles para este proyecto

- No cambiar el motor de renderizado por una dependencia nueva sin comparar
  licencia, tamaño del APK, `minSdk`, memoria y compatibilidad offline.
- No añadir permisos amplios de almacenamiento; trabajar con URIs elegidas por
  el usuario y permisos concedidos por el sistema.
- No cargar un PDF completo en memoria si el flujo puede procesarlo por páginas
  o bajo demanda.
- No ejecutar búsqueda, extracción o validación en el hilo principal.
- No persistir texto extraído, consultas o contenido del PDF salvo que una spec
  aprobada lo exija.
- No ocultar errores con valores por defecto que parezcan éxito; mostrar un
  mensaje comprensible y conservar el estado válido.
- No considerar una compilación verde como prueba suficiente de UX: probar
  también Redmi, orientación horizontal, barras del sistema, PDFs escaneados y
  documentos grandes.

## Gestión del contexto

- `MEMORY.md` contiene solo el estado actual y las decisiones que siguen
  vigentes.
- Cada spec debe mantener alcance, dependencias, casos límite y criterios de
  aceptación.
- Las instrucciones específicas del proyecto deben mantenerse cortas y
  separadas de la memoria histórica.
- Al resumir una sesión, conservar siempre: spec activa, archivos tocados,
  comandos de validación, fallos conocidos y siguiente acción.

## Estrategia de revisión

Antes de cerrar una funcionalidad, revisar:

- ¿Cada requisito EARS tiene una comprobación?
- ¿Se probaron entradas inválidas, URIs revocadas y registros corruptos?
- ¿Se preserva la última página y el estado válido?
- ¿La operación permanece local y sin red?
- ¿El cambio respeta accesibilidad, una mano, insets y orientación?
- ¿La dependencia y su licencia están documentadas?
- ¿La documentación y `MEMORY.md` reflejan el estado real?

## Aplicación a las specs actuales

- Specs 001–004 establecen apertura, renderizado, navegación, lectura,
  orientación, recientes y restauración.
- Spec 005 añade extracción y búsqueda local mediante PdfBox-Android, sin
  reemplazar `PdfRenderer`; el índice debe tratarse como un recurso temporal y
  medirse en PDFs de más de 1000 páginas.
- Spec 006 separa índice interno del PDF y marcadores personales. Los
  marcadores personales ya se almacenan por URI y página; el índice interno
  debe implementarse solo después de verificar la API de destinos de PdfBox.

## Fuente y alcance de esta guía

La guía resume y adapta:

- `Apuntes-Curso-Desarrollo-IA-Dia-1.pdf`
- `Apuntes-Curso-Desarrollo-IA-Dia-2.pdf`
- `Apuntes-Curso-Desarrollo-IA-Dia-3.pdf`

Los apuntes incluyen ejemplos de una aplicación web y herramientas concretas
de otros entornos. Esos ejemplos no se trasladan literalmente: aquí prevalecen
Android, Kotlin, las specs aprobadas, la privacidad offline y las pruebas
reales del lector PDF.
