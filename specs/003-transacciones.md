# SPEC 003 — Transacciones

> **Estado:** Aprobado
> **Depende de:** SPEC 001, SPEC 002
> **Fecha:** 2026-11-10
> **Objetivo:** Registrar y consultar compras y ventas de activos, con el monto bruto y el efectivo neto calculados por el backend y validación cronológica de la cantidad vendida, desde una pantalla Transacciones.

## 1. Por qué existe esta spec

Es la FASE 4 de `documentacion/07-hoja-ruta.md`.

Las transacciones son la fuente de verdad de las cantidades y del costo de las posiciones (01 §4, §9) y de una parte del efectivo (01 §11).

07 §FASE 4 exige definir aquí: reglas de compra y venta, validación de cantidad disponible, comisiones, efecto sobre efectivo, método de costo y estrategia de corrección. Esta spec resuelve dos decisiones marcadas como pendientes en CLAUDE.md: validaciones de venta y método de costo.

Esta spec solo registra y consulta. Posiciones (FASE 8) y Efectivo (FASE 9) consumirán estos datos.

Reutiliza de las SPEC 001 y 002: estructura de errores, `ActivoServicio.buscar`, el `Clock` de negocio, la base de pruebas con Testcontainers, el shell y los estilos globales.

## 2. Alcance

**Incluye:**

- Tabla `transaccion` mediante `V4__crear_tabla_transaccion.sql`.
- Registrar compras y ventas (`POST /api/v1/transacciones`) con `montoBruto` y `efectivoNeto` calculados en backend.
- Validación cronológica de ventas: ninguna venta puede dejar negativa la cantidad del activo en ninguna fecha.
- Consultar la cantidad disponible para vender (`GET /api/v1/transacciones/disponible`).
- Listar transacciones con filtros `activoId` y `tipo`, más totales (`GET /api/v1/transacciones`), y consultar una (`GET /api/v1/transacciones/{id}`).
- Bloqueo del símbolo de un activo con transacciones (ampliar `ActivoServicio.tieneRegistrosHistoricos`).
- Documentar el método de costo para ventas: costo promedio ponderado móvil.
- Pantalla Transacciones: historial con filtros, formulario lateral de compra/venta, disponible para vender.
- Entrada "Transacciones" en la navegación.
- Comparación con la hoja Transacciones del Excel.

**Fuera del alcance (para futuras specs):**

- Editar o eliminar transacciones. Depende de la spec de corrección de movimientos históricos.
- Cálculo de costo promedio, costo base y ganancia realizada (Posiciones, FASE 8).
- Efectivo disponible y su validación o advertencia en compras (FASE 9).
- Filtros `?desde=` y `?hasta=` y paginación.
- Cantidades recibidas por referidos en la validación de ventas (la spec de Referidos amplía la regla).
- Ayuda contextual y tutoriales "Registrar compra" / "Registrar venta" (FASE 12).

## 3. Modelo de datos

### 3.1 Migración

`backend/src/main/resources/db/migration/V4__crear_tabla_transaccion.sql`

```sql
CREATE TABLE transaccion (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    activo_id           BIGINT        NOT NULL,
    tipo_transaccion    VARCHAR(20)   NOT NULL,
    fecha_transaccion   DATE          NOT NULL,
    cantidad            NUMERIC(28,12) NOT NULL,
    precio_unitario     NUMERIC(19,6) NOT NULL,
    monto_bruto         NUMERIC(19,6) NOT NULL,
    comision            NUMERIC(19,6) NOT NULL DEFAULT 0,
    comentario          TEXT,
    fecha_creacion      TIMESTAMPTZ   NOT NULL,
    fecha_actualizacion TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_transaccion_activo        FOREIGN KEY (activo_id) REFERENCES activo (id),
    CONSTRAINT ck_transaccion_tipo          CHECK (tipo_transaccion IN ('COMPRA', 'VENTA')),
    CONSTRAINT ck_transaccion_cantidad      CHECK (cantidad > 0),
    CONSTRAINT ck_transaccion_precio        CHECK (precio_unitario > 0),
    CONSTRAINT ck_transaccion_comision      CHECK (comision >= 0),
    CONSTRAINT ck_transaccion_monto_bruto   CHECK (monto_bruto = ROUND(cantidad * precio_unitario, 2))
);

CREATE INDEX ix_transaccion_activo_fecha ON transaccion (activo_id, fecha_transaccion);
CREATE INDEX ix_transaccion_fecha        ON transaccion (fecha_transaccion);
```

