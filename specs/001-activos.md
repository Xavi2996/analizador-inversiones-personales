# SPEC 001 — Activos y asignación objetivo

> **Estado:** Aprobado
> **Depende de:** ninguna
> **Fecha:** 2026-10-26
> **Objetivo:** Registrar, consultar y editar los activos del portafolio y su porcentaje objetivo desde una pantalla Activos dentro del shell base de la aplicación.

## 1. Por qué existe esta spec

Es la primera funcionalidad del MVP (FASE 2 de `documentacion/07-hoja-ruta.md`).

Además del catálogo de activos, establece piezas que reutilizarán las siguientes specs:

- la estructura común de errores de la API (`documentacion/04-contratos-api.md` §7);
- la base de pruebas de integración con PostgreSQL real;
- el shell del frontend (layout, navegación y estilos globales del tema oscuro);
- la conexión de Angular con el backend en desarrollo.

La asignación objetivo entra aquí porque la pantalla Activos la consulta y la edita, como en el prototipo aprobado. Sigue siendo un recurso propio, separado del activo.

Las decisiones de esta spec ya están reflejadas en la documentación general desde el 2026-10-26: `01-modelo-funcional.md` §2.4, `02-base-datos.md` §9 y §15, `04-contratos-api.md` §7, §8, §14 y §20, `03-arquitectura.md`, `05-guia-interfaz.md` y `06-convenciones.md`. Si la implementación cambia alguna decisión, se debe actualizar también esa documentación.

## 2. Alcance

**Incluye:**

- Tabla `activo` y tabla `asignacion_objetivo` mediante migraciones Flyway.
- Crear, listar, consultar y actualizar activos (`/api/v1/activos`).
- Crear o actualizar la asignación objetivo de un activo y listar las asignaciones con su suma (`/api/v1/asignaciones`).
- Manejo global de errores con la estructura común de la API.
- Pruebas de integración del backend con Testcontainers (PostgreSQL 17).
- Estilos globales del tema oscuro, tipografías y tokens de color de `documentacion/05-guia-interfaz.md`.
- Shell del frontend con navegación lateral en escritorio y compacta en móvil.
- Pantalla Activos: tabla (escritorio), tarjetas (móvil), barra de asignación objetivo y panel lateral para registrar y editar.
- Proxy de Angular hacia el backend en desarrollo.

**Fuera del alcance (para futuras specs):**

- Eliminar activos. No existe `DELETE /api/v1/activos/{id}`.
- Estado del activo (activo/inactivo), la columna Estado y las acciones Activar/Desactivar del prototipo.
- Eliminar una asignación. Para excluir un activo del objetivo se asigna 0 %.
- Indicadores "dentro / cerca / fuera del objetivo" (el umbral sigue pendiente; ver `documentacion/05-guia-interfaz.md` §31).
- Porcentaje actual y diferencia contra objetivo (FASE 8, Posiciones).
- Restricciones de edición de un activo con transacciones (spec de Transacciones).
- Ayuda contextual y tutorial "Registrar activo" (FASE 12).
- Monedas distintas de USD.
- La columna `Nota` de la hoja Activos del Excel.
- Retirar las credenciales versionadas de `backend/src/main/resources/application.properties`, que va en una tarea separada.

## 3. Modelo de datos

### 3.1 Migraciones

`backend/src/main/resources/db/migration/V1__crear_tabla_activo.sql`

```sql
CREATE TABLE activo (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    simbolo             VARCHAR(20)  NOT NULL,
    nombre              VARCHAR(150) NOT NULL,
    tipo_activo         VARCHAR(30)  NOT NULL,
    moneda              VARCHAR(10)  NOT NULL,
    fecha_creacion      TIMESTAMPTZ  NOT NULL,
    fecha_actualizacion TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_activo_simbolo UNIQUE (simbolo),
    CONSTRAINT ck_activo_tipo CHECK (tipo_activo IN ('ACCION', 'ETF', 'CRIPTOMONEDA')),
    CONSTRAINT ck_activo_moneda CHECK (moneda IN ('USD'))
);
```

`backend/src/main/resources/db/migration/V2__crear_tabla_asignacion_objetivo.sql`

