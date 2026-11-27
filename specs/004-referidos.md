# SPEC 004 — Referidos

> **Estado:** Aprobado
> **Depende de:** SPEC 001, SPEC 003
> **Fecha:** 2026-11-24
> **Objetivo:** Registrar y consultar los activos recibidos por programas de referidos, con el valor recibido calculado por el backend y la cantidad referida sumada a la cantidad disponible para vender, desde una pantalla Referidos.

## 1. Por qué existe esta spec

Es la FASE 5 de `documentacion/07-hoja-ruta.md`.

Un referido aumenta la cantidad de un activo sin ser aporte, compra ni dividendo (01 §5). Es la tercera fuente de cantidad de las posiciones (01 §9.2).

SPEC 003 §3.3 dejó una obligación: la cantidad referida debe sumarse al saldo que valida las ventas. Esta spec la cumple.

El valor actual y la variación dependen del último precio del activo. La tabla `precio_activo` llega en la FASE 7. Esta spec solo maneja los datos históricos; la spec de Precios agrega la parte calculada con precios (ver §6).

Reutiliza de las SPEC 001 a 003: estructura de errores, `ActivoServicio.buscar`, el `Clock` de negocio, la base de pruebas con Testcontainers, el shell y los estilos globales.

Esta spec cambia decisiones documentadas (ver §6). La documentación general se actualiza en el paso 1 del plan.

## 2. Alcance

**Incluye:**

- Tabla `referido` mediante `V5__crear_tabla_referido.sql`.
- Registrar un referido (`POST /api/v1/referidos`) con `valorRecibido` calculado en backend.
- Listar referidos con totales (`GET /api/v1/referidos`) y consultar uno (`GET /api/v1/referidos/{id}`).
- Sumar las cantidades referidas al saldo de la validación de ventas y a `GET /api/v1/transacciones/disponible`.
- Bloqueo del símbolo de un activo con referidos (ampliar `ActivoServicio.tieneRegistrosHistoricos`).
- Pantalla Referidos: totales, historial (tabla en escritorio, lista en móvil) y formulario lateral de registro.
- Entrada "Referidos" en la navegación.
- Comparación con la hoja Referidos del Excel.

**Fuera del alcance (para futuras specs):**

- Precio actual, valor actual, variación y rendimiento desde la entrega. Los agrega la spec de Precios (FASE 7).
- Editar o eliminar referidos. Depende de la spec de corrección de movimientos históricos.
- Tratamiento del referido en el costo base, el costo promedio y la ganancia (Posiciones, FASE 8).
- Valor de referidos en el resumen y en las fotografías (FASE 10 y 11).
- Filtros por activo o por fecha y paginación.
- Catálogo cerrado de orígenes.
- Ayuda contextual y tutorial "Registrar referido" (FASE 12).

## 3. Modelo de datos

### 3.1 Migración

`backend/src/main/resources/db/migration/V5__crear_tabla_referido.sql`

```sql
CREATE TABLE referido (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    activo_id           BIGINT         NOT NULL,
    fecha_referido      DATE           NOT NULL,
    origen              VARCHAR(100)   NOT NULL,
    cantidad            NUMERIC(28,12) NOT NULL,
    precio_al_recibir   NUMERIC(19,6)  NOT NULL,
    valor_recibido      NUMERIC(19,6)  NOT NULL,
    comentario          TEXT,
    fecha_creacion      TIMESTAMPTZ    NOT NULL,
    fecha_actualizacion TIMESTAMPTZ    NOT NULL,
    CONSTRAINT fk_referido_activo         FOREIGN KEY (activo_id) REFERENCES activo (id),
    CONSTRAINT ck_referido_cantidad       CHECK (cantidad > 0),
    CONSTRAINT ck_referido_precio         CHECK (precio_al_recibir > 0),
    CONSTRAINT ck_referido_valor_recibido CHECK (valor_recibido = ROUND(cantidad * precio_al_recibir, 2))
);

CREATE INDEX ix_referido_activo_fecha ON referido (activo_id, fecha_referido);
```

- `origen` pasa a `NOT NULL` respecto de 02 §12 (decisión del usuario).
- La restricción de `valor_recibido` implica `valor_recibido >= 0`, como pide 02 §12.
- `ROUND` de PostgreSQL sobre `NUMERIC` equivale a `HALF_UP` con valores positivos (igual que `transaccion.monto_bruto`).

### 3.2 Reglas de los datos

**Activo (`activoId`):** obligatorio. Si no existe: 404 `ACTIVO_NO_ENCONTRADO` (vía `ActivoServicio.buscar`). Se acepta cualquier activo registrado, de cualquier tipo.

