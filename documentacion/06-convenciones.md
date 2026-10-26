# Convenciones del proyecto

## 1. Objetivo

Este documento define las convenciones que se utilizarán durante el desarrollo del Analizador de Inversiones Personales.

Su objetivo es mantener consistencia entre:

- backend;
- frontend;
- base de datos;
- API REST;
- pruebas;
- documentación;
- Git.

Estas convenciones deben respetarse durante todo el desarrollo, salvo que una especificación aprobada indique explícitamente lo contrario.

---

## 2. Idioma del proyecto

El proyecto utilizará español para:

- nombres de módulos;
- clases relacionadas con negocio;
- DTO;
- endpoints;
- tablas;
- columnas;
- variables de negocio;
- documentación;
- mensajes mostrados al usuario.

Se mantendrán en inglés únicamente términos técnicos propios del framework o de librerías cuando corresponda.

Ejemplos:

```text
Activo
Aporte
Transaccion
PrecioActivo
ResumenPortafolio
```

Evitar:

```text
Asset
Contribution
PortfolioSummary
```

---

## 3. Convenciones generales de nombres

### 3.1 Clases

Usar `PascalCase`.

```text
Activo
ActivoServicio
ActivoControlador
ActivoRepositorio
CrearActivoSolicitud
ActivoRespuesta
```

---

### 3.2 Métodos y variables

Usar `camelCase`.

```text
obtenerActivo()
registrarAporte()
calcularEfectivoDisponible()

precioActual
montoNeto
cantidadDisponible
```

---

### 3.3 Constantes

Usar `UPPER_SNAKE_CASE`.

```text
MONEDA_PRINCIPAL
ESCALA_CANTIDAD
ESCALA_MONETARIA
```

---

### 3.4 Archivos

Backend:

```text
Activo.java
ActivoServicio.java
ActivoControlador.java
```

Frontend:

```text
activo.model.ts
activo.service.ts
activo-form.component.ts
activo-lista.component.ts
```

---

## 4. Backend

### 4.1 Organización por funcionalidad

El backend se organizará por funcionalidad.

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

Cada funcionalidad podrá contener:

```text
controlador/
servicio/
repositorio/
modelo/
dto/
```

No se crearán capas adicionales sin una necesidad concreta.

---

## 5. Controladores

Los controladores deben encargarse únicamente de:

- recibir la solicitud;
- validar datos de entrada;
- delegar al servicio;
- devolver la respuesta HTTP.

No deben contener cálculos financieros ni lógica de negocio.

Ejemplo conceptual:

```java
@PostMapping
public ResponseEntity<ActivoRespuesta> crear(
        @Valid @RequestBody CrearActivoSolicitud solicitud) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(activoServicio.crear(solicitud));
}
```

---

## 6. Servicios

Los servicios contienen la lógica de negocio.

Ejemplos:

```text
registrarAporte()
registrarTransaccion()
calcularPosiciones()
calcularEfectivoDisponible()
obtenerResumenPortafolio()
```

Los cálculos financieros deben realizarse en backend.

El frontend no debe convertirse en la fuente de verdad de cálculos financieros.

---

## 7. Repositorios

Los repositorios se utilizarán exclusivamente para acceso a datos.

No deben contener lógica de presentación ni reglas de negocio.

Usar Spring Data JPA cuando sea suficiente.

Las consultas personalizadas deben tener nombres claros y relacionados con el dominio.

Ejemplo:

```java
findByActivoIdOrderByFechaDesc(...)
```

---

## 8. Entidades y DTO

Las entidades JPA representan persistencia.

No deben exponerse directamente en la API.

Se utilizarán DTO para entrada y salida.

Ejemplo:

```text
CrearActivoSolicitud
ActualizarActivoSolicitud
ActivoRespuesta
```

Esto permite desacoplar:

```text
base de datos
≠
contrato API
```

---

## 9. Tipos financieros

### 9.1 Dinero