```sql
CREATE TABLE asignacion_objetivo (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    activo_id           BIGINT       NOT NULL REFERENCES activo (id),
    porcentaje_objetivo NUMERIC(8,4) NOT NULL,
    fecha_creacion      TIMESTAMPTZ  NOT NULL,
    fecha_actualizacion TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_asignacion_activo UNIQUE (activo_id),
    CONSTRAINT ck_asignacion_rango CHECK (porcentaje_objetivo >= 0 AND porcentaje_objetivo <= 100)
);
```

### 3.2 Reglas de los datos

**Símbolo:**

- Obligatorio.
- Se eliminan los espacios de los extremos y se convierte a mayúsculas antes de validar y guardar (`" ivv "` → `"IVV"`).
- Solo admite `A-Z`, `0-9`, `.` y `-`, con una longitud de 1 a 20 caracteres (`^[A-Z0-9.-]{1,20}$` sobre el valor normalizado).
- Es único. La comparación se hace sobre el valor normalizado.
- Se puede modificar mientras el activo no tenga registros históricos relacionados: transacciones, referidos, dividendos o precios.
- Cuando exista al menos uno de esos registros, el símbolo queda bloqueado.
- La asignación objetivo no cuenta como registro histórico y no bloquea el símbolo.
- En esta spec esos módulos todavía no existen, así que el símbolo siempre se puede modificar.
- Cada spec que agregue un módulo histórico deberá incorporar su verificación al bloqueo.

**Nombre:**

- Obligatorio.
- Se eliminan los espacios de los extremos.
- Admite de 1 a 150 caracteres.

**Tipo de activo:**

- Obligatorio.
- Valores permitidos: `ACCION`, `ETF` o `CRIPTOMONEDA`.
- El valor `CRIPTO` del Excel corresponde a `CRIPTOMONEDA`.

**Moneda:**

- Obligatoria.
- Único valor permitido: `USD`.

**Porcentaje objetivo:**

- Obligatorio en `PUT /asignaciones/{activoId}`.
- Debe estar entre 0 y 100, ambos incluidos.
- Admite como máximo 4 decimales, igual que la columna.

**Fechas técnicas:**

- `fecha_creacion` y `fecha_actualizacion` las asigna el backend.
- No se exponen en la API.

**Asignaciones:**

- Un activo sin fila en `asignacion_objetivo` no tiene objetivo. La interfaz lo muestra como `—` y cuenta como 0 % para la suma.
- Crear un activo no crea su asignación.

### 3.3 Backend (`com.personal.inversiones`)

```text
compartido/
  error/ErrorRespuesta.java            // fechaHora, estadoHttp, codigo, mensaje, ruta, erroresCampos
  error/ErrorCampo.java                // campo, mensaje
  error/ManejadorGlobalErrores.java    // @RestControllerAdvice
  error/RecursoNoEncontradoExcepcion.java
  error/ConflictoExcepcion.java
activos/
  controlador/ActivoControlador.java
  servicio/ActivoServicio.java
  repositorio/ActivoRepositorio.java
  modelo/Activo.java
  modelo/TipoActivo.java               // enum ACCION, ETF, CRIPTOMONEDA
  modelo/Moneda.java                   // enum USD
  dto/CrearActivoSolicitud.java
  dto/ActualizarActivoSolicitud.java
  dto/ActivoRespuesta.java
asignaciones/
  controlador/AsignacionObjetivoControlador.java
  servicio/AsignacionObjetivoServicio.java
  repositorio/AsignacionObjetivoRepositorio.java
  modelo/AsignacionObjetivo.java
  dto/GuardarAsignacionSolicitud.java
  dto/AsignacionRespuesta.java
  dto/ListaAsignacionesRespuesta.java
```

`porcentajeObjetivo` y la suma usan `BigDecimal`.

### 3.4 Contratos API

**Activos.** Se mantiene el contrato de `documentacion/04-contratos-api.md` §8:

| Método | Ruta                   | Éxito | Cuerpo de respuesta                                           |
| ------ | ---------------------- | ----- | ------------------------------------------------------------- |
| POST   | `/api/v1/activos`      | 201   | `ActivoRespuesta`                                             |
| GET    | `/api/v1/activos`      | 200   | lista de `ActivoRespuesta`, ordenada por `simbolo` ascendente |
| GET    | `/api/v1/activos/{id}` | 200   | `ActivoRespuesta`                                             |
| PUT    | `/api/v1/activos/{id}` | 200   | `ActivoRespuesta`                                             |

