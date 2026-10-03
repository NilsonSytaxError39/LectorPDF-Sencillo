# Tareas — Spec 007 Control de brillo

- [x] **T1. Añadir acciones de brillo al menú y textos de interfaz.** F-701,
  F-702, F-706, NF-704.
  - Hecho cuando: el menú contiene `Brillo de lectura` y
    `Restablecer brillo` dentro de `Apariencia y pantalla`, con textos
    reutilizables y sin permisos nuevos.

- [x] **T2. Implementar el estado temporal y captura del brillo inicial.** F-703,
  F-705, F-707, F-709, NF-703.
  - Hecho cuando: la actividad captura el valor inicial de ventana, mantiene
    el rango 20%-100% y no persiste ni modifica el brillo global.

- [x] **T3. Implementar el diálogo interactivo y la aplicación del valor.**
  F-702, F-703, F-704, NF-701.
  - Hecho cuando: mover el control aplica el porcentaje a la ventana y muestra
    el valor actual sin bloquear el visor.

- [x] **T4. Implementar restablecimiento, ciclo de vida y errores.** F-706,
  F-707, F-708.
  - Hecho cuando: restablecer y salir restauran exactamente el valor inicial,
    incluyendo brillo automático, y los fallos muestran un mensaje explícito.

- [x] **T5. Validar la Spec 007 y actualizar documentación.** F-701 a F-709,
  NF-701 a NF-704.
  - Hecho cuando: la compilación offline termina correctamente, el checklist
    manual fue completado por el usuario y la spec quedó marcada como validada.
