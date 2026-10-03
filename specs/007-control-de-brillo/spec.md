# Spec 007: Control de brillo durante la lectura

Estado: implementada y validada manualmente

## Dependencias

- `specs/002-navegacion-zoom-y-orientacion/spec.md`
- `specs/003-modos-lectura-y-pantalla/spec.md`

## Contexto y objetivo

Esta especificación define un control de brillo local para mejorar la
legibilidad durante sesiones prolongadas sin cambiar permanentemente la
configuración del dispositivo ni solicitar permisos adicionales. El control
solo estará disponible mientras el usuario lea un PDF y restaurará el brillo
que existía antes de abrir el visor al salir.

## Usuarios / actores

- Usuario que lee un PDF en el visor.

## Historias de usuario

- **HU-701:** Como lector, quiero ajustar el brillo dentro del visor para
  adaptar la pantalla a mi entorno sin cambiar permanentemente el dispositivo.
- **HU-702:** Como lector, quiero recuperar el brillo anterior al salir para que
  la aplicación no modifique mi configuración personal.

## Definiciones

- **Brillo de lectura:** nivel aplicado únicamente a la ventana del visor.
- **Brillo inicial:** nivel de brillo de ventana existente al abrir el PDF; si
  el sistema usa brillo automático, se representa como el valor efectivo
  disponible para la ventana.
- **Rango permitido:** valores entre 20% y 100%, incluidos.

## Requisitos funcionales

- **F-701** — EL SISTEMA ofrecerá un control de brillo dentro de
  `Apariencia y pantalla` mientras exista un PDF abierto.
- **F-702** — CUANDO el usuario abra el control de brillo, EL SISTEMA mostrará
  el nivel actual de la ventana y permitirá modificarlo mediante un control
  deslizante.
- **F-703** — EL SISTEMA limitará el brillo de lectura al rango entre 20% y
  100%, incluidos.
- **F-704** — CUANDO el usuario modifique el control, EL SISTEMA aplicará el
  nuevo brillo a la ventana del visor sin cambiar la configuración global del
  dispositivo.
- **F-705** — CUANDO el visor se abra, EL SISTEMA iniciará el control con el
  brillo actual de la ventana.
- **F-706** — CUANDO el usuario seleccione `Restablecer brillo`, EL SISTEMA
  restaurará el brillo inicial capturado al abrir el visor.
- **F-707** — CUANDO el usuario salga del visor, EL SISTEMA restaurará el brillo
  inicial de la ventana y no conservará el nivel temporal como preferencia.
- **F-708** — SI el control de brillo no puede aplicarse, ENTONCES EL SISTEMA
  mantendrá el brillo vigente, mostrará un error explícito y mantendrá abierto
  el PDF.
- **F-709** — EL SISTEMA no solicitará permisos para modificar el brillo del
  sistema.

## Requisitos no funcionales

- **NF-701** — EL SISTEMA aplicará un cambio confirmado del control en un
  máximo de 100 ms, excluyendo limitaciones del dispositivo.
- **NF-702** — EL SISTEMA no realizará conexiones de red para mostrar, aplicar
  o restaurar el brillo.
- **NF-703** — EL SISTEMA no persistirá el nivel temporal de brillo.
- **NF-704** — El control deberá ser accesible con una mano y conservar el
  comportamiento existente de `Apariencia y pantalla`.

## Casos límite

- El brillo inicial está en el mínimo o máximo permitido.
- El sistema tiene brillo automático activo.
- El usuario rota el dispositivo durante la lectura.
- El usuario activa pantalla completa y después modifica el brillo.
- El usuario sale mediante atrás, inicio, otra aplicación o cierre del visor.
- El usuario intenta establecer un valor menor que 20%.
- El usuario abre y cierra el control sin modificarlo.
- La ventana deja de estar disponible durante la restauración.

## Fuera de alcance

- Cambiar el brillo global del dispositivo.
- Solicitar `WRITE_SETTINGS` u otro permiso equivalente.
- Guardar una preferencia de brillo entre sesiones.
- Control automático basado en hora, sensor de luz o batería.
- Modificar el brillo de otras aplicaciones o ventanas.

## Criterios de finalización

- Todos los requisitos F-701 a F-709 tienen una comprobación.
- El control aparece dentro de `Apariencia y pantalla`.
- El brillo se mantiene entre 20% y 100%.
- El brillo inicial se restaura al salir por las rutas principales.
- No se añade permiso de configuración del sistema.
- La compilación offline termina correctamente.
- Se valida manualmente en modo claro, oscuro, sepia, pantalla completa y tras
  rotación.

## Dudas abiertas

- [NECESITA ACLARACIÓN] Ninguna; las decisiones de alcance fueron confirmadas
  por el usuario.
