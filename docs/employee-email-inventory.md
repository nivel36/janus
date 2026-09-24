# Inventario de uso de `employeeEmail`

El identificador empresarial estable de una persona empleada es `employeeNumber`; la
clave interna entre agregados es `employee.id`. El correo es mutable y no debe
utilizarse para enlazar fichajes, turnos, horarios, centros o usuarios ya asociados.

## API y documentación OpenAPI

`apps/backend/src/main/resources/janus.yaml` expone `employeeEmail` únicamente como:

- filtro opcional de `GET /worksites/`, `GET /schedules/` y `GET /timelogs/`;
- dato de salida en `ClockOutWithoutClockInEventResponse` y `TimeLogResponse`;
- dato de entrada/salida del empleado;
- selector de compatibilidad en las rutas de fichaje `clock-in` y `clock-out`.

Las interfaces HTTP Java contienen además selectores de compatibilidad por correo en
las rutas de fichajes, eventos de salida sin entrada y asignaciones de centros. Estos
selectores se resuelven una sola vez a `Employee` en el controlador; desde ese límite,
servicios y repositorios usan la entidad o `employee.id`. Su sustitución pública por
`employeeNumber` requiere una versión nueva del contrato HTTP.

## Consultas internas

Las únicas consultas internas que comparan correo son filtros explícitos:

- `TimeLogSearchSpecifications.matching`, para `TimeLogSearchCriteria.employeeEmail`;
- `ScheduleRepository.search`, para el filtro de horarios asignados;
- `WorksiteRepository.search`, para el filtro de centros asignados;
- `EmployeeRepository.findByEmail`/`findIdByEmail`, para resolver entrada HTTP y para
  la asociación inicial de `AppUser`.

Las consultas operativas de `WorkshiftRepository`, `TimeLogRepository` y
`ScheduleRepository.findTimeRangeForDate` usan `employee.id`. Las comprobaciones de
asignación usan también `employee.id`; por tanto, cambiar el correo no modifica las
relaciones persistidas.
