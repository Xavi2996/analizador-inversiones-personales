# SPEC 002 — Aportes

> **Estado:** Implementado
> **Depende de:** SPEC 001
> **Fecha:** 2026-11-04
> **Objetivo:** Registrar y consultar los aportes de dinero al intermediario, con el costo de fondeo y los totales calculados por el backend, desde una pantalla Aportes.

## 1. Por qué existe esta spec

Es la FASE 3 de `documentacion/07-hoja-ruta.md`.

Los aportes son la base del total aportado desde bolsillo y del efectivo disponible (`01-modelo-funcional.md` §3 y §11).

Esta spec solo los registra y consulta. El efectivo y el resumen llegan en FASE 9 y FASE 10.

Reutiliza de la SPEC 001 la estructura de errores, la base de pruebas con Testcontainers, el shell y los estilos globales.

Esta spec cambia decisiones documentadas (ver §6). La documentación general se actualiza en el paso 1 del plan de implementación.

## 2. Alcance

**Incluye:**

- Tabla `aporte` mediante la migración `V3__crear_tabla_aporte.sql`.
- Registrar un aporte (`POST /api/v1/aportes`) con cálculo de `costoFondeo` en backend.
- Listar aportes con totales (`GET /api/v1/aportes`) y consultar uno (`GET /api/v1/aportes/{id}`).
- Porcentaje de costo de fondeo por aporte y total, calculado en backend.
- Pantalla Aportes: totales, historial (tabla en escritorio, lista en móvil) y formulario lateral de registro.
- Entrada "Aportes" en la navegación.
- Comparación con la hoja Aportes del Excel.

**Fuera del alcance (para futuras specs):**

- Editar o eliminar aportes. Depende de la decisión pendiente sobre corrección de movimientos históricos.
- Filtros `?desde=` y `?hasta=` (04 §9.2 los marca como futuros).
- Efectivo disponible (FASE 9) y resumen del portafolio (FASE 10).
- Catálogo cerrado de métodos de fondeo.
- Ayuda contextual "[?]" del monto neto y tutorial "Registrar aporte" (FASE 12).
- Paginación del historial.

## 3. Modelo de datos

### 3.1 Migración

`backend/src/main/resources/db/migration/V3__crear_tabla_aporte.sql`

```sql
CREATE TABLE aporte (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha_aporte        DATE          NOT NULL,
    monto_aportado      NUMERIC(19,6) NOT NULL,
    monto_neto_broker   NUMERIC(19,6) NOT NULL,
    costo_fondeo        NUMERIC(19,6) NOT NULL,
    metodo_fondeo       VARCHAR(50),
    comentario          TEXT,
    fecha_creacion      TIMESTAMPTZ   NOT NULL,
    fecha_actualizacion TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_aporte_monto_aportado CHECK (monto_aportado > 0),
    CONSTRAINT ck_aporte_monto_neto     CHECK (monto_neto_broker >= 0 AND monto_neto_broker <= monto_aportado),
    CONSTRAINT ck_aporte_costo_fondeo   CHECK (costo_fondeo = monto_aportado - monto_neto_broker)
);
```

`ck_aporte_costo_fondeo`, junto con `ck_aporte_monto_neto`, implica `costo_fondeo >= 0`.

### 3.2 Reglas de los datos

**Fecha del aporte (`fechaAporte`):**

- Obligatoria, formato `AAAA-MM-DD`.
- No puede ser posterior a la fecha actual de negocio en `America/Guayaquil`.
- La zona horaria de negocio se configura con la propiedad `app.zona-horaria-negocio=America/Guayaquil` y no depende de la zona del servidor.

**Monto aportado (`montoAportado`):**

- Obligatorio y mayor que cero.
- Máximo 2 decimales y 13 dígitos enteros.

**Monto neto recibido (`montoNetoBroker`):**

- Obligatorio y mayor o igual que cero.
- Máximo 2 decimales y 13 dígitos enteros.
- No puede superar `montoAportado`.
- Igual al monto aportado es válido (costo 0).

**Costo de fondeo (`costoFondeo`):**

- Lo calcula el backend: `montoAportado - montoNetoBroker`.
- No se acepta en la solicitud. Si llega, se ignora.
- Se guarda en la columna para conservar el hecho histórico.

**Método de fondeo (`metodoFondeo`):**

- Opcional, texto libre.
- Se eliminan los espacios de los extremos. Vacío se guarda como `null`.
- Máximo 50 caracteres. Ejemplo válido: `Binance USDT → ERC20 → Hapi`.

**Comentario (`comentario`):**