- `ROUND` de PostgreSQL sobre `NUMERIC` redondea la mitad alejándose de cero. Con valores positivos equivale a `HALF_UP`.
- El índice compuesto cubre también las búsquedas por `activo_id` (02 §11 sugiere ambos).
- El tipo de activo no se guarda en la transacción: se lee del activo (01 §2.4).

### 3.2 Reglas de los datos

**Activo (`activoId`):** obligatorio. Si no existe: 404 `ACTIVO_NO_ENCONTRADO` (vía `ActivoServicio.buscar`). Cualquier activo registrado es válido.

**Tipo (`tipoTransaccion`):** obligatorio, `COMPRA` o `VENTA`. Otro valor: 400 `DATOS_INVALIDOS`.

**Fecha (`fechaTransaccion`):** obligatoria, `AAAA-MM-DD`, no posterior a hoy en `America/Guayaquil` (`Clock` de `ConfiguracionReloj`).

**Cantidad (`cantidad`):** obligatoria, mayor que cero, máximo 8 decimales y 16 dígitos enteros.

**Precio unitario (`precioUnitario`):** obligatorio, mayor que cero, máximo 6 decimales y 13 dígitos enteros.

**Comisión (`comision`):** obligatoria, mayor o igual que cero, máximo 2 decimales y 13 dígitos enteros. En una VENTA no puede superar `montoBruto` (400 en `comision`).

**Monto bruto (`montoBruto`):** lo calcula el backend: `cantidad × precioUnitario`, redondeado a 2 decimales `HALF_UP`. No se acepta en la solicitud; si llega, se ignora. Se guarda para conservar el hecho histórico.

**Efectivo neto (`efectivoNeto`):** se calcula al responder, no se guarda. Con signo, como la columna I del Excel:

- COMPRA: `−(montoBruto + comision)`.
- VENTA: `montoBruto − comision`.

**Comentario (`comentario`):** opcional, se recorta, vacío → `null`, máximo 500 caracteres.

**Formato de respuesta:** `cantidad` con escala 8; `precioUnitario` con escala 6 sin ceros finales forzados (se devuelve tal como se guardó, escala mínima 2); montos y comisiones con escala 2.

### 3.3 Validación cronológica de ventas

Saldo de un activo al cierre de una fecha D:

```text
saldo(D) = Σ cantidad COMPRA con fecha ≤ D − Σ cantidad VENTA con fecha ≤ D
```

Los movimientos de un mismo día cuentan como simultáneos.

Cantidad disponible para vender en la fecha D:

```text
disponible(D) = min( { saldo(D) } ∪ { saldo(F) : F > D y F tiene movimientos del activo } )
```

- `saldo(D)` siempre forma parte del mínimo, aunque D no tenga movimientos.
- Si no hay movimientos posteriores a D, `disponible(D) = saldo(D)`.
- Si el activo no tiene movimientos con fecha ≤ D, `saldo(D) = 0` y `disponible(D) = 0`.
- Basta evaluar las fechas con movimientos: entre dos de ellas el saldo no cambia.

Una venta de cantidad q en la fecha D es válida si `q ≤ disponible(D)`. Si no, 409 `CANTIDAD_INSUFICIENTE` con un mensaje que incluye la cantidad disponible.

Ejemplo: compra 1.0 el 10/10, venta 0.6 el 20/10. Una venta de 0.5 con fecha 15/10 se rechaza: el 20/10 el saldo quedaría en −0.1. `disponible(15/10)` = 0.4.