Usar:

```java
BigDecimal
```

Nunca utilizar:

```java
double
float
```

para cálculos monetarios.

---

### 9.2 Cantidades de activos

También utilizar `BigDecimal`.

Esto es especialmente importante para activos fraccionarios y criptomonedas.

---

### 9.3 Fechas

Para fechas de negocio:

```java
LocalDate
```

Para timestamps técnicos:

```java
OffsetDateTime
```

o el tipo equivalente elegido en la configuración definitiva.

---

## 10. Base de datos

### 10.1 PostgreSQL

La base de datos principal será PostgreSQL.

---

### 10.2 Nombres

Usar `snake_case`.

Ejemplos:

```text
precio_activo
fecha_registro
monto_neto
costo_fondeo
```

---

### 10.3 Claves primarias

Usar:

```text
id
```

como nombre de clave primaria cuando no exista una razón clara para utilizar otro nombre.

---

### 10.4 Estados

Usar una columna `estado` solo cuando una tabla realmente necesite
representar un estado funcional. En el MVP ninguna tabla lo utiliza.

Si se necesita, evitar booleanos ambiguos como:
abierto = true

Preferir estados explícitos:
estado = 'ABIERTO'
estado = 'CERRADO'

---

## 11. Migraciones Flyway

Flyway será la única herramienta responsable de crear y modificar el esquema.

Hibernate solo validará el esquema.

Configuración esperada:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Nunca utilizar en el proyecto:

```properties
spring.jpa.hibernate.ddl-auto=update
```

---

### 11.1 Nombre de migraciones

Formato:

```text
V1__crear_tabla_activo.sql
V2__crear_tabla_aporte.sql
V3__crear_tabla_transaccion.sql
```

Usar:

```text
V<numero>__<descripcion>.sql
```

---

### 11.2 Migraciones ejecutadas

Una migración que ya fue ejecutada no debe modificarse.

Si se necesita cambiar el esquema, crear una nueva migración.

Ejemplo:

```text
V8__agregar_columna_fuente_precio.sql
```

---

## 12. API REST

La API tendrá como prefijo:

```text
/api/v1
```

Ejemplos:

```text
GET  /api/v1/activos
POST /api/v1/aportes
GET  /api/v1/portafolio/resumen
```

---

### 12.1 Recursos

Los endpoints deben representar recursos y no acciones.

Preferir:

```text
POST /api/v1/aportes
```

Evitar:

```text
POST /api/v1/registrarAporte
```

---

### 12.2 JSON

Usar `camelCase`.

Ejemplo:

```json
{
  "montoAportado": 200.0,
  "montoNetoBroker": 195.58,
  "costoFondeo": 4.42
}
```

---

## 13. Errores de API

Los errores seguirán una estructura común.

Ejemplo:

```json
{
  "fechaHora": "2026-10-23T10:00:00-05:00",
  "estadoHttp": 400,
  "codigo": "DATOS_INVALIDOS",
  "mensaje": "La solicitud contiene datos inválidos.",
  "ruta": "/api/v1/aportes",
  "erroresCampos": [
    {
      "campo": "montoNetoBroker",
      "mensaje": "No puede ser mayor al monto aportado."
    }
  ]
}
```

La estructura completa está definida en `documentacion/04-contratos-api.md` §7.

Los mensajes técnicos internos no deben exponerse directamente al frontend.

---

## 14. Angular

### 14.1 Organización

El frontend se organizará por funcionalidad.

Ejemplo:

```text
src/app/
├── activos/
├── aportes/
├── transacciones/
├── referidos/
├── dividendos/
├── precios/
├── portafolio/
├── ayuda/
└── compartido/
```

---

### 14.2 Componentes

Usar componentes standalone.

Los componentes deben tener una responsabilidad clara.

Evitar componentes excesivamente grandes que mezclen:

- obtención de datos;
- cálculos;
- formularios;
- tablas;
- gráficos;
- navegación.