- Opcional.
- Se eliminan los espacios de los extremos. Vacío se guarda como `null`.
- Máximo 500 caracteres, validado en el DTO. La columna sigue siendo `TEXT`.

**Porcentajes:**

- Por aporte: `porcentajeCostoFondeo = costoFondeo / montoAportado × 100`.
- Total: `porcentajeCostoFondeo = totalCostoFondeo / totalAportado × 100`, o 0 si no hay aportes.
- Total: `porcentajeNetoBroker = totalNetoBroker / totalAportado × 100`, o 0 si no hay aportes.
- Todos con 2 decimales y `RoundingMode.HALF_UP`.
- Se calculan sobre los valores sin redondear.

**Montos en la respuesta:**

- Se devuelven con escala 2 (`200.00`).
- No hay redondeo, porque la entrada admite como máximo 2 decimales.

**Fechas técnicas:**

- `fecha_creacion` y `fecha_actualizacion` las asigna el backend.
- No se exponen en la API.

### 3.3 Backend (`com.personal.inversiones`)

```text
compartido/
  tiempo/ConfiguracionReloj.java           // bean Clock con la zona de app.zona-horaria-negocio
aportes/
  controlador/AporteControlador.java
  servicio/AporteServicio.java
  repositorio/AporteRepositorio.java      // findAllByOrderByFechaAporteDescIdDesc()
  modelo/Aporte.java
  dto/CrearAporteSolicitud.java
  dto/AporteRespuesta.java
  dto/ListaAportesRespuesta.java
```

Todos los montos y porcentajes usan `BigDecimal`. `fechaAporte` usa `LocalDate`.

El 404 reutiliza `RecursoNoEncontradoExcepcion` de `compartido/error/`.

`AporteServicio` obtiene la fecha de hoy con `LocalDate.now(clock)` usando el `Clock` inyectado. No se llama a `LocalDate.now()` sin reloj.

### 3.4 Contratos API

| Método | Ruta                   | Éxito | Cuerpo de respuesta     |
| ------ | ---------------------- | ----- | ----------------------- |
| POST   | `/api/v1/aportes`      | 201   | `AporteRespuesta`       |
| GET    | `/api/v1/aportes`      | 200   | `ListaAportesRespuesta` |
| GET    | `/api/v1/aportes/{id}` | 200   | `AporteRespuesta`       |

No existen `PUT` ni `DELETE`.

**Solicitud `POST`:**

```json
{
  "fechaAporte": "2026-10-21",
  "montoAportado": 200.0,
  "montoNetoBroker": 195.58,
  "metodoFondeo": "Binance USDT → ERC20 → Hapi",
  "comentario": "Tercer aporte"
}
```

**`AporteRespuesta`:**

```json
{
  "id": 3,
  "fechaAporte": "2026-10-21",
  "montoAportado": 200.0,
  "montoNetoBroker": 195.58,
  "costoFondeo": 4.42,
  "porcentajeCostoFondeo": 2.21,
  "metodoFondeo": "Binance USDT → ERC20 → Hapi",
  "comentario": "Tercer aporte"
}
```

**`ListaAportesRespuesta`.** Esto cambia 04 §9.2, que pasa de una lista a un objeto:

```json
{
  "aportes": [],
  "totalAportado": 575.0,
  "totalNetoBroker": 559.13,
  "totalCostoFondeo": 15.87,
  "porcentajeCostoFondeo": 2.76,
  "porcentajeNetoBroker": 97.24
}
```

- `aportes` contiene `AporteRespuesta`, ordenados por `fechaAporte` descendente y, a igual fecha, por `id` descendente.
- Sin aportes: `aportes` = `[]` y todos los totales y porcentajes valen 0.

**Errores** (estructura de 04 §7):

| Caso                                                                                                                                      | HTTP | `codigo`                                   |
| ----------------------------------------------------------------------------------------------------------------------------------------- | ---- | ------------------------------------------ |
| Campo obligatorio ausente, monto aportado ≤ 0, neto negativo, más de 2 decimales, fecha futura, textos demasiado largos, JSON mal formado | 400  | `DATOS_INVALIDOS` con `erroresCampos`      |
| `montoNetoBroker` mayor que `montoAportado`                                                                                               | 400  | `DATOS_INVALIDOS`, campo `montoNetoBroker` |
| `/aportes/{id}` inexistente                                                                                                               | 404  | `APORTE_NO_ENCONTRADO` (código nuevo)      |
| Método no permitido (`PUT`, `DELETE`)                                                                                                     | 405  | `SOLICITUD_INVALIDA`                       |
| Error inesperado                                                                                                                          | 500  | `ERROR_INTERNO`                            |

Mensajes de campo, en español y comprensibles. Ejemplos:

- "Es obligatorio."
- "Debe ser mayor que cero."
- "No puede ser negativo."
- "Admite como máximo 2 decimales."
- "No puede ser mayor al monto aportado."
- "No puede ser una fecha futura."
- "Admite como máximo 50 caracteres."

### 3.5 Frontend (`frontend/app-inversiones/src/app/`)

```text
aportes/aporte.model.ts                // Aporte, CrearAporte, ListaAportes
aportes/aporte.service.ts              // HTTP /api/v1/aportes
aportes/aportes-pagina.component.ts    // totales, historial, estados
aportes/aporte-form.component.ts       // formulario lateral (Reactive Forms)
```

Otros cambios:

- `app.routes.ts` agrega la ruta `aportes`.
- `nucleo/layout/layout.component.ts` agrega la entrada "Aportes" al grupo Registro, después de Activos.

## 4. Plan de implementación

1. **Documentación.** Reflejar las decisiones de esta spec en la documentación general:
   - `01-modelo-funcional.md` §3.3: el monto neto no puede superar el monto aportado, sin excepción.
   - `01-modelo-funcional.md` §17: regla general de que "hoy" es la fecha de negocio en `America/Guayaquil`.
   - `03-arquitectura.md` §30: zona horaria de negocio configurable (`app.zona-horaria-negocio`) y uso de un `Clock` inyectado.
   - `02-base-datos.md` §10: identidad del `id`, restricciones con nombre y `metodo_fondeo` como texto libre.
   - `04-contratos-api.md` §9: forma de `GET /aportes`, `porcentajeCostoFondeo`, reglas y errores.
   - `04-contratos-api.md` §20: código `APORTE_NO_ENCONTRADO`.
   - `05-guia-interfaz.md`, "Diferencias conocidas con el prototipo": el método es texto libre, no un selector.
2. **Tabla aporte y reloj de negocio.**
   - Crear `V3__crear_tabla_aporte.sql`, la entidad `Aporte` y `AporteRepositorio`.
   - Agregar `app.zona-horaria-negocio=America/Guayaquil` a `application.properties` y crear `ConfiguracionReloj` con el bean `Clock`.
   - La aplicación arranca con `ddl-auto=validate` sin errores.
3. **Registrar aporte.**
   - Crear `CrearAporteSolicitud` con Bean Validation.
   - Implementar `AporteServicio.registrar`: normalización de textos, fecha no futura, neto ≤ aportado y cálculo del costo.
   - Crear `AporteControlador` con `POST`.
   - Pruebas de integración: alta correcta con costo calculado, neto igual al aportado (costo 0), neto mayor al aportado, monto aportado ≤ 0, neto negativo, 3 decimales, fecha futura, campos obligatorios, método de 51 caracteres, comentario de 501 caracteres y `costoFondeo` enviado que se ignora.
4. **Consultar aportes.**
   - Implementar `GET` por id y `GET` lista con `ListaAportesRespuesta`.
   - Prueba unitaria del cálculo de porcentajes y del redondeo `HALF_UP`.
   - Prueba unitaria de la fecha con un `Clock` fijo en `2026-10-22T03:00:00Z` (21/10 a las 22:00 en Guayaquil): acepta 2026-10-21 y rechaza 2026-10-22.
   - Pruebas de integración: orden por fecha e id descendentes, totales, lista vacía con ceros y 404 `APORTE_NO_ENCONTRADO`.
5. **Servicio y modelo del frontend.** Crear `aporte.model.ts` y `aporte.service.ts`. Pruebas con Vitest usando `HttpTestingController`.
6. **Pantalla Aportes (consulta).**
   - Ruta `/aportes` y entrada en la navegación.
   - Totales: aportado desde bolsillo, neto recibido, costos de fondeo y barra neto/costo con los porcentajes del backend. La barra lleva texto, no solo color.
   - Historial en escritorio: tabla con Fecha, Aportado, Neto recibido, Costo de fondeo (con %) y Método y comentario, más una fila de totales.
   - Historial en móvil: lista compacta.
   - Estado de carga, estado vacío ("No existen aportes registrados." con la acción "Registrar primer aporte") y mensaje de error comprensible.
7. **Formulario de registro.**
   - Campos: fecha (por defecto, hoy), monto aportado (USD), monto neto recibido (USD), método (texto opcional) y comentario (opcional).
   - Bloque "Costo de fondeo · Calculado" con la vista previa `aportado − neto`. Solo informa: el valor oficial es el del backend.
   - Los errores de campo de la API se muestran bajo cada campo.
   - Al guardar se muestra "Aporte registrado correctamente.", se limpia el formulario y se recargan historial y totales.