**Fecha (`fechaReferido`):** obligatoria, `AAAA-MM-DD`, no posterior a hoy en `America/Guayaquil` (`Clock` de `ConfiguracionReloj`).

**Origen (`origen`):** obligatorio, texto libre. Se recorta y debe quedar con al menos un carácter. Máximo 100 caracteres. Ejemplo: `Programa de referidos Hapi`.

**Cantidad (`cantidad`):** obligatoria, mayor que cero, máximo 8 decimales y 16 dígitos enteros (igual que Transacciones).

**Precio al recibir (`precioAlRecibir`):** obligatorio, mayor que cero, máximo 6 decimales y 13 dígitos enteros (igual que el precio unitario de Transacciones).

**Valor recibido (`valorRecibido`):** lo calcula el backend: `cantidad × precioAlRecibir`, redondeado a 2 decimales `HALF_UP`. No se acepta en la solicitud; si llega, se ignora. Se guarda para conservar el hecho histórico. Puede ser 0.00 si el producto es menor que 0.005.

**Comentario (`comentario`):** opcional. Se recorta, vacío → `null`, máximo 500 caracteres.

**Inmutabilidad:** cantidad, precio al recibir y valor recibido no cambian después del registro (01 §5.3). En el MVP no hay edición ni eliminación.

**Formato de respuesta:** `cantidad` con escala 8. `precioAlRecibir` tal como se guardó, con escala mínima 2. `valorRecibido` y totales con escala 2.

### 3.3 Efecto en la cantidad disponible para vender

Se amplía el saldo de SPEC 003 §3.3:

```text
saldo(D) = Σ cantidad COMPRA con fecha ≤ D
         − Σ cantidad VENTA con fecha ≤ D
         + Σ cantidad REFERIDO con fecha ≤ D
```

- La fórmula de `disponible(D)` no cambia. Las fechas de los referidos cuentan como fechas con movimientos.
- Un referido es un `Movimiento` positivo de `CalculoCantidadDisponible`, con su `fechaReferido`.
- Los referidos y las transacciones de un mismo día cuentan como simultáneos.
- Afecta a la validación de ventas (409 `CANTIDAD_INSUFICIENTE`) y a `GET /api/v1/transacciones/disponible`, que siguen usando la misma regla.

Ejemplo: referido 0.5 el 10/10. Una venta de 0.5 el 12/10 es válida. Una venta de 0.5 el 09/10 se rechaza con 409: `disponible(09/10)` = 0.

Registrar un referido solo aumenta el saldo. No necesita bloqueo pesimista.

### 3.4 Backend (`com.personal.inversiones`)

```text
referidos/
  controlador/ReferidoControlador.java
  servicio/ReferidoServicio.java            // registrar, listar, obtener
  repositorio/ReferidoRepositorio.java      // existsByActivoId, findByActivoId
  modelo/Referido.java
  dto/CrearReferidoSolicitud.java
  dto/ReferidoRespuesta.java
  dto/ListaReferidosRespuesta.java
```

Cambios en código existente:

- `ActivoServicio.tieneRegistrosHistoricos` devuelve `true` también si `ReferidoRepositorio.existsByActivoId(id)`.
- `TransaccionServicio.cantidadDisponible` agrega los referidos del activo como movimientos positivos (`ReferidoRepositorio.findByActivoId`).
- Javadoc de `CalculoCantidadDisponible.Movimiento`: quitar "en el futuro" del comentario sobre referidos.

Todo en `BigDecimal`; fechas con `LocalDate.now(clock)`.

### 3.5 Contratos API

| Método | Ruta                     | Éxito | Respuesta                 |
| ------ | ------------------------ | ----- | ------------------------- |
| POST   | `/api/v1/referidos`      | 201   | `ReferidoRespuesta`       |
| GET    | `/api/v1/referidos`      | 200   | `ListaReferidosRespuesta` |
| GET    | `/api/v1/referidos/{id}` | 200   | `ReferidoRespuesta`       |

No existen `PUT` ni `DELETE`.

**Solicitud `POST`:**

```json
{
  "activoId": 3,
  "fechaReferido": "2026-10-13",
  "origen": "Programa de referidos Hapi",
  "cantidad": 0.00001196,
  "precioAlRecibir": 83580.27,
  "comentario": "Premio Hapi por referido: $1.00 en BTC"
}
```

**`ReferidoRespuesta`:**