---

### 14.3 Servicios

Los servicios Angular se utilizarán para:

- comunicación HTTP;
- estado compartido cuando sea necesario;
- abstracciones reutilizables.

La lógica financiera principal seguirá perteneciendo al backend.

---

### 14.4 Formularios

Usar Reactive Forms.

Las validaciones visuales del frontend mejoran la experiencia del usuario, pero no sustituyen las validaciones del backend.

---

### 14.5 Signals

Se podrán utilizar Signals para estado local y derivado cuando simplifiquen el componente.

No utilizar Signals únicamente por moda si una variable normal es suficiente.

---

## 15. CSS y diseño

La implementación debe tomar como referencia:

```text
documentacion/05-guia-interfaz.md
```

y:

```text
referencia/prototipo-interfaz/Prototipo Interfaz v2.dc.html
```

El prototipo es una referencia visual aprobada, no una restricción absoluta.

Durante la implementación pueden realizarse ajustes cuando:

- mejoren usabilidad;
- mejoren accesibilidad;
- mejoren responsive;
- solucionen limitaciones técnicas;
- mantengan coherencia con el sistema visual.

Si un cambio modifica una regla visual global, debe actualizarse la documentación correspondiente.

---

## 16. Pruebas

Las reglas financieras relevantes deben tener pruebas automatizadas.

Especialmente:

- cálculo de costo de fondeo;
- efectivo disponible;
- cantidades compradas y vendidas;
- referidos;
- costo promedio;
- valor actual;
- ganancia o pérdida;
- resumen del portafolio;
- fotografías históricas.

Cuando sea posible, los resultados se compararán con el Excel de referencia:

```text
referencia/registro_inversiones_v4_1.xlsx
```

---

## 17. Git

### 17.1 Ramas

Para este proyecto personal se mantendrá un flujo simple.

La rama principal será:

```text
main
```

Para funcionalidades suficientemente grandes podrán utilizarse ramas:

```text
feature/activos
feature/aportes
feature/transacciones
```

No es obligatorio crear una rama para cambios pequeños.

---

### 17.2 Commits

Usar mensajes claros y cortos.

Formato recomendado:

```text
tipo: descripcion
```

Ejemplos:

```text
feat: implementar registro de activos
fix: corregir calculo de efectivo disponible
docs: actualizar contratos de api
test: agregar pruebas de calculo de posiciones
refactor: simplificar servicio de portafolio
chore: configurar flyway
```

Tipos principales:

```text
feat
fix
docs
test
refactor
chore
```

---

## 18. Especificaciones

Antes de implementar una funcionalidad importante se debe crear o aprobar su especificación.

Flujo esperado:

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

Claude Code no debe inventar reglas funcionales que no estén documentadas.

Si una regla necesaria no está especificada, debe detener esa decisión y solicitar definición.

---

## 19. Documentación viva

La documentación forma parte del proyecto.

Cuando una decisión implementada cambie algo previamente documentado, se debe evaluar si corresponde actualizar alguno de estos archivos:

```text
documentacion/00-alcance-mvp.md
documentacion/01-modelo-funcional.md
documentacion/02-base-datos.md
documentacion/03-arquitectura.md
documentacion/04-contratos-api.md
documentacion/05-guia-interfaz.md
documentacion/06-convenciones.md
documentacion/07-hoja-ruta.md
CLAUDE.md
PRODUCT.md
DESIGN.md (cuando exista)
```

No todos los cambios requieren modificar documentación.

Se actualizará únicamente cuando exista un cambio real en:

- requerimientos;
- reglas funcionales;
- contratos;
- arquitectura;
- modelo de datos;
- sistema visual;
- convenciones;
- flujo de desarrollo.

---

## 20. Principio general

Priorizar:

```text
claridad
> simplicidad
> consistencia
> abstracción prematura
```

La arquitectura y el código deben crecer únicamente cuando exista una necesidad real del producto.
