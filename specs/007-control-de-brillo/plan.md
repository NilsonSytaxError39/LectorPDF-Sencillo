# Plan técnico — Spec 007 Control de brillo

## Alcance de implementación

Añadir un control temporal del brillo de la ventana del visor, accesible desde
`Apariencia y pantalla`, sin modificar el brillo global del dispositivo, sin
permisos nuevos y sin persistencia del nivel elegido.

## Archivos y responsabilidades

- `app/src/main/res/menu/pdf_viewer_menu.xml`
  - Añadir la entrada `Brillo de lectura` dentro del submenú de apariencia.
  - Añadir la acción `Restablecer brillo` en el mismo grupo.
- `app/src/main/java/com/example/lectorpdf/PdfViewerActivity.kt`
  - Capturar el brillo inicial de la ventana.
  - Mostrar el control deslizante y el valor actual.
  - Aplicar valores entre 20% y 100% únicamente a
    `window.attributes.screenBrightness`.
  - Restaurar el valor inicial al salir por las rutas de ciclo de vida.
  - Mostrar errores explícitos si la ventana rechaza el cambio.
- `app/src/main/res/values/strings.xml`
  - Añadir textos reutilizables para título, porcentaje, restablecimiento y
    errores.
- `specs/007-control-de-brillo/spec.md`
  - Mantener el contrato y marcarlo implementado después de la validación.
- `docs/validacion-specs.md`
  - Añadir la lista de pruebas manuales de brillo.
- `MEMORY.md`
  - Registrar la decisión y el estado final.

## Modelo de brillo

Usar `WindowManager.LayoutParams.screenBrightness`, cuyo rango efectivo de
ventana es `0f..1f`.

1. Capturar el valor original al crear el visor.
2. Si el valor original es negativo, conservarlo como `-1f` para respetar el
   comportamiento automático del sistema; para el control visual usar un valor
   efectivo entre 20% y 100%.
3. Convertir el control entero de porcentaje a `Float` dividiendo entre 100.
4. Aplicar siempre un mínimo de `0.20f` y un máximo de `1.0f`.
5. Restablecer exactamente el valor original capturado, incluido `-1f`.

No se usará `Settings.System.SCREEN_BRIGHTNESS` ni se solicitará
`WRITE_SETTINGS`, porque eso cambiaría la configuración global y contradice la
Spec 007.

## Interfaz

El menú mantendrá sus grupos actuales. `Brillo de lectura` abrirá un diálogo
compacto con:

- Título `Brillo de lectura`.
- Porcentaje actual.
- `SeekBar` entre 20 y 100.
- Acción `Restablecer brillo`.
- Acción `Cerrar`.

El valor se aplicará mientras se mueve la barra para permitir ajuste inmediato.
El diálogo no persistirá el valor al cerrarse; el nivel se conserva mientras el
visor siga abierto.

## Ciclo de vida y errores

- Capturar el brillo antes de realizar cambios.
- Restaurar en `onDestroy()` como garantía final.
- Restaurar también al salir del visor mediante `onPause()` sin convertir una
  recreación por rotación en una salida definitiva.
- Si la restauración falla, mostrar un mensaje cuando la ventana siga activa y
  registrar únicamente el tipo de fallo, nunca contenido del PDF o URI.
- Si la aplicación vuelve al visor tras pasar a segundo plano, conservar el
  ajuste temporal de la ventana durante esa instancia.

## Cobertura de requisitos

- F-701, F-702, F-703, F-704, F-705, F-706: menú, diálogo y aplicación local.
- F-707: captura/restauración en ciclo de vida.
- F-708: comprobación de errores y notificación explícita.
- F-709, NF-702, NF-703: uso exclusivo de atributos de ventana, sin permisos,
  red ni almacenamiento.
- NF-701: actualización directa del atributo al cambiar el control.
- NF-704: diálogo compacto dentro del submenú existente.

## Casos a verificar

- Valor original `-1f` por brillo automático.
- Valores iniciales inferiores al mínimo visual.
- Cambios 20%, 50% y 100%.
- Restablecimiento sin modificación previa.
- Salida con atrás, inicio y cierre del visor.
- Rotación y pantalla completa.
- Modo claro, oscuro y sepia.
- Fallo de aplicación/restauración del atributo.

## Alternativas descartadas

- `Settings.System.SCREEN_BRIGHTNESS`: descartado porque modifica el sistema y
  puede requerir permisos especiales.
- Guardar en `SharedPreferences`: descartado porque la Spec exige restaurar el
  brillo inicial y no conservar el nivel temporal.
- Barra permanente junto al contador: descartada porque ocupa espacio durante
  la lectura y contradice la decisión de ubicarlo en el menú desplegable.