Al registrar una venta se bloquea la fila del activo (`PESSIMISTIC_WRITE`) para que dos ventas simultáneas no superen el disponible.

La spec de Referidos debe sumar las cantidades referidas a `saldo(D)`.

### 3.4 Método de costo (documentado, no implementado aquí)

Las ventas usarán **costo promedio ponderado móvil**. El costo base y la cantidad se acumulan recorriendo los movimientos del activo en orden.

Orden: por `fechaTransaccion`. En una misma fecha, primero las compras y después las ventas, coherente con §3.3. A igual fecha y tipo, por `id`.

```text
Inicio:  cantidad = 0, costoBase = 0

COMPRA:  costoBase += montoBruto + comision
         cantidad  += q

VENTA:   costoPromedio = costoBase / cantidad
         costoVendido  = q × costoPromedio
         costoBase    -= costoVendido
         cantidad     -= q
         (si cantidad = 0, costoBase = 0 exacto)

costo promedio actual = costoBase / cantidad   (0 si cantidad = 0)
```

- Una venta no cambia el costo promedio. Reduce el costo base por el costo de lo vendido.
- Una compra posterior recalcula el promedio sobre la cantidad remanente más la comprada.
- No se redondea en pasos intermedios. Se redondea solo al presentar.

Ejemplo: compra 1 a $100, venta de 1, compra 1 a $200. Costo promedio = $200, no $150.

Relación con el Excel: la fórmula del Excel (`Posiciones!K` y `O`) divide todas las compras históricas entre toda la cantidad comprada. Coincide con este método mientras no haya compras posteriores a una venta, como en los datos actuales. Después difiere, y el Excel lo reconoce como simplificación (Guía B57).

Esta spec solo documenta el método. La spec de Posiciones lo implementa. También define la ganancia realizada a partir de `costoVendido` y el tratamiento de los referidos en el costo base.

### 3.5 Backend (`com.personal.inversiones`)

```text
transacciones/
  controlador/TransaccionControlador.java
  servicio/TransaccionServicio.java            // registrar, listar, obtener, disponible
  servicio/CalculoCantidadDisponible.java      // saldo cronológico (lógica pura, prueba unitaria)
  repositorio/TransaccionRepositorio.java
  modelo/Transaccion.java
  modelo/TipoTransaccion.java                  // COMPRA, VENTA
  dto/CrearTransaccionSolicitud.java
  dto/TransaccionRespuesta.java
  dto/ListaTransaccionesRespuesta.java
  dto/CantidadDisponibleRespuesta.java
```

Cambios en código existente:

- `ActivoServicio.tieneRegistrosHistoricos` devuelve `true` si `TransaccionRepositorio.existsByActivoId(id)`.
- `ActivoRepositorio`: método de búsqueda con `@Lock(PESSIMISTIC_WRITE)` para ventas.

Todo en `BigDecimal`; fechas con `LocalDate.now(clock)`.

### 3.6 Contratos API

| Método | Ruta                               | Éxito | Respuesta                     |
| ------ | ---------------------------------- | ----- | ----------------------------- |
| POST   | `/api/v1/transacciones`            | 201   | `TransaccionRespuesta`        |
| GET    | `/api/v1/transacciones`            | 200   | `ListaTransaccionesRespuesta` |
| GET    | `/api/v1/transacciones/{id}`       | 200   | `TransaccionRespuesta`        |
| GET    | `/api/v1/transacciones/disponible` | 200   | `CantidadDisponibleRespuesta` |

No existen `PUT` ni `DELETE`.

**Solicitud `POST`:**

```json
{
  "activoId": 1,
  "tipoTransaccion": "COMPRA",
  "fechaTransaccion": "2026-10-22",
  "cantidad": 0.14553,
  "precioUnitario": 769.57,
  "comision": 0.15,
  "comentario": "Compra IVV"
}
```

**`TransaccionRespuesta`:**