```json
{
  "id": 1,
  "activoId": 3,
  "simbolo": "BTC",
  "nombreActivo": "Bitcoin",
  "fechaReferido": "2026-10-13",
  "origen": "Programa de referidos Hapi",
  "cantidad": 0.00001196,
  "precioAlRecibir": 83580.27,
  "valorRecibido": 1.0,
  "comentario": "Premio Hapi por referido: $1.00 en BTC"
}
```

**`GET /api/v1/referidos`** (sin filtros). Cambia 04 §11.2 a objeto, como `/aportes`:

```json
{
  "referidos": [],
  "totalValorRecibido": 1.0,
  "conteo": 1
}
```

- `referidos` ordenados por `fechaReferido` descendente y, a igual fecha, por `id` descendente.
- Sin datos: lista vacía, `totalValorRecibido` 0.00 y `conteo` 0.

**Errores** (estructura de 04 §7):

| Caso                                                                                                                                        | HTTP | `codigo`                              |
| ------------------------------------------------------------------------------------------------------------------------------------------- | ---- | ------------------------------------- |
| Campo obligatorio ausente, origen vacío o largo, cantidad/precio ≤ 0, exceso de decimales, fecha futura, comentario largo, JSON mal formado | 400  | `DATOS_INVALIDOS` con `erroresCampos` |
| `activoId` inexistente                                                                                                                      | 404  | `ACTIVO_NO_ENCONTRADO`                |
| `/referidos/{id}` inexistente                                                                                                               | 404  | `REFERIDO_NO_ENCONTRADO` (nuevo)      |
| `PUT`, `DELETE`                                                                                                                             | 405  | `SOLICITUD_INVALIDA`                  |
| Error inesperado                                                                                                                            | 500  | `ERROR_INTERNO`                       |

`REFERIDO_INVALIDO` queda sin uso (se usa `DATOS_INVALIDOS`, como en las SPEC 001 a 003).

`GET /api/v1/transacciones/disponible` no cambia de contrato; cambia su resultado cuando existen referidos.

### 3.6 Frontend (`frontend/app-inversiones/src/app/`)

```text
referidos/referido.model.ts
referidos/referido.service.ts              // HTTP /api/v1/referidos
referidos/referidos-pagina.component.ts    // totales, historial, estados
referidos/referido-form.component.ts       // formulario lateral (Reactive Forms)
```

- Reutiliza `activos/activo.service.ts` para el selector de activo.
- `app.routes.ts` agrega `referidos`.
- `nucleo/layout/layout.component.ts` agrega "Referidos" al grupo Registro, después de Transacciones.
- Totales: "Valor al recibir" (`totalValorRecibido`) con el texto "Histórico · N referidos" (`conteo`).
- Historial en escritorio: Fecha y origen, Activo, Cantidad × precio, Valor recibido. Grupo de columnas "Al recibir · histórico" como en el prototipo.
- Historial en móvil: lista compacta con los mismos datos.
- No se muestran columnas "Hoy · calculado" vacías ni con marcadores. Llegan con la spec de Precios.

## 4. Plan de implementación

1. **Documentación.**
   - `01-modelo-funcional.md` §5: valor recibido calculado (redondeado a 2 decimales, mitad hacia arriba); origen obligatorio; fecha no posterior a hoy de negocio; precio al recibir > 0; sin edición ni eliminación en el MVP; valor actual y variación llegan con Precios.
   - `01-modelo-funcional.md` §4.7: concretar que el referido suma su cantidad al saldo desde su fecha.
   - `02-base-datos.md` §12: identidad, `origen NOT NULL`, restricciones con nombre, CHECK de `valor_recibido`, índice.
   - `04-contratos-api.md` §11: contratos de esta spec. §20: `REFERIDO_NO_ENCONTRADO`. §22: valor recibido del referido.
   - `05-guia-interfaz.md` §26 y "Diferencias conocidas con el prototipo": la parte "Hoy" llega con Precios; el valor recibido es una vista previa en el formulario; los datos del prototipo son simulados (precio $83,612.04).
   - `07-hoja-ruta.md`: el valor actual de referidos pasa de FASE 5 a FASE 7.