8. **Comparación con el Excel.** Crear la prueba de integración `ComparacionExcelAportesTest`, que registra los tres aportes de la hoja Aportes y compara costos y totales. Revisar manualmente la pantalla con los mismos datos.

## 5. Criterios de aceptación

**Backend:**

- [ ] `mvn verify` en `backend/` compila y todas las pruebas pasan (requiere Docker en ejecución).
- [ ] Flyway aplica V1, V2 y V3 sobre una base vacía, y Hibernate valida el esquema con `ddl-auto=validate`.
- [ ] `POST /api/v1/aportes` con `{"fechaAporte":"2026-10-21","montoAportado":200.00,"montoNetoBroker":195.58}` responde 201 con `costoFondeo` = 4.42 y `porcentajeCostoFondeo` = 2.21.
- [ ] Un POST con `montoNetoBroker` igual a `montoAportado` responde 201 con `costoFondeo` = 0.
- [ ] Un POST con `montoAportado` = 200.00 y `montoNetoBroker` = 200.01 responde 400 `DATOS_INVALIDOS` con `erroresCampos` en `montoNetoBroker`.
- [ ] Un POST con `montoAportado` = 0 o negativo responde 400 con error en `montoAportado`.
- [ ] Un POST con `montoNetoBroker` = -0.01 responde 400. Con `montoNetoBroker` = 0 responde 201.
- [ ] Un POST con un monto de 3 decimales responde 400.
- [ ] Un POST con `fechaAporte` de mañana responde 400. Con la fecha de hoy responde 201. "Hoy" es la fecha en `America/Guayaquil`.
- [ ] Con un `Clock` fijo en `2026-10-22T03:00:00Z`, la fecha 2026-10-21 se acepta y la 2026-10-22 se rechaza.
- [ ] Un POST sin `fechaAporte`, `montoAportado` o `montoNetoBroker` responde 400 con ese campo en `erroresCampos`.
- [ ] Un POST con `metodoFondeo` de 51 caracteres o `comentario` de 501 caracteres responde 400.
- [ ] `metodoFondeo` = `"   "` se guarda y se responde como `null`. `" Binance "` se guarda como `"Binance"`.
- [ ] Un `costoFondeo` enviado en la solicitud no altera el costo calculado.
- [ ] `GET /api/v1/aportes` devuelve los aportes por `fechaAporte` descendente y, a igual fecha, por `id` descendente.
- [ ] Sin aportes, `GET /api/v1/aportes` devuelve `aportes` = `[]` y todos los totales y porcentajes en 0.
- [ ] `GET /api/v1/aportes/999` responde 404 con `APORTE_NO_ENCONTRADO`.
- [ ] `PUT` y `DELETE` sobre `/api/v1/aportes/{id}` responden 405 con `SOLICITUD_INVALIDA`.
- [ ] Las respuestas no exponen `fechaCreacion` ni `fechaActualizacion`.
- [ ] Ninguna respuesta de error contiene stack traces, SQL ni nombres de clases Java.

**Frontend:**

- [ ] `npm test` en `frontend/app-inversiones/` pasa.
- [ ] La navegación muestra "Aportes" y `/aportes` abre la pantalla.
- [ ] Sin aportes, se muestra "No existen aportes registrados." con la acción "Registrar primer aporte".
- [ ] Al escribir 200.00 y 195.58, el bloque calculado muestra $4.42 y no es un campo editable.
- [ ] Registrar un aporte válido muestra "Aporte registrado correctamente." y actualiza historial y totales sin recargar la página.
- [ ] Un neto mayor que el aportado muestra bajo "Monto neto recibido" el mensaje de la API.
- [ ] Los totales y porcentajes mostrados son los del backend. El frontend no suma la lista.
- [ ] A 390 px de ancho, el historial se muestra como lista compacta, sin scroll horizontal de página.
- [ ] Los montos se muestran como `$1,234.56` y las fechas como `DD/MM/AAAA`.

**Excel** (hoja Aportes de `referencia/registro_inversiones_v4_1.xlsx`):

- [ ] Registrar 2026-10-13 (125.00 / 120.22), 2026-10-19 (250.00 / 243.33) y 2026-10-21 (200.00 / 195.58) da costos de 4.78, 6.67 y 4.42.
- [ ] Los totales coinciden con `Resumen!B4:B6`: aportado 575.00, neto 559.13 y costos 15.87.
- [ ] El total tiene `porcentajeCostoFondeo` = 2.76 y `porcentajeNetoBroker` = 97.24.

## 6. Decisiones