```json
{
  "id": 5,
  "activoId": 1,
  "simbolo": "IVV",
  "nombreActivo": "iShares Core S&P 500 ETF",
  "tipoTransaccion": "COMPRA",
  "fechaTransaccion": "2026-10-22",
  "cantidad": 0.14553,
  "precioUnitario": 769.57,
  "montoBruto": 112.0,
  "comision": 0.15,
  "efectivoNeto": -112.15,
  "comentario": "Compra IVV"
}
```

**`GET /api/v1/transacciones?activoId=&tipo=`** (ambos opcionales). Cambia 04 §10.3 de lista a objeto:

```json
{
  "transacciones": [],
  "totalComprado": 503.07,
  "totalVendido": 0.0,
  "totalComisiones": 1.02,
  "conteoCompras": 7,
  "conteoVentas": 0
}
```

- `transacciones` ordenadas por `fechaTransaccion` descendente y, a igual fecha, por `id` descendente.
- `totalComprado`, `totalVendido` y `totalComisiones` se calculan sobre la lista filtrada (activo y tipo).
- `conteoCompras` y `conteoVentas` se calculan solo con el filtro de activo, para las pestañas Todas / Compras / Ventas.
- Sin resultados: lista vacía, totales y conteos en 0.
- `tipo` inválido: 400 `DATOS_INVALIDOS`. `activoId` inexistente: lista vacía.

**`GET /api/v1/transacciones/disponible?activoId=1&fecha=2026-10-22`:** `fecha` opcional (por defecto, hoy de negocio). Activo inexistente: 404 `ACTIVO_NO_ENCONTRADO`.

```json
{ "activoId": 1, "fecha": "2026-10-22", "cantidadDisponible": 0.54308 }
```

**Errores** (estructura de 04 §7):

| Caso                                                                                                                                                    | HTTP | `codigo`                              |
| ------------------------------------------------------------------------------------------------------------------------------------------------------- | ---- | ------------------------------------- |
| Campo obligatorio ausente, cantidad/precio ≤ 0, comisión negativa, exceso de decimales, fecha futura, tipo inválido, comentario largo, JSON mal formado | 400  | `DATOS_INVALIDOS` con `erroresCampos` |
| Comisión de VENTA mayor que `montoBruto`                                                                                                                | 400  | `DATOS_INVALIDOS`, campo `comision`   |
| `activoId` inexistente (POST o disponible)                                                                                                              | 404  | `ACTIVO_NO_ENCONTRADO`                |
| `/transacciones/{id}` inexistente                                                                                                                       | 404  | `TRANSACCION_NO_ENCONTRADA` (nuevo)   |
| Venta mayor que `disponible(fecha)`                                                                                                                     | 409  | `CANTIDAD_INSUFICIENTE`               |
| `PUT`, `DELETE`                                                                                                                                         | 405  | `SOLICITUD_INVALIDA`                  |
| Error inesperado                                                                                                                                        | 500  | `ERROR_INTERNO`                       |

Mensaje de 409, ejemplo: "La cantidad supera la disponible para vender en esa fecha (0.40000000 IVV)."

`TRANSACCION_INVALIDA` queda sin uso (se usa `DATOS_INVALIDOS`, como en SPEC 001 y 002).

### 3.7 Frontend (`frontend/app-inversiones/src/app/`)

```text
transacciones/transaccion.model.ts
transacciones/transaccion.service.ts              // HTTP /api/v1/transacciones
transacciones/transacciones-pagina.component.ts   // filtros, historial, totales, estados
transacciones/transaccion-form.component.ts       // formulario lateral (Reactive Forms)
```

- Reutiliza `activos/activo.service.ts` para el selector de activo.
- `app.routes.ts` agrega `transacciones`.
- `nucleo/layout/layout.component.ts` agrega "Transacciones" al grupo Registro, después de Aportes.

## 4. Plan de implementación

