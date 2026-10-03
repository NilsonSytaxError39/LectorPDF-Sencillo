# Spec 001: Apertura y renderizado

Estado: aprobada para implementación

## Alcance

Esta especificación define la apertura y el renderizado inicial de documentos PDF
locales mediante URI. No define navegación entre páginas, zoom, modos de lectura,
orientación, recientes ni persistencia de la última página.

Decisiones aprobadas:

- Nombre de aplicación: `Lector PDF`
- Identificador: `com.example.lectorpdf`
- `minSdk`: 24
- Interfaz: XML/View Binding
- Acceso a documentos: únicamente mediante URI seleccionada por el usuario
- Motor provisional: `android.graphics.pdf.PdfRenderer`

## Requisitos funcionales

### Apertura mediante selector del sistema

- **F-001** — CUANDO el usuario solicite abrir un documento desde la aplicación,
  EL SISTEMA mostrará el selector del sistema restringido a documentos PDF.
- **F-002** — CUANDO el usuario seleccione una URI válida desde el selector del
  sistema, EL SISTEMA conservará autorización de lectura persistente para esa URI
  cuando el proveedor lo permita y abrirá el documento.
- **F-003** — CUANDO el usuario cancele el selector del sistema, EL SISTEMA
  permanecerá en la pantalla actual y no mostrará un error.
- **F-004** — EL SISTEMA no solicitará permisos amplios de almacenamiento para
  abrir una URI seleccionada por el usuario.

### Apertura mediante “Abrir con”

- **F-005** — CUANDO otra aplicación envíe una URI PDF mediante una acción de
  visualización compatible, EL SISTEMA abrirá esa URI en el visor.
- **F-006** — CUANDO otra aplicación envíe una URI sin permiso de lectura válido,
  EL SISTEMA solicitará únicamente el acceso necesario si el sistema operativo lo
  permite; si no, informará que el documento no puede abrirse.
- **F-007** — CUANDO la URI recibida no corresponda a un PDF legible, EL SISTEMA
  rechazará la apertura y mostrará un mensaje comprensible para el usuario.
- **F-007a** — CUANDO una URI tenga un MIME type ausente o distinto de
  `application/pdf`, EL SISTEMA comprobará el contenido antes de rechazarla.

### Apertura y ciclo de vida del documento

- **F-008** — CUANDO el sistema entregue una URI válida, EL SISTEMA abrirá el
  descriptor mediante `ContentResolver` y creará un renderizador asociado al
  documento.
- **F-009** — MIENTRAS un documento esté abierto, EL SISTEMA mantendrá abiertos
  únicamente los recursos necesarios para la operación actual.
- **F-010** — CUANDO el usuario abandone el visor o la actividad sea destruida,
  EL SISTEMA cerrará el renderizador, las páginas abiertas y los descriptores
  asociados.
- **F-011** — SI la URI deja de estar disponible durante la apertura o el uso,
  ENTONCES EL SISTEMA cerrará los recursos parciales, informará del problema y
  permitirá volver a la pantalla anterior sin cerrar la aplicación.

### Renderizado

- **F-012** — CUANDO el documento se abra correctamente, EL SISTEMA mostrará la
  primera página válida sin cargar todas las páginas simultáneamente.
- **F-013** — CUANDO una página deba representarse, EL SISTEMA renderizará esa
  página bajo demanda en un bitmap limitado al tamaño de visualización.
- **F-014** — MIENTRAS el renderizado de una página esté en curso, EL SISTEMA
  mostrará un estado de carga y mantendrá disponible la interacción de salida.
- **F-015** — SI una página no puede renderizarse, ENTONCES EL SISTEMA mostrará
  un estado de error para esa página y conservará disponible la navegación fuera
  del documento.
- **F-016** — EL SISTEMA no modificará permanentemente el contenido del PDF
  durante el renderizado.

## Requisitos no funcionales medibles

- **NF-001** — Para un PDF local de hasta 20 MiB y 100 páginas, EL SISTEMA
  mostrará la primera página en un máximo de 2 segundos en un dispositivo de
  referencia con Android 10 o superior, excluyendo el tiempo de selección de la
  URI.
- **NF-002** — EL SISTEMA no mantendrá en memoria más de tres bitmaps de páginas
  completas simultáneamente durante la apertura inicial.
- **NF-003** — Para una página cuyo bitmap calculado supere 6.000.000 píxeles,
  EL SISTEMA reducirá la resolución de renderizado antes de asignar el bitmap.
- **NF-004** — Durante la apertura y el renderizado, EL SISTEMA no realizará
  conexiones de red.
- **NF-005** — EL SISTEMA funcionará con `minSdk 24` y no dependerá de permisos
  amplios de almacenamiento para abrir una URI seleccionada por el usuario.
- **NF-006** — Las operaciones de apertura y renderizado no bloquearán el hilo
  principal durante más de 100 ms consecutivos.

## Errores mínimos

El sistema debe diferenciar, al menos, estos casos:

1. URI cancelada por el usuario.
2. URI sin permiso de lectura.
3. URI inexistente o proveedor no disponible.
4. Documento que no es PDF.
5. PDF vacío o ilegible.
6. Error al abrir el descriptor.
7. Error al abrir el documento con `PdfRenderer`.
8. Error al renderizar una página.
9. Memoria insuficiente durante el renderizado.

Para los casos 2 a 9, el mensaje visible debe indicar que la apertura no fue
posible y ofrecer una acción de retorno. No se deben ocultar silenciosamente
errores de apertura o renderizado.

## Criterios de aceptación

- **AC-001** — Desde una instalación limpia, el usuario puede seleccionar un PDF
  mediante el selector del sistema y ver su primera página sin conceder acceso
  global al almacenamiento.
- **AC-002** — Un PDF enviado desde “Abrir con” se abre en la misma actividad de
  visor o en una actividad equivalente, sin duplicar una sesión de documento
  innecesariamente.
- **AC-003** — Cancelar el selector no cambia la pantalla ni muestra un error.
- **AC-004** — Una URI inválida, no autorizada o no legible muestra un error
  explícito y no provoca cierre forzoso.
- **AC-005** — Al salir del visor, el documento y sus páginas dejan de retener
  recursos abiertos.
- **AC-006** — Un documento de prueba de 100 páginas no provoca la carga
  simultánea de sus 100 páginas.
- **AC-007** — Un PDF con una página de resolución extrema se renderiza con el
  límite de píxeles definido o muestra un error controlado.
- **AC-008** — Un análisis de tráfico durante la prueba no detecta conexiones de
  red originadas por la aplicación.

## Casos de prueba propuestos

1. PDF local pequeño seleccionado desde el selector.
2. PDF recibido mediante “Abrir con”.
3. Cancelación del selector.
4. URI revocada antes de abrir.
5. URI eliminada o proveedor desconectado.
6. Archivo con extensión `.pdf` pero contenido no PDF.
7. PDF de 100 páginas.
8. PDF con una página de gran resolución.
9. PDF con imágenes escaneadas.
10. Rotación o destrucción de la actividad durante la apertura.
11. Dispositivo con poca memoria disponible.
12. Verificación de ausencia de tráfico de red.

## Decisiones de alcance confirmadas

- Los umbrales de rendimiento y memoria quedan aprobados.
- El nombre del documento se reserva para la spec de navegación.
- Una URI con MIME type ausente o incorrecto se acepta si la validación del
  contenido confirma que es un PDF legible.