2. **Tabla y modelo.** `V5__crear_tabla_referido.sql`, `Referido`, `ReferidoRepositorio`. Arranca con `ddl-auto=validate`.
3. **Registrar.** `CrearReferidoSolicitud`, `ReferidoServicio.registrar`, `POST`. Pruebas de integración: cálculo de `valorRecibido`, `valorRecibido` enviado ignorado, validaciones de campos, decimales, origen vacío y largo, fecha futura, activo inexistente.
4. **Consultar.** `GET` por id y `GET` lista con total y conteo. Pruebas: orden, vacío, 404 `REFERIDO_NO_ENCONTRADO`, 405 en `PUT`/`DELETE`.
5. **Bloqueo de símbolo.** Ampliar `tieneRegistrosHistoricos`. Prueba: activo con un referido y sin transacciones → cambiar símbolo responde 409 `SIMBOLO_BLOQUEADO`; `simboloEditable` = `false`.
6. **Disponible para vender.** Sumar referidos en `TransaccionServicio.cantidadDisponible`. Prueba unitaria en `CalculoCantidadDisponibleTest` con un movimiento de referido. Pruebas de integración del ejemplo de §3.3.
7. **Servicio y modelo frontend.** `referido.model.ts`, `referido.service.ts`, pruebas Vitest con `HttpTestingController`.
8. **Pantalla (consulta).** Ruta, navegación, totales, tabla en escritorio, lista compacta en móvil, estados de carga, vacío ("No existen referidos registrados." con "Registrar primer referido") y error.
9. **Formulario.** Fecha (hoy), activo, origen, cantidad, precio al recibir, comentario. Vista previa "Valor recibido" calculada como cantidad × precio (solo ayuda visual, no editable). Errores de campo visibles. Al guardar: "Referido registrado correctamente.", limpia y recarga.
10. **Comparación con el Excel.** `ComparacionExcelReferidosTest`: registra los 3 activos, las 7 compras de SPEC 003 y el referido de la hoja Referidos, y compara. Revisión manual de la pantalla con los mismos datos.

## 5. Criterios de aceptación

**Backend:**

- [ ] `mvn verify` compila y todas las pruebas pasan (Docker en ejecución).
- [ ] Flyway aplica V1–V5 sobre base vacía y Hibernate valida con `ddl-auto=validate`.
- [ ] POST BTC 0.00001196 × 83580.27 → 201 con `valorRecibido` 1.00.
- [ ] POST con `valorRecibido` en la solicitud no altera el calculado.
- [ ] POST 0.00000001 × 100.00 → 201 con `valorRecibido` 0.00.
- [ ] Cantidad o precio = 0 o negativos → 400 en ese campo.
- [ ] Cantidad con 9 decimales o precio con 7 → 400.
- [ ] Fecha de mañana → 400; hoy (Guayaquil) → 201.
- [ ] Sin `activoId`, `fechaReferido`, `origen`, `cantidad` o `precioAlRecibir` → 400 con ese campo.
- [ ] `origen` = `"   "` → 400. `origen` de 101 caracteres → 400; de 100 → 201.
- [ ] `activoId` = 999 → 404 `ACTIVO_NO_ENCONTRADO`.
- [ ] Comentario de 501 caracteres → 400; `"   "` → `null`.
- [ ] `GET /referidos` ordena por fecha e id descendentes y devuelve `totalValorRecibido` y `conteo`. Sin datos → lista vacía, 0.00 y 0.
- [ ] `GET /referidos/999` → 404 `REFERIDO_NO_ENCONTRADO`. `PUT`/`DELETE` → 405.
- [ ] Un activo con un referido y sin transacciones no permite cambiar el símbolo (409 `SIMBOLO_BLOQUEADO`) y responde `simboloEditable` = `false`.
- [ ] Con un referido de 0.5 el 2026-10-10 y sin transacciones: venta de 0.5 el 2026-10-12 → 201; venta de 0.5 el 2026-10-09 → 409 `CANTIDAD_INSUFICIENTE`.
- [ ] En el mismo escenario, `GET /transacciones/disponible?fecha=2026-10-10` → 0.50000000 y `fecha=2026-10-09` → 0.00000000.
- [ ] Compra 1.0 el 10/10, referido 0.2 el 15/10, venta 1.2 el 20/10 → 201; con la venta el 14/10 → 409.
- [ ] Registrar un referido no cambia `GET /aportes` ni los totales de `GET /transacciones`.
- [ ] Ninguna respuesta expone `fechaCreacion`, `fechaActualizacion`, stack traces ni SQL.

**Frontend:**

- [ ] `npm test` pasa.
- [ ] "Referidos" aparece en la navegación después de Transacciones y `/referidos` abre la pantalla.
- [ ] Con 0.00001196 y 83580.27 la vista previa muestra $1.00 y no es editable.
- [ ] El total "Valor al recibir" y el conteo son los del backend; el frontend no suma la lista.
- [ ] Un error 400 muestra el mensaje en su campo sin perder lo ingresado.
- [ ] No aparecen columnas de valor actual, variación ni rendimiento.
- [ ] A 390 px el historial es lista compacta, sin scroll horizontal de página.
- [ ] Montos `$1,234.56`, cantidades con 8 decimales, fechas `DD/MM/AAAA`.

