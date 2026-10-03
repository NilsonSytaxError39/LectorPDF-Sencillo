# Spec 005: Búsqueda de texto

Estado: implementada y validada manualmente

## Dependencias

- `specs/001-apertura-y-renderizado/spec.md`
- `specs/002-navegacion-zoom-y-orientacion/spec.md`
- `specs/003-modos-lectura-y-pantalla/spec.md`
- `specs/004-estado-recientes-y-restauracion/spec.md`

## Alcance

Esta especificación define la búsqueda local de texto seleccionable dentro del
PDF abierto. La búsqueda se ejecutará sin internet, sin cuentas, sin
telemetría y sin OCR.

Decisiones aprobadas:

- La búsqueda no distinguirá mayúsculas de minúsculas.
- Los resultados se actualizarán mientras el usuario escribe.
- El visor saltará a la primera coincidencia y resaltará las coincidencias
  visibles.
- Los botones anterior y siguiente navegarán entre coincidencias sin ciclo.
- Los PDFs escaneados sin capa de texto mostrarán un estado explícito de texto
  no encontrado.

Esta especificación no define OCR, traducción, anotaciones permanentes ni
modificación del archivo PDF.

## Requisitos funcionales

### Activación y consulta

- **F-501** — EL SISTEMA ofrecerá una acción de búsqueda cuando exista un PDF
  abierto.
- **F-502** — CUANDO el usuario active la búsqueda, EL SISTEMA mostrará un
  campo de texto, un botón para cerrar y controles anterior/siguiente.
- **F-503** — MIENTRAS el usuario introduzca una consulta no vacía, EL SISTEMA
  buscará coincidencias sin distinguir mayúsculas de minúsculas.
- **F-504** — CUANDO la consulta esté vacía, EL SISTEMA eliminará los resultados,
  el resaltado y el estado de navegación de la búsqueda.
- **F-505** — CUANDO el usuario modifique la consulta, EL SISTEMA actualizará
  los resultados sin cerrar el PDF ni cambiar la URI abierta.
- **F-506** — EL SISTEMA tratará los caracteres introducidos literalmente y no
  interpretará la consulta como una expresión regular.

### Resultados y navegación

- **F-507** — CUANDO existan coincidencias, EL SISTEMA mostrará el número de
  coincidencia activa y el total de coincidencias.
- **F-508** — CUANDO aparezca la primera coincidencia de una consulta, EL
  SISTEMA saltará a la página que la contiene.
- **F-509** — CUANDO el usuario seleccione siguiente, EL SISTEMA saltará a la
  siguiente coincidencia en orden de página y posición dentro de la página.
- **F-510** — CUANDO el usuario seleccione anterior, EL SISTEMA saltará a la
  coincidencia anterior en orden inverso.
- **F-511** — SI el usuario solicita siguiente estando en la última coincidencia,
  ENTONCES EL SISTEMA mantendrá la última coincidencia y mostrará que no hay
  más resultados posteriores.
- **F-512** — SI el usuario solicita anterior estando en la primera coincidencia,
  ENTONCES EL SISTEMA mantendrá la primera coincidencia y mostrará que no hay
  más resultados anteriores.
- **F-513** — CUANDO una coincidencia esté dentro de una página visible, EL
  SISTEMA la resaltará visualmente sin modificar el contenido del PDF.
- **F-514** — CUANDO el usuario cierre la búsqueda, EL SISTEMA retirará el
  resaltado y conservará la página alcanzada por la navegación.

### Sin resultados y PDFs escaneados

- **F-515** — SI no existen coincidencias para una consulta no vacía, ENTONCES
  EL SISTEMA mostrará “No se encontró texto” y no moverá la página actual.
- **F-516** — SI el PDF no contiene una capa de texto consultable, ENTONCES EL
  SISTEMA mostrará “No se encontró texto” y no ejecutará OCR.
- **F-517** — SI una página no puede analizarse, ENTONCES EL SISTEMA omitirá esa
  página, conservará las demás coincidencias válidas y mostrará un error
  explícito si ninguna página puede analizarse.

### Privacidad y límites

- **F-518** — EL SISTEMA realizará la extracción y búsqueda únicamente en el
  dispositivo.
