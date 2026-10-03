# Spec 009: Endurecimiento antes de distribución

Estado: implementándose

## Contexto

Antes de preparar una versión para distribución, el lector debe evitar trabajo
de I/O en el hilo principal, liberar recursos de vistas destruidas y proteger
el escaneo local frente a carpetas grandes o profundamente anidadas.

## Requisitos funcionales

- **F-901** — CUANDO se cargue la lista de Recientes, EL SISTEMA resolverá
  disponibilidad y nombres sin bloquear el hilo principal.
- **F-902** — CUANDO una vista con miniaturas se destruya, EL SISTEMA cancelará
  o apagará sus ejecutores y no actualizará vistas destruidas.
- **F-903** — MIENTRAS se escanee una carpeta local, EL SISTEMA permitirá la
  cancelación asociada al ciclo de vida de la vista.
- **F-904** — SI el escaneo encuentra una estructura excesivamente profunda o
  inaccesible, ENTONCES EL SISTEMA omitirá esa rama, conservará los resultados
  válidos y mostrará un estado explícito si no quedan resultados.

## Requisitos no funcionales

- **NF-901** — EL SISTEMA no realizará I/O de disponibilidad de Recientes en
  el hilo principal.
- **NF-902** — EL SISTEMA no conservará ejecutores activos después de destruir
  la vista que los creó.
- **NF-903** — EL SISTEMA mantendrá el funcionamiento offline y no añadirá
  permisos amplios.
- **NF-904** — EL SISTEMA no modificará los PDFs originales.

## Backup

La decisión sobre `allowBackup` y las URIs persistidas queda documentada como
decisión de distribución pendiente; no se cambiará automáticamente dentro de
esta spec.

## Criterios de finalización

- Recientes se cargan usando trabajo fuera del hilo principal.
- Los adaptadores cierran sus ejecutores en el ciclo de vida apropiado.
- El escaneo puede cancelarse y no produce actualizaciones tardías.
- La compilación offline termina correctamente.
- La decisión de backup queda registrada para la Spec 009 de distribución.