```json
{
  "id": 1,
  "simbolo": "IVV",
  "nombre": "iShares Core S&P 500 ETF",
  "tipoActivo": "ETF",
  "moneda": "USD",
  "simboloEditable": true
}
```

`simboloEditable` es un campo nuevo de `ActivoRespuesta`, calculado por el backend. Vale `false` cuando el activo tiene registros históricos relacionados. En esta spec siempre vale `true`.

**Esto cambia el contrato de 04 §8.4:** `PUT` pasa a aceptar `simbolo`, `nombre`, `tipoActivo` y `moneda`, todos obligatorios.

```json
{
  "simbolo": "IVV",
  "nombre": "iShares Core S&P 500 ETF",
  "tipoActivo": "ETF",
  "moneda": "USD"
}
```

Reglas del `PUT`:

- El símbolo se normaliza y valida igual que al crear.
- Si el nuevo símbolo pertenece a otro activo, responde 409 `SIMBOLO_DUPLICADO`.
- Enviar el mismo símbolo que ya tiene el activo no es un cambio y siempre se acepta.
- Si el símbolo cambia y el activo tiene registros históricos, responde 409 `SIMBOLO_BLOQUEADO`. Este caso no puede ocurrir en esta spec.

**Asignaciones:**

`PUT /api/v1/asignaciones/{activoId}` crea la asignación o la actualiza si ya existe. Responde 200 en ambos casos.

```json
// solicitud
{ "porcentajeObjetivo": 85.00 }
// respuesta
{ "activoId": 1, "simbolo": "IVV", "porcentajeObjetivo": 85.00 }
```

`GET /api/v1/asignaciones` responde 200. **Esto cambia el contrato de 04 §14.2**, que pasa de una lista a un objeto:

```json
{
  "asignaciones": [
    { "activoId": 1, "simbolo": "IVV", "porcentajeObjetivo": 85.0 },
    { "activoId": 2, "simbolo": "VXUS", "porcentajeObjetivo": 10.0 },
    { "activoId": 3, "simbolo": "BTC", "porcentajeObjetivo": 5.0 }
  ],
  "sumaPorcentajes": 100.0,
  "sumaCorrecta": true
}
```

- `asignaciones` contiene solo los activos con asignación registrada, ordenados por `simbolo`.
- `sumaPorcentajes` es la suma de `porcentajeObjetivo`, o 0 si no hay asignaciones.
- `sumaCorrecta` es `true` solo si `sumaPorcentajes` es exactamente 100. Se compara con `BigDecimal.compareTo`.

**Errores** (estructura de 04 §7):

| Caso                                                                                             | HTTP | `codigo`                                                        |
| ------------------------------------------------------------------------------------------------ | ---- | --------------------------------------------------------------- |
| Validación de campos, JSON mal formado o valor de enum inválido                                  | 400  | `DATOS_INVALIDOS` (con `erroresCampos` cuando aplica)           |
| Activo inexistente en `/activos/{id}` o `/asignaciones/{activoId}`                               | 404  | `ACTIVO_NO_ENCONTRADO`                                          |
| Símbolo ya existente, incluida la violación de `uk_activo_simbolo` en la base de datos           | 409  | `SIMBOLO_DUPLICADO`                                             |
| Cambio de símbolo en un activo con registros históricos (sin uso hasta que existan esos módulos) | 409  | `SIMBOLO_BLOQUEADO`                                             |
| Error inesperado                                                                                 | 500  | `ERROR_INTERNO`, con un mensaje genérico sin stack trace ni SQL |

Mensajes de campo, en español y comprensibles. Ejemplos:

- "Es obligatorio."
- "Solo se permiten letras, números, punto y guion (máximo 20)."
- "Debe estar entre 0 y 100."

### 3.5 Frontend (`frontend/app-inversiones/src/app/`)