1. **Documentación.**
   - `01-modelo-funcional.md` §4: monto bruto redondeado a 2 decimales; efectivo neto con signo; validación cronológica de ventas; comisión de venta ≤ monto bruto; sin edición ni eliminación en el MVP.
   - `01-modelo-funcional.md` §10.2: método de costo = costo promedio ponderado móvil, con el algoritmo de §3.4.
   - `02-base-datos.md` §11: columna `monto_bruto`, restricciones con nombre, índices.
   - `04-contratos-api.md` §10: contratos de esta spec; §20: `TRANSACCION_NO_ENCONTRADA`; §22: efectivo neto.
   - `05-guia-interfaz.md`, "Diferencias conocidas con el prototipo": selector con todos los activos (no hay estado ACTIVO).
   - `CLAUDE.md`, "Decisiones todavía pendientes": quitar método de costo y validaciones de venta.
2. **Tabla y modelo.** `V4__crear_tabla_transaccion.sql`, `Transaccion`, `TipoTransaccion`, `TransaccionRepositorio`. Arranca con `ddl-auto=validate`.
3. **Registrar compra.** `CrearTransaccionSolicitud`, `TransaccionServicio.registrar` (COMPRA), `POST`. Pruebas de integración: cálculo de `montoBruto` y `efectivoNeto`, validaciones de campos, decimales, fecha futura, activo inexistente, `montoBruto` enviado ignorado.
4. **Bloqueo de símbolo.** Ampliar `tieneRegistrosHistoricos`. Prueba: activo con una transacción → cambiar símbolo responde 409 `SIMBOLO_BLOQUEADO`; tipo sigue editable.
5. **Cantidad disponible.** `CalculoCantidadDisponible` con pruebas unitarias (caso del ejemplo §3.3, mismo día, sin movimientos). `GET /disponible`.
6. **Registrar venta.** Validación cronológica con bloqueo pesimista y comisión ≤ monto. Pruebas: venta total, venta parcial, venta retroactiva que rompe un saldo futuro (409), venta sin compras (409), comisión mayor al monto (400).
7. **Consultar.** `GET` por id, `GET` lista con filtros, totales y conteos. Pruebas: orden, filtros combinados, vacío, `tipo` inválido, 404 `TRANSACCION_NO_ENCONTRADA`, 405 en PUT/DELETE.
8. **Servicio y modelo frontend.** `transaccion.model.ts`, `transaccion.service.ts`, pruebas Vitest con `HttpTestingController`.
9. **Pantalla (consulta).** Ruta, navegación, pestañas Todas/Compras/Ventas con conteos, selector de activo, tabla en escritorio (Fecha, Operación, Cantidad, Precio unitario, Comisión, Monto) con fila de totales, lista compacta en móvil, estados de carga, vacío ("No existen transacciones registradas." con "Registrar primera transacción") y error.
10. **Formulario.** Selector Compra/Venta, activo, fecha (hoy), cantidad, precio, comisión, comentario. Vista previa "Monto aproximado" y "Efectivo utilizado/recibido" (solo ayuda visual). En Venta muestra "Disponible para vender" desde el endpoint, recargado al cambiar activo o fecha. Errores de campo y 409 visibles. Al guardar: "Compra registrada correctamente." / "Venta registrada correctamente.", limpia y recarga.
11. **Comparación con el Excel.** `ComparacionExcelTransaccionesTest`: registra los 3 activos, los 3 aportes y las 7 compras del Excel y compara. Revisión manual de la pantalla con los mismos datos.

## 5. Criterios de aceptación

**Backend:**

