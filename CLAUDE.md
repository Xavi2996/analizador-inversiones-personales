# Analizador de Inversiones Personales

Aplicación web personal para registrar, consultar y analizar un portafolio de inversiones.

El proyecto sigue desarrollo guiado por especificaciones.

## Stack

Backend:

- Java 21
- Spring Boot
- Maven
- Spring Web
- Spring Data JPA
- Validation
- PostgreSQL
- Flyway

Frontend:

- Angular
- TypeScript
- Standalone Components
- Reactive Forms
- Signals cuando aporten valor
- HttpClient
- Router

## Estructura

```text
backend/          Backend Spring Boot
frontend/         Frontend Angular
documentacion/    Decisiones funcionales y técnicas
specs/            Especificaciones de funcionalidades
referencia/       Excel y prototipo visual
```

## Documentación del proyecto

Antes de tomar decisiones relevantes, consulta la documentación relacionada.

@documentacion/00-alcance-mvp.md
@documentacion/01-modelo-funcional.md
@documentacion/02-base-datos.md
@documentacion/03-arquitectura.md
@documentacion/04-contratos-api.md
@documentacion/05-guia-interfaz.md
@documentacion/06-convenciones.md
@documentacion/07-hoja-ruta.md

No dupliques aquí reglas que ya estén definidas en esos documentos.

## Prioridad de fuentes

Cuando exista información contradictoria, utiliza este orden:

1. especificación aprobada de la funcionalidad;
2. documentación funcional;
3. contratos API;
4. modelo de base de datos;
5. arquitectura;
6. convenciones;
7. prototipo visual;
8. criterio técnico de implementación.

No permitas que el prototipo visual defina reglas de negocio.

## Flujo de desarrollo

Para funcionalidades importantes:

```text
SPEC
↓
REVISIÓN
↓
APROBACIÓN
↓
IMPLEMENTACIÓN
↓
TEST
↓
COMPARACIÓN CON EXCEL
↓
COMMIT
```

No implementes una regla de negocio importante si todavía está sin definir en la especificación.

Si falta una decisión funcional, indícala en lugar de inventarla.

## Especificaciones

Las especificaciones viven en:

```text
specs/
```

Antes de implementar una funcionalidad, revisa su especificación.

Una especificación aprobada tiene prioridad sobre supuestos derivados del código o del prototipo.

## Referencia funcional

La referencia numérica del MVP es:

```text
referencia/registro_inversiones_v4_1.xlsx
```

Cuando implementes cálculos financieros:

1. implementa la regla en backend;
2. crea pruebas;
3. compara el resultado con el Excel;
4. investiga diferencias relevantes.

El Excel define comportamiento esperado, no arquitectura.

## Referencia visual

La referencia visual aprobada es:

```text
referencia/prototipo-interfaz/Prototipo Interfaz v2.dc.html
```

Úsala como guía, no como código Angular para copiar literalmente.

La implementación puede ajustarse cuando mejore:

- usabilidad;
- responsive;
- accesibilidad;
- mantenibilidad.

Los cambios visuales globales deben reflejarse en la documentación correspondiente.

## Backend

Organiza el código por funcionalidad.

Ejemplo:

```text
com.personal.inversiones
├── activos
├── aportes
├── transacciones
├── referidos
├── dividendos
├── precios
├── portafolio
└── compartido
```

No introduzcas capas o abstracciones sin una necesidad concreta.

Reglas obligatorias:

- cálculos financieros en backend;
- usar `BigDecimal` para dinero, precios y cantidades;
- no usar `double` ni `float` para cálculos financieros;
- no exponer entidades JPA directamente por la API;
- usar DTO de entrada y salida;
- Flyway controla el esquema;
- Hibernate valida el esquema.

Configuración esperada:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

No utilizar:

```properties
spring.jpa.hibernate.ddl-auto=update
```

## Frontend

Organiza Angular por funcionalidad.

No conviertas el frontend en la fuente de verdad de cálculos financieros.

Usa:

- Standalone Components;
- Reactive Forms;
- Signals cuando simplifiquen el estado;
- componentes reutilizables cuando exista reutilización real.

Evita componentes excesivamente grandes, pero tampoco fragmentes componentes sin necesidad.

## Base de datos

PostgreSQL es la base de datos del MVP.

Migraciones Flyway:

```text
V1__crear_tabla_activo.sql
V2__crear_tabla_aporte.sql
...
```

No modifiques una migración que ya haya sido ejecutada.

Para cambios posteriores crea una nueva migración.

## API

Prefijo:

```text
/api/v1
```

Los endpoints representan recursos.

Preferir:

```text
POST /api/v1/aportes
```

Evitar:

```text
POST /api/v1/registrarAporte
```

JSON en `camelCase`.

No expongas stack traces, SQL ni mensajes internos de excepciones.

## Git

Formato de commits:

```text
tipo: descripcion
```

Tipos habituales:

```text
feat
fix
docs
test
refactor
chore
```

Ejemplos:

```text
feat: implementar registro de activos
fix: corregir calculo de efectivo
docs: actualizar contratos de api
test: agregar pruebas de posiciones
```

No mezcles cambios funcionalmente independientes en un mismo commit cuando pueda evitarse.

## Documentación viva

Después de una decisión relevante, comprueba si debe actualizarse documentación.

Ejemplos:

```text
nuevo campo de negocio
→ revisar modelo funcional, base de datos y API

nuevo endpoint
→ actualizar contratos API

cambio de tabla
→ actualizar modelo de base de datos

cambio arquitectónico
→ actualizar arquitectura

cambio visual global
→ actualizar guía de interfaz (y DESIGN.md cuando exista)

nueva regla financiera
→ actualizar spec y documentación funcional cuando corresponda
```

Al terminar una tarea informa:

```text
Impacto en documentación:
- archivos que deben actualizarse
- motivo
```

Si no existe impacto:

```text
Impacto en documentación:
Ninguno.
```

## No hacer

No:

- inventes reglas de negocio;
- agregues funcionalidades fuera del MVP sin indicación;
- introduzcas microservicios, CQRS, event sourcing u otras arquitecturas complejas sin necesidad;
- modifiques contratos API silenciosamente;
- cambies tablas sin migración Flyway;
- copies el prototipo directamente como implementación Angular;
- hagas refactors grandes no relacionados con la tarea actual;
- agregues dependencias sin justificar su necesidad;
- modifiques documentación únicamente por cambios internos que no alteren decisiones;
- incluyas secretos, credenciales ni rutas personales en archivos versionados.

## Decisiones todavía pendientes

No asumir una respuesta definitiva para:

- método de costo de las ventas;
- validaciones completas de venta;
- corrección de movimientos históricos;
- confirmación de fotografías;
- umbral definitivo de desviación de asignación.

Estas decisiones deben resolverse en sus respectivas especificaciones antes de implementarlas.

## Antes de terminar una tarea

Verifica:

```text
[ ] compila
[ ] pruebas pasan
[ ] cumple la especificación
[ ] contratos API siguen siendo correctos
[ ] migraciones son correctas
[ ] resultados financieros son coherentes
[ ] no se agregó complejidad innecesaria
[ ] se revisó el impacto en documentación
```

Prioriza siempre:

```text
claridad > simplicidad > consistencia > abstracción prematura
```