```text
nucleo/layout/layout.component.ts        // shell: navegación lateral (escritorio) y compacta (móvil)
activos/activo.model.ts                  // Activo, TipoActivo, Moneda, Asignacion, ListaAsignaciones
activos/activo.service.ts                // HTTP /api/v1/activos
activos/asignacion.service.ts            // HTTP /api/v1/asignaciones
activos/activos-pagina.component.ts      // tabla/tarjetas, barra de asignación, estados
activos/activo-form.component.ts         // panel lateral crear/editar (Reactive Forms)
```

Otros archivos:

- `frontend/app-inversiones/proxy.conf.json` redirige `/api` a `http://localhost:8080`, y se referencia en `angular.json`.
- Los estilos globales en `src/styles.css` definen los tokens de color de la guía §4 y las tipografías Hanken Grotesk y Geist Mono.

## 4. Plan de implementación

1. **Base de errores y pruebas en el backend.**
   - Crear `compartido/error/` con `ErrorRespuesta`, `ErrorCampo`, las excepciones y `ManejadorGlobalErrores`.
   - Agregar al `pom.xml` las dependencias de test de Testcontainers para PostgreSQL y la integración `spring-boot-testcontainers`, con versiones gestionadas por el BOM de Spring Boot.
   - Configurar una clase base de pruebas de integración con PostgreSQL 17.
   - La aplicación arranca y `InversionesApplicationTests` pasa con el contenedor.
2. **Tabla activo.** Crear `V1__crear_tabla_activo.sql`, junto con la entidad `Activo`, los enums `TipoActivo` y `Moneda` y `ActivoRepositorio`. La aplicación arranca con `ddl-auto=validate` sin errores.
3. **Crear y consultar activos.**
   - Implementar los DTO, `ActivoServicio` (normalización del símbolo, unicidad y validaciones) y `ActivoControlador` con `POST`, `GET` lista y `GET` por id.
   - Pruebas de integración: creación correcta, símbolo normalizado, duplicado en 409, campos obligatorios en 400, tipo o moneda inválidos en 400, inexistente en 404 y lista ordenada por símbolo.
4. **Actualizar activo.**
   - Implementar `PUT /api/v1/activos/{id}` con `simbolo`, `nombre`, `tipoActivo` y `moneda`.
   - `ActivoServicio` expone un método `tieneRegistrosHistoricos(activoId)`, que en esta spec devuelve `false`. Las specs de Transacciones, Referidos, Dividendos y Precios lo ampliarán.
   - Ese método alimenta `simboloEditable` y el bloqueo del símbolo.
   - Pruebas: actualiza los cuatro campos; normaliza el nuevo símbolo; devuelve 409 `SIMBOLO_DUPLICADO` si el símbolo es de otro activo; acepta el mismo símbolo sin cambios; devuelve 404 si no existe y 400 si los datos son inválidos.
   - Prueba unitaria del servicio con `tieneRegistrosHistoricos` simulado en `true`: cambiar el símbolo devuelve `SIMBOLO_BLOQUEADO`, y cambiar solo el nombre se acepta.
5. **Tabla asignacion_objetivo.** Crear `V2__crear_tabla_asignacion_objetivo.sql`, junto con la entidad `AsignacionObjetivo` y su repositorio. La aplicación arranca con `validate`.
6. **Endpoints de asignación.**
   - Implementar el servicio y el controlador de `PUT /asignaciones/{activoId}` y `GET /asignaciones`, con `sumaPorcentajes` y `sumaCorrecta`.
   - Pruebas: crear asignación, actualizarla sin duplicar la fila, rechazar valores por debajo de 0 o por encima de 100, devolver 404 si el activo no existe, suma 100 con `sumaCorrecta` en `true`, suma 95 con `sumaCorrecta` en `false`, y lista vacía con suma 0.
7. **Estilos globales y proxy.**
   - Reemplazar el placeholder de `app.html`.
   - Definir los tokens y las tipografías en `styles.css`.
   - Agregar `proxy.conf.json` y `provideHttpClient()`.
   - `npm start` muestra una página vacía con el tema oscuro.
8. **Shell.**
   - Crear `LayoutComponent` con navegación lateral en escritorio y compacta en móvil.
   - La navegación muestra solo las pantallas implementadas: por ahora, Activos.
   - Configurar las rutas `/activos` y `''` → redirección a `/activos` hasta que exista Resumen.