**Excel** (hoja Referidos de `referencia/registro_inversiones_v4_1.xlsx`):

- [ ] El referido BTC del 2026-10-13 (0.00001196 × 83580.27) da `valorRecibido` 1.00 (`Referidos!F5`).
- [ ] `totalValorRecibido` = 1.00 y `conteo` = 1.
- [ ] Disponible a hoy: BTC 0.00034184 (`Posiciones!F7`), IVV 0.54308000 y VXUS 0.67273000 sin cambios (`Posiciones!F5:F6`).
- [ ] `totalComprado` sigue en 503.07 (`Resumen!B7`): el referido no es compra.

## 6. Decisiones

- **Sí:** `valorRecibido` = cantidad × precio al recibir, redondeado a 2 decimales `HALF_UP`, calculado por backend y guardado con CHECK. Reproduce el $1.00 del Excel y sigue la regla del monto bruto. Decisión del usuario.
- **No:** ingresar el valor recibido a mano (como 04 §11.1 y la fila del Excel), ni ingresarlo y validarlo con tolerancia.
- **Sí:** esta spec solo maneja datos históricos. Precio actual, valor actual, variación y rendimiento se agregan en la spec de Precios. Decisión del usuario.
- **No:** campos nulos "Sin precio" en la API y la pantalla desde ahora, ni adelantar la FASE 7.
- **Sí:** `origen` obligatorio, texto libre de hasta 100 caracteres. Sin origen el referido pierde trazabilidad. Cambia 02 §12 (`NOT NULL`). Decisión del usuario.
- **No:** catálogo cerrado de orígenes.
- **Sí:** `GET /referidos` sin filtros, con `totalValorRecibido` y `conteo`. Pocos registros. Decisión del usuario.
- **No:** filtro por activo en esta spec.
- **Sí:** la cantidad referida suma al saldo desde `fechaReferido`, con la misma regla cronológica de SPEC 003 §3.3. Cumple 01 §4.7.
- **Sí:** referidos y transacciones del mismo día cuentan como simultáneos.
- **Sí:** sin bloqueo pesimista al registrar referidos: solo aumentan el saldo.
- **Sí:** un referido bloquea el símbolo del activo (01 §2.4).
- **Sí:** solo crear y consultar; sin `PUT` ni `DELETE`. Igual que Aportes y Transacciones.
- **Sí:** precisión de entrada igual a Transacciones: cantidad 8 decimales, precio 6.
- **Sí:** se acepta cualquier tipo de activo. Hay programas que entregan acciones o ETF.
- **Sí:** nuevo código `REFERIDO_NO_ENCONTRADO`. `REFERIDO_INVALIDO` queda sin uso.
- **Sí:** la columna `Costo desde bolsillo` del Excel (siempre 0) no se guarda. Un referido no tiene costo desde bolsillo por definición (01 §5.6).
- **Sí:** el costo del referido en el costo base y el costo promedio se define en la spec de Posiciones, como indicó SPEC 003 §3.4.
- **Sí:** la vista previa del valor recibido en el formulario es solo ayuda visual (05 §24).
- **Sí:** migración `V5__crear_tabla_referido.sql` (V1–V4 ya existen).

## 7. Riesgos

| Riesgo                                                                     | Mitigación                                                                                     |
| -------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| El programa informa un valor distinto de `redondear(cantidad × precio, 2)` | El único caso real coincide. Si aparece otro, se revisa la regla en una spec nueva.            |
| La spec de Precios olvida agregar el valor actual de los referidos         | Queda escrito en §2 y en la hoja de ruta (paso 1); debe ser criterio de aceptación de Precios. |
| La spec de Posiciones mezcla el referido con el costo de compras           | SPEC 003 §3.4 y esta spec lo dejan explícito para Posiciones.                                  |
| Un referido mal registrado no se puede corregir                            | Fuera de alcance explícito; spec de corrección de movimientos.                                 |
| Un referido con fecha errónea permite una venta que no debía               | El referido es inmutable; la corrección depende de la spec de corrección de movimientos.       |

## Lo que **no** entra en esta spec

- Precio actual, valor actual, variación y rendimiento del referido.
- Editar o eliminar referidos.
- Costo del referido en posiciones.
- Valor de referidos en el resumen y las fotografías.
- Filtros y paginación.
- Ayuda contextual y tutoriales.

Cada uno de estos puntos, si se aborda, irá en su propia spec o tarea.