- [ ] `mvn verify` compila y todas las pruebas pasan (Docker en ejecución).
- [ ] Flyway aplica V1–V4 sobre base vacía y Hibernate valida con `ddl-auto=validate`.
- [ ] POST COMPRA IVV 0.14553 × 769.57, comisión 0.15 → 201, `montoBruto` 112.00, `efectivoNeto` −112.15.
- [ ] POST con `montoBruto` en la solicitud no altera el calculado.
- [ ] Cantidad o precio = 0 o negativos → 400 en ese campo. Comisión −0.01 → 400; comisión 0 → 201.
- [ ] Cantidad con 9 decimales, precio con 7 o comisión con 3 → 400.
- [ ] Fecha de mañana → 400; hoy (Guayaquil) → 201.
- [ ] Sin `activoId`, `tipoTransaccion`, `fechaTransaccion`, `cantidad`, `precioUnitario` o `comision` → 400 con ese campo.
- [ ] `tipoTransaccion` = "CANJE" → 400. `activoId` = 999 → 404 `ACTIVO_NO_ENCONTRADO`.
- [ ] Comentario de 501 caracteres → 400; `"   "` → `null`.
- [ ] Compra 1.0 el 2026-10-10 y venta 1.0 el 2026-10-12 → 201 (venta total).
- [ ] Con compra 1.0 el 10/10 y venta 0.6 el 20/10, venta de 0.5 el 15/10 → 409 `CANTIDAD_INSUFICIENTE`; de 0.4 → 201.
- [ ] Venta sin compras previas → 409. Compra y venta del mismo día por la misma cantidad → 201.
- [ ] VENTA con comisión mayor que `montoBruto` → 400 en `comision`.
- [ ] `GET /disponible?activoId=&fecha=2026-10-15` en el escenario anterior → 0.40000000. Sin `fecha` usa hoy.
- [ ] En el mismo escenario, `fecha=2026-10-25` (sin movimientos ese día ni después) → 0.40000000, igual a `saldo(25/10)`. `fecha=2026-10-05` (antes de toda compra) → 0.
- [ ] Un activo con transacciones no permite cambiar el símbolo (409 `SIMBOLO_BLOQUEADO`) y sí el tipo.
- [ ] `GET /transacciones` ordena por fecha e id descendentes; `?activoId=` y `?tipo=` filtran; totales sobre la lista filtrada; conteos solo con filtro de activo.
- [ ] `GET /transacciones?tipo=OTRO` → 400. Sin datos → lista vacía y ceros.
- [ ] `GET /transacciones/999` → 404 `TRANSACCION_NO_ENCONTRADA`. `PUT`/`DELETE` → 405.
- [ ] Ninguna respuesta expone `fechaCreacion`, `fechaActualizacion`, stack traces ni SQL.

**Frontend:**

- [ ] `npm test` pasa.
- [ ] "Transacciones" aparece en la navegación y `/transacciones` abre la pantalla.
- [ ] Las pestañas muestran los conteos y el selector filtra por activo usando la API.
- [ ] Con 0.10 y 770.00 la vista previa muestra $77.00 y no es editable.
- [ ] En Venta se muestra "Disponible para vender" del backend y cambia al cambiar activo o fecha.
- [ ] Una venta mayor al disponible muestra el mensaje del 409 sin perder lo ingresado.
- [ ] Totales mostrados = los del backend; el frontend no suma la lista.
- [ ] A 390 px el historial es lista compacta, sin scroll horizontal de página.
- [ ] Montos `$1,234.56`, cantidades con 8 decimales, fechas `DD/MM/AAAA`.

**Excel** (hoja Transacciones de `referencia/registro_inversiones_v4_1.xlsx`):

- [ ] Las 7 compras (2026-10-13, 2026-10-19 ×3, 2026-10-22 ×3) dan `montoBruto` 120.07, 188.00, 36.00, 17.00, 112.00, 20.00, 10.00 y `efectivoNeto` igual a la columna I.
- [ ] `totalComprado` = 503.07 (`Resumen!B7`) y `totalComisiones` = 1.02 (`Resumen!B8`).
- [ ] Disponible a hoy: IVV 0.54308000, VXUS 0.67273000, BTC 0.00032988 (`Posiciones!C5:C7`, sin referidos).
- [ ] Neto de aportes 559.13 + Σ `efectivoNeto` (−504.09) = 55.04 (`Resumen!B11`, sin dividendos).

## 6. Decisiones

