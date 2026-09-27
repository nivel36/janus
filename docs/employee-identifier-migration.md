# Migración de rutas de empleado a `employeeNumber`

`employeeNumber` es el identificador público canónico de empleado. Las rutas de
fichajes, eventos de salida sin entrada y asignaciones de centros deben usarlo;
el backend resuelve ese número una vez y los servicios continúan operando con la
entidad y su ID interno. El correo se conserva como dato del empleado y como
filtro explícito en las búsquedas que lo admiten.

## Compatibilidad obsoleta

Durante la transición se mantienen las variantes que reciben `employeeEmail`.
Están marcadas como `deprecated: true` en `janus.yaml` y no deben utilizarse en
integraciones nuevas. Se retirarán en la próxima versión mayor de la API:

- `/employees/{employeeEmail}/time-logs/**`
- `/employees/{employeeEmail}/clock-out-without-clock-in-events/**`
- `/worksites/{worksiteCode}/employees/{employeeEmail}`

Los consumidores deben obtener `employeeNumber` desde `EmployeeResponse` y
migrar a la operación equivalente antes de esa retirada. El servidor resuelve
primero por número y solo intenta el correo como mecanismo transitorio.