9. **Servicios y modelo del frontend.** Crear `activo.model.ts`, `activo.service.ts` y `asignacion.service.ts`. Pruebas con Vitest usando `HttpTestingController`.
10. **Pantalla Activos (consulta).**
    - Tabla en escritorio con las columnas Activo (símbolo y nombre), Tipo, Moneda, Objetivo y Acciones (Editar).
    - Tarjetas en móvil.
    - Barra de asignación con la suma y la advertencia cuando `sumaCorrecta` es `false`. La advertencia lleva símbolo y texto, no solo color.
    - Estado de carga, estado vacío ("No existen activos registrados." con la acción "Registrar primer activo") y mensaje de error comprensible.
11. **Panel registrar/editar.**
    - Formulario reactivo con símbolo, nombre, tipo (grupo de opciones Acción / ETF / Criptomoneda), moneda (selector con USD) y porcentaje objetivo opcional.
    - Al editar, el símbolo es editable si `simboloEditable` es `true`. Si es `false`, se muestra como "No editable".
    - Al guardar se llama a `POST` o `PUT /activos`. Si hay porcentaje, después se llama a `PUT /asignaciones/{id}`.
    - Los errores de campo de la API se muestran bajo cada campo. Por ejemplo: "Ya existe un activo con el símbolo IVV."
    - Si falla la asignación después de guardar el activo, el panel se cierra y avisa que el activo se guardó pero el objetivo no.
    - Mensajes de éxito: "Activo registrado correctamente." y "Cambios guardados correctamente."
12. **Comparación con el Excel.** Registrar IVV, VXUS y BTC con los datos de la hoja Activos de `referencia/registro_inversiones_v4_1.xlsx` y asignar 85 / 10 / 5. Comprobar que la pantalla coincide con el Excel.

## 5. Criterios de aceptación

**Backend:**

- [ ] `mvn verify` en `backend/` compila y todas las pruebas pasan (requiere Docker en ejecución).
- [ ] Flyway aplica V1 y V2 sobre una base vacía, y Hibernate valida el esquema con `ddl-auto=validate`.
- [ ] `POST /api/v1/activos` con `{"simbolo":" ivv ","nombre":"iShares Core S&P 500 ETF","tipoActivo":"ETF","moneda":"USD"}` responde 201 con `"simbolo":"IVV"`.
- [ ] Repetir ese POST con `"simbolo":"IVV"` responde 409 con `codigo` = `SIMBOLO_DUPLICADO`.
- [ ] Un POST sin `nombre` responde 400 con `codigo` = `DATOS_INVALIDOS` y un `erroresCampos` que incluye `nombre`.
- [ ] Un POST con `"tipoActivo":"CRIPTO"` responde 400 con `DATOS_INVALIDOS`.
- [ ] Un POST con `"moneda":"EUR"` responde 400 con `DATOS_INVALIDOS`.
- [ ] Un POST con `"simbolo":"IV V"` o con un símbolo de más de 20 caracteres responde 400.
- [ ] `GET /api/v1/activos` devuelve los activos ordenados por `simbolo` ascendente.
- [ ] `GET /api/v1/activos/999` responde 404 con `ACTIVO_NO_ENCONTRADO`.
- [ ] `PUT /api/v1/activos/{id}` actualiza `simbolo`, `nombre`, `tipoActivo` y `moneda`. Un símbolo enviado como `" vxus "` se guarda como `"VXUS"`.
- [ ] `PUT /api/v1/activos/{id}` con el símbolo de otro activo responde 409 `SIMBOLO_DUPLICADO`.
- [ ] `PUT /api/v1/activos/{id}` con el mismo símbolo que ya tiene el activo responde 200.
- [ ] Todas las respuestas de activo incluyen `simboloEditable` = `true`.
- [ ] La prueba unitaria con registros históricos simulados devuelve `SIMBOLO_BLOQUEADO` al cambiar el símbolo y acepta los cambios de los demás campos.
- [ ] `PUT /api/v1/asignaciones/{id}` con `85` crea la asignación. Repetirlo con `80` la actualiza y la tabla sigue teniendo una sola fila para ese activo.
- [ ] `PUT /api/v1/asignaciones/{id}` con `-1` o `100.01` responde 400 con `DATOS_INVALIDOS`.
- [ ] `PUT /api/v1/asignaciones/999` responde 404 con `ACTIVO_NO_ENCONTRADO`.
- [ ] Con asignaciones 85 / 10 / 5, `GET /api/v1/asignaciones` devuelve `sumaPorcentajes` = 100 y `sumaCorrecta` = `true`.
- [ ] Con asignaciones 85 / 10, devuelve `sumaPorcentajes` = 95 y `sumaCorrecta` = `false`.
- [ ] Sin asignaciones, devuelve `asignaciones` = `[]`, `sumaPorcentajes` = 0 y `sumaCorrecta` = `false`.
- [ ] Ninguna respuesta de error contiene stack traces, SQL ni nombres de clases Java.
- [ ] Las respuestas no exponen `fechaCreacion` ni `fechaActualizacion`.

