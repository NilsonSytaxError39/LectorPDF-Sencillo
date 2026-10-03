# Spec 004: Estado, recientes y restauración

Estado: aprobada para implementación

## Dependencias

- `specs/001-apertura-y-renderizado/spec.md`
- `specs/002-navegacion-zoom-y-orientacion/spec.md`

## Alcance

Esta especificación define la persistencia de la última página por PDF, la lista
de documentos recientes y el tratamiento de URIs que ya no sean accesibles.

Decisiones aprobadas:

- Máximo de 20 documentos recientes.
- Orden descendente por última apertura.
- La última página se guarda al salir y al pausar la aplicación.
- Las URIs no accesibles permanecen visibles con estado identificable.
- El usuario puede eliminar una entrada de recientes sin borrar el archivo.

## Requisitos funcionales

### Identidad y última página

- **F-401** — EL SISTEMA identificará un PDF mediante su URI completa y estable,
  sin usar únicamente el nombre visible del archivo.
- **F-402** — CUANDO el usuario abra un PDF, EL SISTEMA recuperará la última
  página válida guardada para esa URI, o utilizará la primera página si no
  existe un estado previo.
- **F-403** — CUANDO el usuario abandone el visor, EL SISTEMA guardará la página
  actualmente visible asociada a la URI del PDF.
- **F-404** — CUANDO la aplicación pase a estado pausado, EL SISTEMA guardará la
  página actualmente visible y las preferencias de lectura necesarias para
  restaurar la sesión.
- **F-405** — SI la página guardada es menor que 1 o mayor que el total actual,
  ENTONCES EL SISTEMA la ajustará al rango válido antes de mostrar el documento.
- **F-406** — SI el PDF no puede abrirse, ENTONCES EL SISTEMA no sobrescribirá
  una última página válida con un estado inválido.

### Registro de recientes

- **F-407** — CUANDO el usuario abra correctamente un PDF, EL SISTEMA lo añadirá
  o actualizará en la lista de recientes.
- **F-408** — CUANDO un PDF ya existente en recientes vuelva a abrirse, EL SISTEMA
  actualizará su fecha de última apertura sin duplicar la entrada.
- **F-409** — MIENTRAS existan documentos recientes, EL SISTEMA los mostrará en
  orden descendente por fecha de última apertura.
- **F-410** — EL SISTEMA conservará como máximo 20 documentos recientes.
- **F-411** — CUANDO la lista supere 20 entradas, EL SISTEMA eliminará las
  entradas más antiguas de la lista, sin borrar los archivos originales.
- **F-412** — CUANDO el usuario seleccione una entrada reciente, EL SISTEMA
  intentará abrir la URI almacenada mediante el mismo flujo de apertura por URI.
- **F-413** — CUANDO el usuario elimine una entrada reciente, EL SISTEMA
  eliminará únicamente el registro local y conservará el archivo original.

### URIs no accesibles

- **F-414** — SI una URI reciente no puede consultarse, ENTONCES EL SISTEMA
  conservará la entrada y la mostrará como no disponible.
- **F-415** — CUANDO el usuario seleccione una entrada no disponible, EL SISTEMA
  mostrará un error explícito y ofrecerá eliminar la entrada de recientes.
- **F-416** — CUANDO el usuario elimine una entrada no disponible, EL SISTEMA
  eliminará también su estado de última página asociado.
- **F-417** — EL SISTEMA no buscará automáticamente el mismo archivo mediante
  escaneo global del almacenamiento para reparar una URI no accesible.
- **F-418** — SI el permiso persistente de lectura de una URI es revocado,
  ENTONCES EL SISTEMA la tratará como no disponible y no solicitará permisos
  amplios de almacenamiento.

### Privacidad y consistencia

- **F-419** — EL SISTEMA almacenará localmente solo la URI, el nombre mostrado,
  la fecha de apertura, el estado de disponibilidad y la última página.
- **F-420** — EL SISTEMA no enviará la lista de recientes ni el estado de lectura
  fuera del dispositivo.
- **F-421** — CUANDO la aplicación se cierre y vuelva a abrirse, EL SISTEMA
  conservará la lista y los estados almacenados correctamente.
- **F-422** — SI un registro local está corrupto, ENTONCES EL SISTEMA omitirá ese
  registro, conservará los demás registros válidos y no cerrará forzosamente la
  aplicación.

## Requisitos no funcionales medibles

- **NF-401** — EL SISTEMA cargará una lista de hasta 20 recientes en un máximo de
  100 ms desde almacenamiento local.
- **NF-402** — EL SISTEMA no realizará consultas de red para cargar, ordenar,
  validar o eliminar recientes.
- **NF-403** — EL SISTEMA no almacenará más de 20 entradas válidas después de
  completar cualquier operación de registro.
- **NF-404** — EL SISTEMA conservará la última página con un error máximo de cero
  páginas cuando el estado se guarde después de una pausa o salida normal.
- **NF-405** — EL SISTEMA mantendrá una sola entrada por URI normalizada.

## Criterios de aceptación

- **AC-401** — Abrir un PDF y salir permite reabrirlo desde la misma página.
- **AC-402** — Pausar y recrear la actividad conserva la página visible dentro
  del rango válido.
- **AC-403** — Abrir dos veces el mismo PDF produce una sola entrada reciente.
- **AC-404** — La lista muestra primero el PDF abierto más recientemente.
- **AC-405** — Después de abrir 21 PDFs distintos, la lista contiene exactamente
  los 20 más recientes.
- **AC-406** — Eliminar una entrada reciente no elimina ni modifica el archivo.
- **AC-407** — Una URI revocada aparece como no disponible y no desaparece sin
  una acción del usuario.
- **AC-408** — Eliminar una URI no disponible elimina también su estado de página.
- **AC-409** — Una entrada corrupta no impide mostrar las demás entradas válidas.
- **AC-410** — Las pruebas de recientes no producen tráfico de red.

## Casos de prueba propuestos

1. Abrir un PDF en la página 5 y salir normalmente.
2. Reabrirlo y comprobar restauración en la página 5.
3. Pausar la aplicación durante una página intermedia.
4. Reabrir el mismo PDF varias veces.
5. Abrir 21 PDFs distintos.
6. Eliminar una entrada disponible.
7. Revocar el permiso de una URI reciente.
8. Intentar abrir una URI no disponible.
9. Eliminar una entrada no disponible.
10. Introducir un registro local corrupto.
11. Confirmar que no se borra ningún archivo original.
12. Confirmar ausencia de tráfico de red.

## Decisiones de alcance confirmadas

- La especificación completa queda aprobada para implementación.
- La eliminación de una entrada reciente se realizará mediante deslizamiento
  lateral.
