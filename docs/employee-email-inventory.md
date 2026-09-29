# Inventario de uso de `employeeEmail`

El identificador empresarial estable de una persona empleada es `employeeNumber`; la
clave interna entre agregados es `employee.id`. El correo es mutable y no debe
utilizarse para enlazar fichajes, turnos, horarios, centros o usuarios ya asociados.

## API y documentación OpenAPI

`apps/backend/src/main/resources/janus.yaml` expone `employeeEmail` únicamente como:

- filtro opcional de `GET /worksites/`, `GET /schedules/` y `GET /timelogs/`;
- dato de entrada/salida del empleado;
- filtro opcional de las búsquedas que lo soportan.

Las interfaces HTTP Java usan `employeeNumber` para las operaciones sobre fichajes,
eventos de salida sin entrada y asignaciones de centros.

## Consultas internas

Las únicas consultas internas que comparan correo son filtros explícitos:

- `TimeLogSearchSpecifications.matching`, para `TimeLogSearchCriteria.employeeEmail`;
- `ScheduleRepository.search`, para el filtro de horarios asignados;
- `WorksiteRepository.search`, para el filtro de centros asignados;
- `EmployeeRepository.findByEmail`, para la gestión del empleado.

`EmployeeRepository.findIdByEmail` se eliminó al quedar sin consumidores. La
asociación inicial de `AppUser` resuelve exclusivamente `employeeNumber`; por eso se
mantiene la unicidad de correo mientras sigan activos los selectores y validaciones
heredados, y se conserva siempre `UK_EMPLOYEE_NUMBER`.

Las consultas operativas de `WorkshiftRepository`, `TimeLogRepository` y
`ScheduleRepository.findTimeRangeForDate` usan `employee.id`. Las comprobaciones de
asignación usan también `employee.id`; por tanto, cambiar el correo no modifica las
relaciones persistidas.