**Frontend:**

- [ ] `npm test` en `frontend/app-inversiones/` pasa.
- [ ] `npm start` con el backend levantado muestra el shell con tema oscuro y la entrada "Activos" en la navegación lateral.
- [ ] A 390 px de ancho, la navegación se muestra compacta y los activos aparecen como tarjetas, sin scroll horizontal de página.
- [ ] Sin activos registrados, se muestra "No existen activos registrados." con la acción "Registrar primer activo".
- [ ] Registrar un activo desde el panel lo agrega a la tabla y muestra "Activo registrado correctamente.".
- [ ] Registrar un símbolo existente muestra bajo el campo Símbolo un mensaje de que ya existe.
- [ ] Al editar un activo, el campo Símbolo es editable y guardar un símbolo nuevo actualiza la tabla.
- [ ] Un activo sin asignación muestra `—` en la columna Objetivo.
- [ ] Con una suma distinta de 100 %, la barra de asignación muestra la advertencia con símbolo y texto.
- [ ] La pantalla no muestra la columna Estado ni las acciones Activar/Desactivar.

**Excel:**

- [ ] IVV, VXUS y BTC registrados con 85 / 10 / 5 coinciden con la hoja Activos del Excel en símbolo, nombre, tipo (`CRIPTO` → `CRIPTOMONEDA`), moneda y objetivo, y la suma aparece como correcta.

## 6. Decisiones

