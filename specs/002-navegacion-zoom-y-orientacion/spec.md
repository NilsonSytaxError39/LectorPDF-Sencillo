# Spec 002: Navegación, zoom y orientación

Estado: aprobada para implementación

## Dependencias

- `specs/001-apertura-y-renderizado/spec.md`

## Alcance

Esta especificación define la navegación de un PDF abierto, el indicador de
página, el salto directo a una página, el desplazamiento continuo vertical, el
zoom y la orientación de la actividad.

Esta especificación no define modos claro, oscuro o sepia, inversión de colores,
pantalla encendida, búsqueda, índice, marcadores ni persistencia de la última
página por documento.

Decisiones aprobadas:

- Desplazamiento continuo vertical.
- El indicador representa la página con mayor área visible.
- Zoom máximo de 5x respecto al tamaño base.
- La orientación automática sigue los cambios físicos del dispositivo.
- El doble toque alterna entre 1x y 2x centrado en el punto tocado.
- La preferencia de orientación es global para toda la aplicación.

## Requisitos funcionales

### Navegación continua

- **F-201** — MIENTRAS un PDF esté abierto, EL SISTEMA mostrará sus páginas en
  una secuencia vertical continua.
- **F-202** — CUANDO el usuario desplace la secuencia vertical, EL SISTEMA
  actualizará la página de referencia sin interrumpir el desplazamiento.
- **F-203** — CUANDO varias páginas estén parcialmente visibles, EL SISTEMA
  considerará como página actual la que tenga la mayor área visible.
- **F-204** — CUANDO el usuario alcance el inicio o el final del documento, EL
  SISTEMA impedirá el desplazamiento más allá del contenido disponible.
- **F-205** — MIENTRAS una página se esté renderizando, EL SISTEMA conservará
  disponible la salida del visor y no bloqueará el desplazamiento de las demás
  páginas.

### Indicador de página

- **F-206** — MIENTRAS un PDF esté abierto, EL SISTEMA mostrará de forma
  persistente y no obstructiva la página actual y el total con el formato
  `actual / total`.
- **F-207** — CUANDO cambie la página de referencia, EL SISTEMA actualizará el
  indicador sin requerir una acción adicional del usuario.
- **F-208** — SI el total de páginas no puede determinarse, ENTONCES EL SISTEMA
  mostrará un estado de error identificable y conservará disponible la salida
  del visor.

### Salto directo

- **F-209** — CUANDO el usuario active la acción de salto de página, EL SISTEMA
  mostrará un campo para introducir un número de página.
- **F-210** — CUANDO el usuario introduzca un número entero entre 1 y el total de
  páginas y confirme, EL SISTEMA desplazará la página indicada hasta una
  posición visible y actualizará el indicador.
- **F-211** — SI el usuario introduce un valor vacío, no entero, menor que 1 o
  mayor que el total, ENTONCES EL SISTEMA mostrará un error junto al campo y no
  cambiará la página actual.
- **F-212** — CUANDO el usuario cancele el salto de página, EL SISTEMA cerrará
  el control sin cambiar la posición actual.

### Zoom mediante gesto

- **F-213** — CUANDO el usuario realice un gesto de pellizco sobre una página,
  EL SISTEMA aumentará o reducirá la escala alrededor del centro del gesto.
- **F-214** — CUANDO el usuario realice un doble toque sobre una página en escala
  1x, EL SISTEMA cambiará la escala a 2x y centrará la vista en el punto tocado.
- **F-215** — CUANDO el usuario realice un doble toque sobre una página en escala
  2x, EL SISTEMA volverá la escala a 1x.
- **F-216** — MIENTRAS el zoom sea superior a 1x, EL SISTEMA permitirá desplazar
  el contenido ampliado horizontal y verticalmente.
- **F-217** — SI el usuario intenta superar el zoom máximo de 5x, ENTONCES EL
  SISTEMA mantendrá la escala en 5x.
- **F-218** — SI el usuario intenta reducir la escala por debajo de 1x, ENTONCES
  EL SISTEMA mantendrá la escala en 1x.
- **F-219** — CUANDO cambie la escala, EL SISTEMA conservará el punto focal del
  gesto dentro de los límites visibles siempre que el contenido lo permita.

### Orientación