- **F-519** — EL SISTEMA no enviará la consulta, el texto extraído ni el
  contenido del PDF por la red.
- **F-520** — EL SISTEMA no persistirá la consulta ni el texto extraído después
  de cerrar el visor, salvo que una futura especificación lo autorice.
- **F-521** — EL SISTEMA no modificará ni creará una copia persistente del PDF
  para realizar la búsqueda.
- **F-522** — CUANDO el usuario cierre la búsqueda mientras se construye el
  índice, EL SISTEMA cancelará la indexación pendiente y liberará sus recursos
  sin mostrar un resultado de búsqueda cerrado.

## Requisitos no funcionales medibles

- **NF-501** — EL SISTEMA mostrará el estado inicial de la consulta en un
  máximo de 500 ms después de cada cambio de texto para un PDF de hasta 100
  páginas y 20 MiB, excluyendo el tiempo de extracción inicial.
- **NF-502** — EL SISTEMA actualizará la interfaz de resultados sin bloquear la
  interacción durante más de 100 ms consecutivos.
- **NF-503** — EL SISTEMA liberará los resultados y el texto extraído al cerrar
  el visor o la búsqueda.
- **NF-504** — EL SISTEMA no realizará conexiones de red durante la activación,
  extracción, búsqueda, resaltado o navegación.
- **NF-505** — EL SISTEMA mantendrá el consumo adicional de memoria de texto y
  resultados por debajo de 32 MiB para un PDF de hasta 100 páginas.

## Criterios de aceptación

- **AC-501** — El usuario puede abrir la búsqueda mientras lee un PDF.
- **AC-502** — Escribir una consulta actualiza los resultados sin distinguir
  mayúsculas y minúsculas.
- **AC-503** — La primera coincidencia lleva el visor a la página correcta.
- **AC-504** — Las coincidencias visibles se resaltan sin alterar el PDF.
- **AC-505** — Los controles anterior y siguiente recorren todas las
  coincidencias en orden.
- **AC-506** — Al alcanzar el primer o último resultado, la aplicación no hace
  ciclo y muestra el límite correspondiente.
- **AC-507** — Una consulta sin resultados muestra “No se encontró texto”.
- **AC-508** — Un PDF escaneado sin texto no inicia OCR ni produce tráfico de
  red.
- **AC-509** — Cerrar la búsqueda retira el resaltado y mantiene la página
  actual.
- **AC-510** — La consulta y los resultados no reaparecen después de cerrar y
  volver a abrir el visor.
- **AC-511** — Las pruebas no detectan tráfico de red originado por la búsqueda.
- **AC-512** — Cerrar la búsqueda durante la indexación detiene el procesamiento
  pendiente y no muestra resultados después del cierre.

## Casos de prueba propuestos

1. Buscar una palabra con coincidencias en una sola página.
2. Buscar una palabra con coincidencias en varias páginas.
3. Cambiar mayúsculas y minúsculas de la consulta.
4. Modificar la consulta mientras se muestran resultados.
5. Usar siguiente hasta alcanzar la última coincidencia.
6. Usar anterior hasta alcanzar la primera coincidencia.
7. Buscar una cadena inexistente.
8. Buscar en un PDF escaneado sin capa de texto.
9. Cerrar la búsqueda y comprobar que desaparece el resaltado.
10. Cerrar y reabrir el visor y comprobar que la consulta no se conserva.
11. Probar un PDF de hasta 100 páginas y 20 MiB.
12. Verificar ausencia de tráfico de red.

## Decisiones de implementación

- La extracción local usa PdfBox-Android 2.0.27.0 bajo Apache 2.0 y mantiene
  `PdfRenderer` como motor visual.
- El índice de texto se construye una vez por apertura de búsqueda y se
  reutiliza para las consultas posteriores.
- El resaltado usa las posiciones reales de los glifos, `cropBox`, rotación y
  coordenadas normalizadas para respetar los modos de lectura existentes.
- La indexación y la búsqueda se ejecutan fuera del hilo principal.
- La indexación acepta cancelación cooperativa; cerrar el diálogo la activa y
  descarta cualquier resultado pendiente.