- **Sí:** asignación objetivo dentro de esta spec y como recurso propio (`/asignaciones`). Así lo exigen la FASE 2 y el prototipo, y no se modifica el contrato de Activo.
- **No:** `porcentajeObjetivo` como campo de Activo. Mezclaría dos recursos y cambiaría 04 §8.
- **Sí:** el backend calcula `sumaPorcentajes` y `sumaCorrecta`, porque el backend es la fuente de los cálculos. Esto cambia la forma de 04 §14.2 de lista a objeto.
- **No:** que el frontend sume la lista.
- **Sí:** el formulario del activo incluye el % opcional y guarda con dos llamadas. Es fiel al prototipo. Un fallo parcial se informa al usuario, sin rollback.
- **No:** editar la asignación por separado ni un endpoint combinado.
- **Sí:** un activo sin asignación se muestra como `—` y cuenta como 0 %. No se crean filas implícitas.
- **No:** crear 0 % automáticamente al crear un activo.
- **Sí:** después de crear un activo se pueden editar nombre, tipo y moneda (contrato 04 §8.4). Las restricciones de tipo y moneda para activos con movimientos se definirán en la spec de Transacciones.
- **Sí:** el símbolo se puede modificar mientras el activo no tenga transacciones, referidos, dividendos ni precios. Cuando existan, queda bloqueado. Decisión del usuario: permite corregir errores de escritura sin romper la trazabilidad histórica. Cambia 04 §8.4 y se aparta del prototipo, que lo mostraba siempre como "No editable".
- **No:** símbolo inmutable siempre. Esa regla se había inferido del contrato y del prototipo, no de una regla de negocio.
- **Sí:** la asignación objetivo no bloquea el símbolo, porque no es un registro histórico (no figura en la lista de la regla).
- **Sí:** `simboloEditable` en `ActivoRespuesta`, calculado por el backend, para que la interfaz no deduzca reglas de negocio.
- **Sí:** el bloqueo se centraliza en `ActivoServicio.tieneRegistrosHistoricos`. Cada spec de un módulo histórico agrega su verificación ahí, en lugar de repartir la regla por varios sitios.
- **Sí:** el nuevo código `SIMBOLO_BLOQUEADO` (409) se define ahora para que el contrato quede estable, aunque todavía no puede producirse.
- **Sí:** el símbolo se normaliza (sin espacios en los extremos, en mayúsculas, `^[A-Z0-9.-]{1,20}$`), para evitar duplicados por diferencias de mayúsculas.
- **No:** guardar el símbolo tal cual.
- **Sí:** solo USD, tanto en el enum como en el CHECK. El MVP analiza en USD. Ampliar la lista requerirá una migración nueva.
- **Sí:** CHECK de base de datos para el tipo, la moneda y el rango del porcentaje. Es validación en tres niveles (03 §31).
- **Sí:** todas las validaciones de campo usan `DATOS_INVALIDOS` con `erroresCampos`. `ASIGNACION_INVALIDA` queda sin uso en esta spec.
- **Sí:** se agrega el código `ERROR_INTERNO` para los errores 500, que 04 §20 no definía.
- **Sí:** Testcontainers para las pruebas de integración. Prueba las migraciones y las restricciones reales sin depender de la base local. Es una dependencia nueva, solo de test.
- **No:** usar el Postgres de docker compose en las pruebas, ni hacer solo pruebas unitarias.
- **Sí:** proxy de Angular (`proxy.conf.json`). Evita configurar CORS en el backend.
- **No:** CORS en Spring por ahora.
- **Sí:** componentes propios con CSS propio (05 §51).
- **Sí:** el shell se construye en esta spec (07, FASE 1 y FASE 2).
- **Sí:** la navegación muestra solo las pantallas implementadas, y cada spec agrega su entrada. Así se evitan enlaces a pantallas vacías.
- **Sí:** `''` redirige a `/activos` hasta que exista Resumen.
- **Sí:** el listado se ordena por `simbolo` ascendente, porque es predecible y no depende del orden de inserción.
- **Sí:** las fechas técnicas no se exponen en la API, que mantiene el contrato de 04 §8.
- **Sí:** paquete backend `asignaciones` separado de `activos`, porque es otro recurso y otra tabla.
- **Sí:** en el frontend, los servicios de asignación viven en `activos/` porque solo los usa esa pantalla.
- **Sí:** la spec se nombra `001-activos.md`, según `03-arquitectura.md` §5.

## 7. Riesgos

| Riesgo                                                                                                                     | Mitigación                                                                                                                                                        |
| -------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Docker no está en ejecución y fallan las pruebas de integración                                                            | El mensaje de error de Testcontainers lo indica. Docker ya es requisito del proyecto (docker compose).                                                            |
| Dos solicitudes crean el mismo símbolo a la vez                                                                            | La restricción `uk_activo_simbolo` lo impide, y el manejador traduce la violación a 409 `SIMBOLO_DUPLICADO`.                                                      |
| Se guarda el activo pero falla la asignación (dos llamadas)                                                                | El panel informa que el objetivo no se guardó. El usuario lo reintenta editando el activo.                                                                        |
| El cambio de forma de `GET /asignaciones` rompe un consumidor                                                              | Todavía no hay consumidores. El cambio ya está documentado en 04 §14.2.                                                                                           |
| Una spec futura (por ejemplo, Precios) olvida ampliar `tieneRegistrosHistoricos` y el símbolo sigue editable con historial | La regla queda documentada en 01 §2.4. Las specs de Transacciones, Referidos, Dividendos y Precios deben incluir la ampliación entre sus criterios de aceptación. |
| Se copia el prototipo obsoleto (Estado, VT inactivo)                                                                       | Los criterios de aceptación exigen que no aparezcan. Ver "Diferencias conocidas" en 05.                                                                           |

## Lo que **no** entra en esta spec

- Eliminar activos o asignaciones.
- Estado activo/inactivo.
- Indicadores dentro / cerca / fuera del objetivo y su umbral.
- Porcentaje actual y diferencia contra objetivo.
- Restricciones de edición por transacciones existentes.
- Ayuda contextual y tutoriales.
- Monedas distintas de USD.
- Columna `Nota` del Excel.
- Limpieza de credenciales en `application.properties`.

Cada uno de estos puntos, si se aborda, irá en su propia spec o tarea.
