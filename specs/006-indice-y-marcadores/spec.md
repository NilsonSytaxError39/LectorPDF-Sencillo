# Spec 006: Índice y marcadores

Estado: implementada y validada manualmente

## Dependencias

- `specs/001-apertura-y-renderizado/spec.md`
- `specs/002-navegacion-zoom-y-orientacion/spec.md`
- `specs/004-estado-recientes-y-restauracion/spec.md`

## Alcance

Esta especificación define dos funciones independientes:

1. El índice interno del PDF, basado en sus marcadores o destinos existentes.
2. Los marcadores personales guardados localmente por el usuario.

Decisiones aprobadas:

- El índice interno se mostrará como lista jerárquica.
- Al tocar una entrada del índice, el visor saltará a su página destino.
- Si el PDF no tiene marcadores internos, se mostrará `Este PDF no tiene índice`.
- Los marcadores personales serán independientes del índice interno.
- El usuario podrá guardar la página actual con un nombre opcional.

## Requisitos funcionales

### Índice interno del PDF

- **F-601** — EL SISTEMA ofrecerá una acción para consultar el índice interno
  mientras exista un PDF abierto.
- **F-602** — CUANDO el usuario consulte el índice, EL SISTEMA mostrará los
  marcadores internos del PDF en una lista jerárquica.
- **F-603** — EL SISTEMA conservará el nivel jerárquico de cada marcador interno
  mediante sangría o una representación equivalente.
- **F-604** — CUANDO el usuario seleccione un marcador interno válido, EL SISTEMA
  saltará a la página destino sin cerrar el PDF.
- **F-605** — SI un marcador interno no tiene un destino de página resoluble,
  ENTONCES EL SISTEMA lo mostrará como no disponible y no cerrará el visor.
- **F-606** — SI el PDF no contiene marcadores internos, ENTONCES EL SISTEMA
  mostrará `Este PDF no tiene índice` y mantendrá abierto el documento.
- **F-607** — CUANDO el usuario cierre el índice, EL SISTEMA conservará la página
  actual y retirará únicamente la vista del índice.
- **F-608** — EL SISTEMA no generará automáticamente un índice a partir del texto
  del PDF.

### Marcadores personales

- **F-609** — EL SISTEMA ofrecerá una acción para consultar los marcadores
  personales del PDF abierto.
- **F-610** — CUANDO el usuario solicite guardar un marcador personal, EL SISTEMA
  mostrará la página actual y permitirá introducir un nombre opcional.
- **F-611** — CUANDO el usuario confirme un marcador personal, EL SISTEMA lo
  almacenará asociado a la URI completa del PDF y al índice de página.
- **F-612** — SI el nombre del marcador está vacío, ENTONCES EL SISTEMA utilizará
  un nombre predeterminado que incluya el número de página.
- **F-613** — CUANDO el usuario seleccione un marcador personal, EL SISTEMA
  saltará a la página almacenada sin cerrar el PDF.
- **F-614** — CUANDO el usuario elimine un marcador personal, EL SISTEMA
  eliminará únicamente ese registro local.
- **F-615** — CUANDO el usuario guarde un marcador en la misma página y con la
  misma URI, EL SISTEMA actualizará el registro existente sin crear duplicados.
- **F-616** — EL SISTEMA permitirá consultar y eliminar marcadores personales
  desde el visor del PDF.

### Persistencia y privacidad

- **F-617** — EL SISTEMA conservará los marcadores personales después de cerrar
  y volver a abrir la aplicación.
- **F-618** — EL SISTEMA asociará cada marcador personal a la URI completa y no
  únicamente al nombre del archivo.
- **F-619** — EL SISTEMA almacenará localmente solo la URI, página, nombre y fecha
  necesarios para restaurar cada marcador.
- **F-620** — EL SISTEMA no enviará índices, marcadores, consultas ni datos de
  lectura fuera del dispositivo.
- **F-621** — SI un registro de marcador personal está corrupto, ENTONCES EL
  SISTEMA omitirá ese registro y conservará los demás válidos.
- **F-622** — CUANDO una URI asociada a marcadores personales deje de estar
  disponible, EL SISTEMA conservará los registros y mostrará un estado de URI no
  disponible sin borrar el archivo original.

## Requisitos no funcionales medibles

- **NF-601** — EL SISTEMA mostrará un índice de hasta 500 entradas en un máximo
  de 500 ms después de finalizar su lectura local.
- **NF-602** — EL SISTEMA abrirá la vista de marcadores personales en un máximo
  de 100 ms para hasta 100 registros locales.
- **NF-603** — EL SISTEMA saltará a un destino válido en un máximo de 300 ms,
  excluyendo el renderizado de la página.
- **NF-604** — EL SISTEMA no realizará conexiones de red al leer, crear,
  actualizar, consultar o eliminar marcadores.
- **NF-605** — EL SISTEMA no modificará ni creará una copia persistente del PDF.

## Criterios de aceptación

- **AC-601** — Un PDF con marcadores muestra su índice en estructura jerárquica.
- **AC-602** — Seleccionar una entrada del índice lleva a la página correcta.
- **AC-603** — Un PDF sin marcadores muestra `Este PDF no tiene índice`.
- **AC-604** — Un destino interno inválido no cierra ni bloquea el visor.
- **AC-605** — El usuario puede guardar la página actual con nombre o sin nombre.
- **AC-606** — Un marcador personal permite volver a su página después de
  cerrar y reabrir el PDF.
- **AC-607** — Guardar nuevamente la misma página no crea duplicados.
- **AC-608** — Eliminar un marcador personal no elimina el PDF.
- **AC-609** — Índice interno y marcadores personales aparecen separados.
- **AC-610** — Un registro corrupto no impide mostrar los marcadores válidos.
- **AC-611** — Las pruebas no detectan tráfico de red.

## Casos de prueba propuestos

1. Abrir el índice de un PDF con varios niveles.
2. Seleccionar una entrada de primer nivel.
3. Seleccionar una entrada anidada.
4. Abrir un PDF sin marcadores.
5. Probar un destino interno inválido.
6. Crear un marcador personal con nombre.
7. Crear un marcador personal sin nombre.
8. Reabrir el PDF y seleccionar un marcador guardado.
9. Guardar dos veces la misma página.
10. Eliminar un marcador personal.
11. Corromper un registro local y comprobar que los demás aparecen.
12. Revocar el acceso a la URI y verificar que el registro no borra el archivo.
13. Confirmar que índice y marcadores personales están separados.
14. Verificar ausencia de tráfico de red.

## Decisiones de implementación

- El índice se lee localmente con PdfBox-Android mediante
  `PDDocumentOutline` y sus hijos recursivos.
- La jerarquía se representa mediante sangría, limitada visualmente a ocho
  niveles para conservar legibilidad en pantallas pequeñas.
- Los destinos se resuelven a índices de página; una entrada sin destino se
  muestra como no disponible y no cierra el visor.
- La lectura se ejecuta fuera del hilo principal y no modifica ni copia el PDF.
- Los marcadores personales se mantienen separados y persistidos localmente
  por URI completa y página.