- **Sí:** `montoBruto` = cantidad × precio redondeado a 2 decimales `HALF_UP`, calculado por backend y guardado con CHECK. Reproduce los 7 registros del Excel. Decisión del usuario.
- **No:** ingresar el monto como el Excel, ni calcularlo sin redondear (daría 503.05 en vez de 503.07).
- **Sí:** ventas incluidas, con validación cronológica por día. Decisión del usuario.
- **No:** validar solo contra la cantidad actual: permitiría saldos negativos en fechas pasadas.
- **Sí:** método de costo para ventas = costo promedio ponderado móvil (§3.4). La venta descuenta su costo al promedio vigente y el promedio se recalcula solo con compras. Se implementa en Posiciones. Decisión del usuario.
- **No:** la fórmula estática del Excel (todas las compras / toda la cantidad comprada). Asigna mal el costo cuando se compra después de vender.
- **Sí:** en una misma fecha, las compras se procesan antes que las ventas, coherente con la validación de §3.3.
- **No:** FIFO ni lotes específicos.
- **No:** validar compras contra el efectivo disponible. El Excel no lo hace y el efectivo llega en FASE 9. Decisión del usuario.
- **Sí:** solo crear y consultar; sin `PUT` ni `DELETE`. Decisión del usuario, igual que Aportes.
- **Sí:** precisión de entrada: cantidad 8 decimales, precio 6, comisión 2. Columnas de 02 §6–7 sin cambios. Decisión del usuario.
- **Sí:** endpoint `GET /transacciones/disponible` con la misma regla que valida la venta, para no calcular en el frontend. Decisión del usuario.
- **Sí:** el tipo de activo sigue editable con transacciones; solo el símbolo se bloquea. El tipo no participa en cálculos. Decisión del usuario.
- **Sí:** `efectivoNeto` con signo (compra negativa, venta positiva), calculado al responder. Igual que la columna I del Excel; Efectivo (FASE 9) lo sumará.
- **Sí:** comisión obligatoria (puede ser 0), para no omitirla en silencio.
- **Sí:** en VENTA la comisión no puede superar el monto bruto; evita un efectivo recibido negativo.
- **Sí:** movimientos del mismo día cuentan como simultáneos; no hay hora en las transacciones.
- **Sí:** bloqueo pesimista del activo al registrar ventas.
- **Sí:** `GET /transacciones` devuelve objeto con lista, totales y conteos, como `/aportes`. Cambia 04 §10.3.
- **Sí:** activo inexistente → 404 `ACTIVO_NO_ENCONTRADO`, reutilizando `ActivoServicio.buscar` como Asignaciones.
- **Sí:** nuevo código `TRANSACCION_NO_ENCONTRADA`. `TRANSACCION_INVALIDA` queda sin uso.
- **Sí:** el selector muestra todos los activos. El prototipo menciona "estado ACTIVO", que 02 §8 eliminó.
- **Sí:** la vista previa de monto y efectivo en el formulario es solo ayuda visual (05 §24).
- **Sí:** migración `V4__crear_tabla_transaccion.sql` (V1–V3 ya existen).

## 7. Riesgos

| Riesgo                                                                 | Mitigación                                                                                |
| ---------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| El broker cobra un monto distinto de `redondear(cantidad × precio, 2)` | Las 7 compras reales coinciden. Si aparece un caso, se revisa la regla en una spec nueva. |
| La spec de Referidos olvida sumar referidos al saldo                   | Queda escrito en §3.3; debe ser criterio de aceptación de Referidos.                      |
| Una transacción mal registrada no se puede corregir                    | Fuera de alcance explícito; spec de corrección de movimientos.                            |
| Dos ventas simultáneas superan el disponible                           | Bloqueo pesimista de la fila del activo.                                                  |
| Recalcular el saldo con muchas transacciones es lento                  | Volumen personal pequeño; índice `(activo_id, fecha_transaccion)`.                        |

## Lo que **no** entra en esta spec

- Editar o eliminar transacciones.
- Costo promedio, costo base y ganancia realizada.
- Efectivo disponible.
- Filtros por fecha y paginación.
- Referidos en el saldo disponible.
- Ayuda contextual y tutoriales.

Cada uno de estos puntos, si se aborda, irá en su propia spec o tarea.