- **F-220** — EL SISTEMA ofrecerá los modos de orientación automática, vertical
  bloqueada y horizontal bloqueada.
- **F-221** — CUANDO el usuario seleccione orientación vertical bloqueada, EL
  SISTEMA mantendrá la actividad en orientación vertical aunque el dispositivo
  gire.
- **F-222** — CUANDO el usuario seleccione orientación horizontal bloqueada, EL
  SISTEMA mantendrá la actividad en orientación horizontal aunque el dispositivo
  gire.
- **F-223** — MIENTRAS esté seleccionada la orientación automática, EL SISTEMA
  seguirá los cambios de orientación física del dispositivo.
- **F-224** — CUANDO el usuario cambie el modo de orientación, EL SISTEMA
  persistirá la preferencia global y la aplicará en la siguiente apertura.
- **F-225** — CUANDO cambie la configuración de orientación, EL SISTEMA
  conservará el documento abierto y restaurará una posición equivalente sin
  cerrar forzosamente la aplicación.

## Requisitos no funcionales medibles

- **NF-201** — EL SISTEMA actualizará el indicador de página en un máximo de
  150 ms después de estabilizarse la posición visible.
- **NF-202** — CUANDO el usuario confirme un salto válido, EL SISTEMA hará
  visible la página solicitada en un máximo de 500 ms, excluyendo el tiempo de
  renderizado de la página.
- **NF-203** — EL SISTEMA mantendrá la escala dentro del intervalo [1x, 5x].
- **NF-204** — Durante un gesto de zoom, EL SISTEMA no asignará un bitmap con más
  de 6.000.000 píxeles por página.
- **NF-205** — EL SISTEMA no bloqueará el hilo principal durante más de 100 ms
  consecutivos al actualizar navegación, escala u orientación.
- **NF-206** — EL SISTEMA mantendrá operativa la salida del visor durante
  renderizados, desplazamientos, saltos y cambios de orientación.

## Criterios de aceptación

- **AC-201** — Un PDF de al menos 10 páginas puede recorrerse verticalmente sin
  cambiar de pantalla por cada página.
- **AC-202** — El indicador muestra siempre una página válida entre 1 y el total,
  y cambia según la página con mayor área visible.
- **AC-203** — Un salto a la página 5 de un PDF de 10 páginas hace visible la
  página 5 y muestra `5 / 10`.
- **AC-204** — Los valores 0, negativos, decimales, texto y valores superiores al
  total se rechazan sin desplazar el documento.
- **AC-205** — El pellizco permite ampliar y reducir; la escala nunca baja de
  1x ni supera 5x.
- **AC-206** — El doble toque en 1x produce 2x centrado en el punto tocado y el
  doble toque posterior devuelve la escala a 1x.
- **AC-207** — En una escala superior a 1x, el usuario puede alcanzar los bordes
  horizontales y verticales del contenido sin perderlo fuera de la vista.
- **AC-208** — Cada uno de los tres modos de orientación produce el
  comportamiento definido después de girar físicamente el dispositivo.
- **AC-209** — Tras cerrar y volver a abrir la aplicación, se conserva el modo de
  orientación global seleccionado.
- **AC-210** — Durante un cambio de orientación o renderizado, la aplicación no
  se cierra forzosamente y permite salir del visor.

## Casos de prueba propuestos

1. Desplazamiento desde la primera hasta la última página.
2. Indicador con dos páginas parcialmente visibles.
3. Salto a primera, intermedia y última página.
4. Entrada vacía, texto, decimal, cero, negativo y superior al total.
5. Pellizco gradual desde 1x hasta 5x.
6. Intento de superar 5x y de bajar de 1x.
7. Doble toque en el centro y cerca de cada borde.
8. Desplazamiento horizontal y vertical a 2x y 5x.
9. Orientación automática con dos giros del dispositivo.
10. Orientación vertical bloqueada con giro del dispositivo.
11. Orientación horizontal bloqueada con giro del dispositivo.
12. Destrucción y recreación de la actividad durante un cambio de orientación.
13. Documento de 100 páginas durante desplazamiento rápido.

## Decisiones de alcance confirmadas

- La especificación completa queda aprobada para implementación.
- El indicador se mostrará como `actual / total`, sin la palabra “Página”.
- El salto directo aceptará únicamente números enteros.
