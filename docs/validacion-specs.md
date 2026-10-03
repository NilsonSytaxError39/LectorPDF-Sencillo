# Checklist de validación por spec

Este checklist complementa los criterios de cada spec. La compilación verde no
sustituye las pruebas manuales del visor en un dispositivo real.

## Registro de una validación

- Fecha:
- Spec:
- Versión o APK:
- Dispositivo/emulador:
- PDF utilizado y número de páginas:
- Resultado: pendiente | aprobado | cambios necesarios
- Observaciones:

## Spec 001 — Apertura y renderizado

- [ ] Abrir un PDF mediante el selector del sistema.
- [ ] Abrir un PDF desde `Abrir con`.
- [ ] Rechazar una URI no disponible sin cerrar inesperadamente la app.
- [ ] Confirmar que no se solicita almacenamiento amplio.
- [ ] Abrir un documento grande sin cargar todas sus páginas de una vez.

## Spec 002 — Navegación, zoom y orientación

- [ ] Ver el indicador `actual / total`.
- [ ] Escribir una página válida y saltar a ella.
- [ ] Rechazar 0, negativos, texto y números mayores que el total.
- [ ] Probar desplazamiento continuo.
- [ ] Probar zoom con pellizco y doble toque.
- [ ] Probar orientación automática, vertical y horizontal.
- [ ] Repetir después de rotar o recrear la actividad.

## Spec 003 — Modos de lectura y pantalla

- [ ] Probar modo claro, oscuro y sepia.
- [ ] Confirmar contraste de toolbar, texto y contador.
- [ ] Activar y desactivar inversión del PDF.
- [ ] Confirmar que mantener pantalla encendida persiste y se puede desactivar.
- [ ] Probar pantalla completa y restauración de controles.

## Spec 004 — Estado, recientes y restauración

- [ ] Reabrir un PDF y confirmar la última página exacta.
- [ ] Repetir con una página alta, por ejemplo 532 o superior.
- [ ] Confirmar que una pausa, rotación o scroll tardío no sobrescribe el destino.
- [ ] Confirmar recientes sin duplicados y con máximo 20 entradas.
- [ ] Eliminar un reciente y comprobar que no se borra el archivo original.
- [ ] Probar una URI revocada.

## Spec 005 — Búsqueda de texto

- [ ] Buscar texto ignorando mayúsculas y minúsculas.
- [ ] Verificar resultados mientras se escribe.
- [ ] Saltar a cada coincidencia sin ciclo inesperado.
- [ ] Confirmar resaltado alineado sobre la palabra.
- [x] Buscar en un PDF escaneado y mostrar el mensaje de ausencia de texto.
- [x] Buscar un término inexistente y mostrar cero resultados claramente.
- [x] Probar un PDF de más de 1000 páginas y observar el progreso.
- [x] Cerrar la búsqueda durante la indexación y confirmar que el procesamiento
  se detiene y no reaparece ningún resultado.
- [x] Confirmar que las consultas no requieren red.

## Spec 006 — Índice y marcadores

- [ ] Guardar un marcador personal con nombre.
- [x] Guardar uno sin nombre y comprobar el nombre predeterminado.
- [x] Guardar otra vez la misma página sin crear duplicado.
- [x] Consultar y abrir un marcador desde el menú del visor.
- [x] Eliminar un marcador sin borrar el PDF.
- [x] Regresión: desde `Mis marcadores`, pulsar `Eliminar`, seleccionar un
  marcador, confirmar y comprobar que desaparece al volver a abrir la lista.
- [x] Cerrar y reabrir el PDF y confirmar persistencia.
- [x] Abrir un PDF con índice jerárquico.
- [x] Abrir un PDF sin índice y mostrar el mensaje correspondiente.
- [x] Probar un destino de índice no resoluble sin cerrar el visor.

## Privacidad y regresión

- [ ] Confirmar ausencia de tráfico de red durante apertura, lectura y búsqueda.
- [ ] Confirmar que no se crean permisos amplios de almacenamiento.
- [ ] Confirmar que el PDF original no se modifica.
- [x] Ejecutar `.\gradlew.bat assembleDebug --offline`.
- [ ] Revisar errores y advertencias nuevas antes de entregar.

## Spec 008 — Biblioteca local y títulos en recientes

- [ ] Elegir una carpeta mediante el selector del sistema.
- [ ] Mostrar PDFs de la carpeta y sus subcarpetas.
- [ ] Abrir un PDF desde la biblioteca.
- [ ] Cancelar el selector y conservar la biblioteca anterior.
- [ ] Probar una carpeta vacía y mostrar el estado correspondiente.
- [ ] Revocar el acceso a la carpeta y mostrar un error explícito.
- [ ] Abrir un PDF sin título adicional y comprobar el nombre en Recientes.
- [ ] Confirmar que no se solicita permiso amplio ni se modifica el PDF.

## Spec 009 — Endurecimiento antes de distribución

- [ ] Abrir Recientes con 20 entradas y confirmar que la interfaz no se
  bloquea.
- [ ] Cambiar varias veces entre `Todos`, `Reciente` y `Marcados` sin errores.
- [ ] Escanear una carpeta con subcarpetas y cancelar o abandonar la pantalla.
- [ ] Confirmar que no quedan actualizaciones después de destruir la vista.
- [ ] Decidir explícitamente la política de backup antes de la release.

## Spec 007 — Control de brillo

- [x] Abrir `Apariencia y pantalla` y mostrar `Brillo de lectura`.
- [x] Ajustar 20%, 50% y 100% y comprobar aplicación inmediata.
- [x] Confirmar que el brillo no cambia fuera del visor.
- [x] Usar `Restablecer brillo` y recuperar el valor inicial.
- [x] Salir mediante atrás, inicio y otra aplicación; confirmar restauración.
- [x] Rotar el dispositivo y confirmar restauración sin cambiar el brillo global.
- [x] Probar con brillo automático, modo claro, oscuro, sepia y pantalla completa.
- [x] Confirmar que no aparece un permiso de configuración del sistema.
