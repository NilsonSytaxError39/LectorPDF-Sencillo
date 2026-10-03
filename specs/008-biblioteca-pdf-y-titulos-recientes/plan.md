# Plan — Spec 008 Biblioteca local de PDFs y títulos en recientes

1. Definir el modelo de estado de la carpeta concedida y su restauración.
2. Conectar `ACTION_OPEN_DOCUMENT_TREE` con el escáner local existente.
3. Ejecutar el escaneo recursivo fuera del hilo principal y mostrar estados
   vacío, cargando y error.
4. Mantener la apertura por URI y el selector de archivo existente.
5. Resolver el nombre del documento desde la URI antes de registrar Recientes.
6. Añadir pruebas manuales de carpeta, revocación, títulos y privacidad.
7. Compilar offline y revisar que no se añadan permisos amplios.
