# Memoria del proyecto

- Proyecto: Lector PDF; paquete `com.example.lectorpdf`; `minSdk 24`.
- UI: XML/View Binding. Acceso exclusivamente mediante URIs del sistema.
- Sin cuentas, anuncios, telemetría, permisos amplios de almacenamiento ni red.
- Motor visual: `android.graphics.pdf.PdfRenderer`.
- Spec 001: selector del sistema, `ACTION_VIEW` y renderizado bajo demanda.
- Spec 002: desplazamiento continuo, indicador de página, salto numérico,
  zoom 1x-5x y orientación automática/vertical/horizontal.
- Spec 003: temas claro/oscuro/sepia, inversión visual y pantalla encendida.
- Spec 004: recientes hasta 20, URI no disponible, borrado local y restauración.
- La restauración persiste por URI completa y protege el destino frente a
  callbacks tardíos, pausas, rotaciones y layouts antiguos.
- La entrada reciente solo se guarda tras crear un `PdfRenderer` válido.
- Los insets y `setDecorFitsSystemWindows(true)` evitan solapes en Redmi.
- Límite actual de bitmap: 6.000.000 píxeles por página.
- Spec 005 implementada y validada: búsqueda local, en vivo, sin OCR,
  navegación sin ciclo y cancelación durante indexación.
- PdfBox-Android 2.0.27.0 (Apache 2.0) extrae texto; `PdfRenderer` sigue
  renderizando. La primera indexación recorre el PDF una sola vez.
- El índice de búsqueda muestra progreso `página actual / total`; las
  consultas siguientes reutilizan el índice.
- La indexación de búsqueda admite cancelación cooperativa al cerrar el diálogo;
  la señal se comprueba durante la extracción y se descartan resultados tardíos.
- El resaltado usa `TextPosition`, `cropBox`, rotación y
  `yDirAdj - heightDir`; fue validado alineado sobre la palabra.
- Spec 005 validada manualmente con PDF escaneado, consulta sin resultados,
  ausencia de red y cancelación durante indexación.
- El salto de página usa una casilla numérica visible; la lupa busca texto.
- Spec 006 implementada y validada: índice interno jerárquico y marcadores
  personales.
- Marcadores personales implementados por URI/página, con nombre opcional,
  actualización sin duplicados, consulta, salto y eliminación local.
- La navegación inferior conserva solo `Todos` y `Reciente`; se eliminó la
  pestaña redundante `Marcados`. Los marcadores personales se consultan desde
  `Mis marcadores` en el menú desplegable del visor.
- El menú desplegable del visor agrupa sus opciones en `Orientación`,
  `Apariencia y pantalla` y `Marcadores`, reduciendo su altura inicial y
  facilitando el uso con una mano en pantallas pequeñas.
- Índice interno implementado y validado manualmente con lectura recursiva local
  mediante PdfBox, sangría jerárquica y destinos de página.
- La compilación validada es `.\gradlew.bat assembleDebug --offline`.
- `AGENTS.md` y `docs/constitution.md` contienen ahora las reglas reales del
  proyecto, sin introducir dependencias ni comportamiento de aplicación.
- Los tres PDFs del curso se analizaron localmente, sin subirlos a servicios.
- Las prácticas aplicables están en `docs/guia-desarrollo-con-ia.md`:
  SDD spec-anchored, EARS, guardarraíles, contexto y validación por requisito.
- La guía adapta los ejemplos web al stack Android y no introduce cambios de
  código ni dependencias nuevas.
- Se creó `AGENTS.md` con stack, comandos, privacidad, dependencias y verificación.
- Se creó `.agents/skills/pdf-android-reader/SKILL.md` con reglas del visor.
- Se creó `docs/validacion-specs.md` con casos manuales por spec y privacidad.
- Riesgo vigente: PdfBox puede consumir memoria elevada en PDFs de más de 1000
  páginas; la cancelación cooperativa reduce el coste al cerrar la búsqueda.
- Specs 001-006 están implementadas y validadas manualmente, incluida la
  regresión de eliminación de marcadores confirmada por el usuario.
- Spec 007 implementada y validada manualmente; plan, tareas y checklist
  actualizados.
- Se inició la Spec 007 para control de brillo temporal dentro del visor:
  porcentaje local de ventana, rango 20%-100%, ubicado en `Apariencia y
  pantalla`, con restauración del brillo inicial y sin permisos ni persistencia.
- Spec 007: T1-T5 completadas; el brillo se aplica a la ventana, se puede
  restablecer y se restaura en `onStop`/`onDestroy`.
- La eliminación de marcadores ahora usa selección explícita, habilita
  `Eliminar` solo tras seleccionar y refresca `Mis marcadores` después del
  borrado; el PDF original no se modifica.
- Se retiró la opción redundante `Pantalla completa` del menú; el visor
  conserva el cambio de pantalla completa mediante toque sobre el documento.
- Spec 008 aprobada e implementada parcialmente: la pantalla principal permite
  elegir una carpeta con SAF, escanea PDFs recursivamente fuera del hilo
  principal y conserva el selector de archivo individual.
- Los títulos de Recientes ahora se resuelven desde `DISPLAY_NAME` cuando la
  apertura no envía `EXTRA_TITLE`, incluyendo registros antiguos guardados como
  `PDF`.
- Pendiente validar manualmente la carpeta concedida, revocación, subcarpetas,
  cancelación, privacidad y títulos de Recientes.
- Spec 009 en implementación: Recientes carga disponibilidad y nombres en
  `Dispatchers.IO`, los adaptadores liberan sus ejecutores al destruirse la
  vista y el escaneo SAF admite cancelación y profundidad máxima.
- La auditoría técnica detectó además que `lint --offline` no puede completar
  porque falta `androidx.collection:collection-ktx:1.1.0` en la caché local;
  `assembleDebug --offline` sí continúa correcto.
