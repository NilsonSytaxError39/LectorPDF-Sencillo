# Spec 003: Modos de lectura y pantalla

Estado: aprobada para implementación

## Dependencias

- `specs/001-apertura-y-renderizado/spec.md`
- `specs/002-navegacion-zoom-y-orientacion/spec.md`

## Alcance

Esta especificación define los modos claro, oscuro y sepia de la interfaz, la
inversión visual del contenido PDF y la opción global para mantener la pantalla
encendida.

El tema de la interfaz y la inversión del PDF son opciones independientes.
El modo sepia afecta únicamente al fondo y controles de la interfaz. La
inversión afecta todo el contenido visual renderizado del PDF, incluidas
imágenes y gráficos.

Esta especificación no define búsqueda, índice, marcadores, compartir, control
manual de brillo ni preferencias independientes por documento.

## Requisitos funcionales

### Modos de interfaz

- **F-301** — EL SISTEMA ofrecerá los modos de interfaz claro, oscuro y sepia.
- **F-302** — CUANDO el usuario seleccione el modo claro, EL SISTEMA mostrará
  fondos, textos, controles y barras del visor con la paleta clara definida.
- **F-303** — CUANDO el usuario seleccione el modo oscuro, EL SISTEMA mostrará
  fondos, textos, controles y barras del visor con la paleta oscura definida.
- **F-304** — CUANDO el usuario seleccione el modo sepia, EL SISTEMA mostrará
  fondos, textos, controles y barras del visor con la paleta sepia definida.
- **F-305** — CUANDO el usuario cambie el modo de interfaz con un PDF abierto,
  EL SISTEMA aplicará la nueva paleta sin cerrar el documento ni cambiar la
  página actual.
- **F-306** — EL SISTEMA no aplicará automáticamente inversión de colores al PDF
  como consecuencia de cambiar el modo de interfaz.

### Inversión del PDF

- **F-307** — EL SISTEMA ofrecerá una opción independiente para invertir los
  colores del contenido PDF.
- **F-308** — CUANDO el usuario active la inversión del PDF, EL SISTEMA mostrará
  invertidos todos los colores del contenido renderizado, incluidas imágenes,
  gráficos y texto.
- **F-309** — CUANDO el usuario desactive la inversión del PDF, EL SISTEMA
  mostrará nuevamente los colores originales del contenido renderizado.
- **F-310** — CUANDO el usuario cambie la inversión con un PDF abierto, EL SISTEMA
  actualizará las páginas visibles sin cerrar el documento ni cambiar la
  posición de navegación.
- **F-311** — EL SISTEMA aplicará la inversión únicamente durante la
  representación visual y no modificará permanentemente el archivo PDF.
- **F-312** — SI una página no puede actualizarse después de cambiar la
  inversión, ENTONCES EL SISTEMA conservará la página original o mostrará un
  error explícito sin cerrar forzosamente la aplicación.

### Pantalla encendida

- **F-313** — EL SISTEMA ofrecerá una opción global para mantener la pantalla
  encendida durante el uso de la aplicación.
- **F-314** — CUANDO el usuario active mantener la pantalla encendida, EL SISTEMA
  impedirá que la pantalla se apague por inactividad mientras la aplicación esté
  visible.
- **F-315** — CUANDO el usuario desactive mantener la pantalla encendida, EL
  SISTEMA permitirá que el sistema operativo gestione el apagado por inactividad.
- **F-316** — CUANDO la aplicación deje de estar visible, EL SISTEMA no mantendrá
  innecesariamente activa la pantalla.
- **F-317** — CUANDO el usuario cambie la opción de pantalla encendida, EL SISTEMA
  persistirá la preferencia global para futuras aperturas.

### Persistencia

- **F-318** — CUANDO el usuario cambie el modo de interfaz, EL SISTEMA persistirá
  la selección global.
- **F-319** — CUANDO el usuario cambie la inversión del PDF, EL SISTEMA
  persistirá la selección global.
- **F-320** — CUANDO la aplicación se abra nuevamente, EL SISTEMA aplicará el
  modo de interfaz, la inversión y la opción de pantalla encendida persistidos.
- **F-321** — SI una preferencia persistida no es válida, ENTONCES EL SISTEMA
  utilizará el modo claro, la inversión desactivada y la pantalla encendida
  desactivada.

## Requisitos no funcionales medibles

- **NF-301** — EL SISTEMA aplicará un cambio de modo de interfaz en un máximo de
  300 ms después de la selección del usuario, excluyendo el tiempo de
  renderizado de páginas.
- **NF-302** — EL SISTEMA aplicará o retirará la inversión visual en un máximo de
  500 ms para las páginas actualmente visibles, excluyendo renderizados que
  deban repetirse por completo.
- **NF-303** — EL SISTEMA mantendrá la posición de navegación con una diferencia
  máxima de una página después de cambiar tema o inversión.
- **NF-304** — EL SISTEMA no creará una segunda copia persistente del PDF para
  aplicar la inversión.
- **NF-305** — EL SISTEMA no realizará conexiones de red al cambiar modos,
  invertir colores o cambiar la opción de pantalla encendida.
- **NF-306** — EL SISTEMA restaurará el estado de pantalla encendida al entrar o
  salir del visor sin mantener el indicador de pantalla encendida después de que
  la aplicación deje de estar visible.

## Criterios de aceptación

- **AC-301** — El usuario puede seleccionar claro, oscuro y sepia y observa una
  diferencia visible en la interfaz.
- **AC-302** — Cambiar entre claro, oscuro y sepia no invierte automáticamente el
  contenido del PDF.
- **AC-303** — Activar inversión cambia el contenido visual del PDF, incluidas
  imágenes, y desactivarla restaura la representación original.
- **AC-304** — Cambiar el tema o la inversión no cierra el PDF ni reinicia la
  posición de navegación.
- **AC-305** — La preferencia de tema e inversión se conserva al cerrar y volver
  a abrir la aplicación.
- **AC-306** — Al activar pantalla encendida, la pantalla permanece activa
  mientras la aplicación está visible.
- **AC-307** — Al salir de la aplicación o bloquear el dispositivo, no se
  conserva de forma indebida una bandera temporal de pantalla encendida.
- **AC-308** — Una preferencia corrupta o desconocida vuelve a los valores
  seguros definidos.
- **AC-309** — Durante todas las pruebas, no se detecta tráfico de red originado
  por la aplicación.

## Casos de prueba propuestos

1. Cambiar de claro a oscuro con un PDF abierto.
2. Cambiar de oscuro a sepia con un PDF abierto.
3. Cambiar de sepia a claro con navegación avanzada.
4. Activar y desactivar inversión con texto, fotografías y gráficos.
5. Cambiar inversión mientras se desplaza el documento.
6. Cambiar inversión durante un renderizado.
7. Cerrar y reabrir para comprobar persistencia de tema e inversión.
8. Activar pantalla encendida y esperar más que el tiempo habitual de apagado.
9. Desactivar pantalla encendida y comprobar el comportamiento normal del sistema.
10. Salir del visor y verificar que la pantalla no queda forzada a permanecer activa.
11. Corromper una preferencia y comprobar la restauración de valores seguros.
12. Verificar ausencia de conexiones de red.

## Decisiones de alcance confirmadas

- La especificación completa queda aprobada para implementación.
- Se mantienen los umbrales de 300 ms para cambios de tema y 500 ms para
  inversión, excluyendo renderizados completos.
- Las preferencias de tema, inversión y pantalla encendida son globales y
  persistentes.
