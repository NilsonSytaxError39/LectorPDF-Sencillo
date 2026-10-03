# Spec 008: Biblioteca local de PDFs y títulos en recientes

Estado: implementándose

## Dependencias

- `specs/001-apertura-y-renderizado/spec.md`
- `specs/004-estado-recientes-y-restauracion/spec.md`

## Contexto y objetivo

La pantalla principal debe mostrar los PDFs de una carpeta elegida
explícitamente por el usuario, sin solicitar permisos amplios de
almacenamiento. La lista debe conservar la apertura mediante URI y mostrar el
nombre real del documento en la biblioteca y en Recientes.

## Decisiones de privacidad

- EL SISTEMA solicitará acceso únicamente mediante el selector de carpetas del
  sistema.
- EL SISTEMA persistirá solo la URI de la carpeta elegida y el estado mínimo
  necesario para mostrar sus PDFs.
- EL SISTEMA no solicitará `MANAGE_EXTERNAL_STORAGE`, `READ_MEDIA_*` ni
  permisos amplios equivalentes.
- El escaneo será local y no realizará conexiones de red.

## Requisitos funcionales

- **F-801** — CUANDO el usuario pulse la acción de buscar PDFs, EL SISTEMA
  abrirá el selector de carpetas del sistema.
- **F-802** — CUANDO el usuario conceda una carpeta, EL SISTEMA conservará el
  acceso persistente a esa carpeta y buscará recursivamente archivos PDF.
- **F-803** — MIENTRAS exista una carpeta concedida, EL SISTEMA mostrará sus
  PDFs en la pantalla donde estaba la acción `Abrir PDF`.
- **F-804** — CUANDO el usuario seleccione un PDF de la biblioteca, EL SISTEMA
  abrirá ese PDF usando su URI y la concesión de lectura disponible.
- **F-805** — SI la carpeta no está disponible o no contiene PDFs, ENTONCES EL
  SISTEMA mostrará un estado vacío explícito y permitirá seleccionar otra
  carpeta.
- **F-806** — CUANDO el usuario cancele el selector, EL SISTEMA conservará la
  biblioteca anterior y no mostrará un error de éxito.
- **F-807** — CUANDO el visor reciba una URI sin título, EL SISTEMA obtendrá el
  nombre mediante `OpenableColumns.DISPLAY_NAME` y lo usará al guardar el
  reciente.
- **F-808** — CUANDO un reciente tenga un título válido, EL SISTEMA mostrará
  ese título en la tarjeta de Recientes.
- **F-809** — SI no se puede obtener el nombre del PDF, ENTONCES EL SISTEMA
  mostrará `PDF` como texto de respaldo y conservará la URI.

## Requisitos no funcionales

- **NF-801** — EL SISTEMA ejecutará el recorrido de carpetas fuera del hilo
  principal.
- **NF-802** — EL SISTEMA no cargará el contenido completo de los PDFs para
  construir la biblioteca.
- **NF-803** — EL SISTEMA no modificará, copiará ni eliminará los archivos
  originales.
- **NF-804** — EL SISTEMA mantendrá la apertura directa mediante selector de
  archivo como comportamiento compatible.

## Casos límite

- El usuario selecciona una carpeta sin PDFs.
- La carpeta contiene subcarpetas anidadas.
- La carpeta incluye un PDF cuyo proveedor no informa tamaño o fecha.
- El usuario revoca el acceso a la carpeta.
- El usuario selecciona una carpeta diferente.
- El nombre contiene caracteres Unicode o el carácter `|`.
- El PDF se abre desde `Abrir con` sin `EXTRA_TITLE`.
- El PDF se abre desde la nueva biblioteca y desde Recientes.

## Fuera de alcance

- Escanear todo el almacenamiento sin una carpeta elegida.
- Solicitar permisos amplios del sistema.
- Sincronizar, copiar o subir PDFs.
- Indexar el texto de todos los PDFs automáticamente.
- Cambiar la política de máximo 20 recientes.

## Criterios de finalización

- La pantalla principal permite seleccionar una carpeta y muestra sus PDFs.
- El escaneo recursivo funciona con subcarpetas y no bloquea la interfaz.
- La cancelación y la carpeta revocada tienen estados explícitos.
- Los títulos aparecen en las tarjetas de biblioteca y Recientes.
- Las aperturas desde selector, biblioteca, Recientes y `Abrir con` conservan
  la URI y el nombre disponible.
- La compilación offline termina correctamente.
- No se añade permiso amplio de almacenamiento ni dependencia nueva.

## Dudas abiertas

- [NECESITA ACLARACIÓN] Ninguna; el alcance de acceso fue confirmado por el
  usuario.