- **Sí:** solo crear y consultar. La corrección de movimientos históricos sigue pendiente y se decidirá en su propia spec. Decisión del usuario.
- **No:** `PUT` ni `DELETE` de aportes por ahora.
- **Sí:** el neto no puede superar el aportado (400 en `montoNetoBroker`). Permitirlo daría un costo negativo, que 01 §3.3 y 02 §10 prohíben. Se elimina la excepción "salvo que exista una razón explícita" de 01 §3.3. Decisión del usuario.
- **No:** aceptar un neto mayor con costo 0 o con costo negativo.
- **Sí:** `costoFondeo` lo calcula el backend y se guarda, con un CHECK que garantiza `costo = aportado − neto`.
- **No:** aceptar `costoFondeo` en la solicitud.
- **Sí:** método de fondeo como texto libre opcional de hasta 50 caracteres. Conserva tal cual la ruta del Excel. Decisión del usuario. Se aparta del selector del prototipo y del ejemplo `"BINANCE"` de 04 §9.1.
- **No:** catálogo cerrado de métodos.
- **Sí:** `GET /aportes` devuelve un objeto con la lista y los totales calculados por el backend, igual que `/asignaciones` en la SPEC 001. Cambia 04 §9.2. Decisión del usuario.
- **No:** que el frontend sume la lista, ni un endpoint de totales separado.
- **Sí:** porcentajes `costo / aportado × 100` con 2 decimales `HALF_UP`, por aporte y total, más el porcentaje neto total. El prototipo los muestra y el frontend no debe calcularlos. Decisión del usuario.
- **Sí:** la fecha no puede ser futura, porque un aporte es un hecho ocurrido. Decisión del usuario.
- **Sí:** "hoy" es la fecha de negocio en `America/Guayaquil`, configurable con `app.zona-horaria-negocio`, y se obtiene de un `Clock` inyectado. La validación no depende de la zona del servidor y se puede probar de forma determinista. Las specs de Transacciones, Referidos, Dividendos, Precios y Fotografías reutilizarán el mismo reloj. Decisión del usuario.
- **No:** usar la zona del servidor ni escribir la zona en el código.
- **Sí:** el formulario propone la fecha de hoy del navegador. El usuario opera desde Guayaquil y el backend valida de todos modos.
- **Sí:** montos con máximo 2 decimales, porque son USD. La columna mantiene `NUMERIC(19,6)` de 02 §6. Decisión del usuario.
- **Sí:** comentario opcional de hasta 500 caracteres, validado en el DTO. Decisión del usuario.
- **Sí:** los textos opcionales vacíos se guardan como `null`.
- **Sí:** nuevo código `APORTE_NO_ENCONTRADO` (404), análogo a `ACTIVO_NO_ENCONTRADO`.
- **Sí:** las validaciones usan `DATOS_INVALIDOS` con `erroresCampos`, como en la SPEC 001. `APORTE_INVALIDO` queda sin uso.
- **Sí:** orden por `fechaAporte` descendente y luego por `id` descendente. Respeta el "más reciente primero" del prototipo y es estable a igual fecha.
- **Sí:** la vista previa del costo en el formulario la calcula el frontend solo como ayuda visual (05 §24). El valor oficial es el que devuelve el backend.
- **Sí:** migración `V3__crear_tabla_aporte.sql`, porque V1 y V2 ya existen. Los ejemplos de CLAUDE.md y 06 §11.1 usan otra numeración solo como ilustración.
- **Sí:** los valores de referencia salen del Excel, no del prototipo, cuyos datos son simulados (05 §57).
- **Sí:** la spec se nombra `002-aportes.md`, según 03 §5.

## 7. Riesgos

| Riesgo                                                               | Mitigación                                                                                    |
| -------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| Un aporte mal registrado no se puede corregir                        | Queda explícito como fuera de alcance. Se resuelve en la spec de corrección de movimientos.   |
| Los porcentajes redondeados de neto y costo no suman exactamente 100 | Ambos se calculan desde los valores sin redondear. La interfaz los muestra tal como llegan.   |
| El cambio de forma de `GET /aportes` rompe un consumidor             | Todavía no hay consumidores. Se documenta en 04 §9.2 en el paso 1.                            |
| Se copia el selector de método del prototipo                         | La decisión queda explícita en esta spec y en "Diferencias conocidas con el prototipo" de 05. |

## Lo que **no** entra en esta spec

- Editar o eliminar aportes.
- Filtros por fecha y paginación.
- Efectivo disponible y resumen del portafolio.
- Catálogo de métodos de fondeo.
- Ayuda contextual y tutoriales.

Cada uno de estos puntos, si se aborda, irá en su propia spec o tarea.
