# Hoja de ruta del proyecto

## 1. Objetivo

Esta hoja de ruta define el orden de construcción del MVP del Analizador de Inversiones Personales.

El desarrollo seguirá un enfoque incremental y guiado por especificaciones.

No se busca implementar todo el sistema al mismo tiempo.

Cada funcionalidad debe avanzar mediante:

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

---

## 2. Principios de ejecución

Durante el desarrollo se aplicarán los siguientes principios:

- implementar una funcionalidad a la vez;
- evitar arquitectura innecesaria;
- no implementar reglas funcionales no especificadas;
- mantener backend y frontend alineados con los contratos API;
- validar los cálculos financieros en backend;
- comparar resultados contra el Excel de referencia;
- mantener la documentación actualizada cuando una decisión cambie;
- realizar commits pequeños y coherentes;
- priorizar funcionamiento correcto antes que optimizaciones prematuras.

---

# FASE 0 — Definición del producto

## Estado

Completada.

## Entregables

- alcance del MVP;
- modelo funcional;
- modelo de base de datos;
- arquitectura;
- contratos iniciales API;
- guía de interfaz;
- prototipo visual;
- convenciones;
- hoja de ruta.

Documentos:

```text
documentacion/00-alcance-mvp.md
documentacion/01-modelo-funcional.md
documentacion/02-base-datos.md
documentacion/03-arquitectura.md
documentacion/04-contratos-api.md
documentacion/05-guia-interfaz.md
documentacion/06-convenciones.md
documentacion/07-hoja-ruta.md
```

Referencia funcional:

```text
referencia/registro_inversiones_v4_1.xlsx
```

Referencia visual:

```text
referencia/prototipo-interfaz/Prototipo Interfaz v2.dc.html
```

---

# FASE 1 — Preparación del proyecto

## Objetivo

Crear la base técnica mínima para empezar a implementar funcionalidades.

---

## 1. Backend base

Crear el proyecto Spring Boot con:

- Java 21;
- Maven;
- Spring Web;
- Spring Data JPA;
- Validation;
- PostgreSQL;
- Flyway.

Configurar:

```text
backend/
```

La aplicación deberá poder iniciar correctamente sin funcionalidad de negocio.

---

## 2. Base de datos local

Inicialmente PostgreSQL se ejecutará mediante Docker Compose.

El objetivo es poder levantar la base con:

```bash
docker compose up -d
```

La configuración deberá mantenerse simple.

No se dockerizará todavía backend ni frontend.

---

## 3. Flyway

Configurar Flyway antes de crear las tablas de negocio.

Hibernate deberá utilizar:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Nunca:

```properties
spring.jpa.hibernate.ddl-auto=update
```

La primera migración real se realizará junto con la funcionalidad Activos.

---

## 4. Frontend base

Crear:

```text
frontend/
```

con Angular.

Configuración esperada:

- standalone components;
- Angular Router;
- HttpClient;
- Reactive Forms;
- Signals cuando aporten valor.

Inicialmente deberá existir únicamente:

- estructura base;
- shell principal;
- navegación;
- configuración de ambientes;
- estilos globales.

No implementar todavía todas las pantallas.

---

## Resultado esperado de FASE 1

```text
PostgreSQL funcionando
+
Spring Boot conectado
+
Flyway configurado
+
Angular funcionando
+
estructura base preparada
```

---

# FASE 2 — Activos

## Objetivo

Implementar el catálogo base de instrumentos financieros.

Antes de implementar se deberá crear una especificación para Activos.

---

## Backend

Implementar:

- tabla `activo`;
- entidad;
- repositorio;
- servicio;
- controlador;
- DTO;
- validaciones;
- estados ACTIVO / INACTIVO.

Operaciones principales:

```text
crear activo
listar activos
consultar activo
actualizar activo
cambiar estado
```

---

## Frontend

Implementar pantalla:

```text
Activos
```

Debe permitir:

- consultar activos;
- registrar activo;
- editar datos permitidos;
- activar;
- desactivar;
- consultar asignación objetivo.

---

## Pruebas

Validar al menos:

- ticker obligatorio;
- nombre obligatorio;
- tipo válido;
- moneda válida;
- estado;
- duplicados según regla definida en la spec.

---

# FASE 3 — Aportes

## Objetivo

Registrar el dinero que entra desde el bolsillo del usuario hacia el intermediario.

---

## Backend

Implementar:

- tabla `aporte`;
- migración Flyway;
- registro;
- consulta;
- cálculo de costo de fondeo.

Regla principal:

```text
costoFondeo = montoAportado - montoNeto
```

El monto neto recibido será el que incremente el efectivo disponible.

---

## Frontend

Implementar:

```text
Aportes
```

Debe mostrar:

- historial;
- total aportado;
- total neto recibido;
- costos de fondeo;
- formulario de registro.

---

## Validación Excel

Comparar:

```text
total aportado
monto neto
costos de fondeo
```

contra:

```text
referencia/registro_inversiones_v4_1.xlsx
```

---

# FASE 4 — Transacciones

## Objetivo

Registrar compras y ventas de activos.

---

## Antes de implementar

La spec deberá definir explícitamente:

- reglas de compra;
- reglas de venta;
- validación de cantidad disponible;
- tratamiento de comisiones;
- efecto sobre efectivo;
- método de costo para ventas;
- estrategia de corrección de errores en registros.

No se implementará la venta hasta que estas reglas estén aprobadas.

---

## Backend

Implementar:

- tabla `transaccion`;
- compras;
- ventas;
- consulta del historial;
- validaciones correspondientes.

---

## Frontend

Implementar:

```text
Transacciones
```

Debe permitir:

- registrar compra;
- registrar venta;
- consultar historial;
- filtrar por tipo;
- filtrar por activo;
- visualizar monto y comisión.

---

# FASE 5 — Referidos

## Objetivo

Registrar activos recibidos sin costo de compra.

---

## Regla principal

Los referidos:

```text
aumentan la cantidad de la posición
```

pero:

```text
no aumentan el costo de compras
```

---

## Backend

Implementar:

- tabla `referido`;
- registro;
- consulta;
- cálculo del valor recibido;
- cálculo del valor actual.

---

## Frontend

Implementar:

```text
Referidos
```

Diferenciar claramente:

```text
datos históricos al recibir
```

de:

```text
datos calculados actualmente
```

---

# FASE 6 — Dividendos

## Objetivo

Registrar dividendos recibidos.

---

## Regla principal

Un dividendo recibido incrementa el efectivo disponible.

Si posteriormente se reinvierte:

```text
dividendo
+
transacción COMPRA independiente
```

No se debe descontar el dinero dos veces.

---

## Backend

Implementar:

- tabla `dividendo`;
- registro;
- retenciones;
- otros cargos;
- monto neto;
- consulta histórica.

---

## Frontend

Implementar:

```text
Dividendos
```

Debe diferenciar:

- bruto;
- retención;
- otros cargos;
- neto;
- estado de reinversión cuando aplique.

---

# FASE 7 — Precios

## Objetivo

Mantener el historial de precios utilizado para valorar los activos.

---

## Backend

Implementar:

- tabla `precio_activo`;
- registro manual;
- consulta histórica;
- obtención del último precio de cada activo.

Inicialmente no habrá integración automática con proveedores externos.

---

## Frontend

Implementar:

```text
Precios
```

Debe permitir:

- registrar precio;
- consultar último precio;
- consultar historial;
- visualizar variaciones.

---

# FASE 8 — Posiciones

## Objetivo

Calcular las posiciones actuales del portafolio.

No se creará una tabla `posicion`.

Las posiciones se calcularán a partir de:

```text
transacciones
+
referidos
+
precios
```

---

## Cálculos principales

Para cada activo:

```text
cantidadActual
costoPromedio
costoCompras
precioActual
valorActual
gananciaPerdida
rentabilidad
porcentajeActual
porcentajeObjetivo
diferenciaAsignacion
```

---

## Regla de cantidad

Conceptualmente:

```text
cantidad actual
=
cantidad comprada
-
cantidad vendida
+
cantidad recibida por referidos
```

---

## Backend

Implementar:

```text
GET /api/v1/portafolio/posiciones
```

La lógica financiera deberá estar completamente cubierta por pruebas.

---

## Frontend

Implementar:

```text
Posiciones
```

La pantalla será principalmente de consulta y análisis.

No se editarán posiciones manualmente.

---

# FASE 9 — Efectivo disponible

## Objetivo

Calcular cuánto dinero permanece disponible para invertir.

No se creará una tabla específica para efectivo.

---

## Fuentes

El efectivo se derivará de:

```text
aportes netos
+
ventas
+
dividendos netos
-
compras
-
comisiones correspondientes
```

La fórmula definitiva deberá estar documentada en la spec correspondiente.

---

## Pruebas

Este cálculo deberá tener pruebas específicas debido a su impacto sobre todo el portafolio.

---

# FASE 10 — Resumen del portafolio

## Objetivo

Construir la vista consolidada del estado actual.

No se creará una tabla `resumen_portafolio`.

---

## Backend

Implementar:

```text
GET /api/v1/portafolio/resumen
```

Debe integrar:

- total aportado;
- total neto recibido;
- costos de fondeo;
- comisiones;
- valor actual de posiciones;
- efectivo disponible;
- valor total del portafolio;
- resultado;
- rentabilidad;
- información de referidos.

---

## Regla importante

El valor actual de referidos forma parte de las posiciones.

Por lo tanto:

```text
valorTotalPortafolio
=
valorActualPosiciones
+
efectivoDisponible
```

No se debe volver a sumar `valorActualReferidos`.

---

## Frontend

Implementar:

```text
Resumen
```

Debe ser la pantalla principal del sistema.

---

# FASE 11 — Fotografías y rendimiento

## Objetivo

Guardar snapshots históricos del estado del portafolio.

---

## Backend

Implementar:

- tabla `fotografia_portafolio`;
- creación de fotografía;
- consulta histórica;
- datos necesarios para gráficos de evolución.

Las fotografías deberán ser históricas e inmutables.

---

## Antes de implementar

La spec debe definir:

- cuándo se crea una fotografía;
- si requiere confirmación;
- qué datos exactos se almacenan;
- cómo se comporta una fotografía si posteriormente cambian precios o movimientos.

---

## Frontend

Implementar:

```text
Rendimiento
```

Debe mostrar:

- evolución del valor;
- aportado acumulado;
- resultado;
- rentabilidad;
- fotografías históricas;
- eventos relevantes.

---

# FASE 12 — Ayuda

## Objetivo

Incorporar ayuda contextual para comprender los conceptos financieros y el funcionamiento de la aplicación.

---

## Frontend

Implementar:

```text
Ayuda
```

Puede utilizar contenido estático durante el MVP.

No requiere inicialmente backend.

---

# FASE 13 — Validación integral

## Objetivo

Comprobar que el sistema reproduce correctamente el comportamiento esperado.

---

## Comparación con Excel

Utilizar:

```text
referencia/registro_inversiones_v4_1.xlsx
```

Crear en la aplicación los mismos:

- activos;
- aportes;
- transacciones;
- referidos;
- dividendos;
- precios.

Comparar:

```text
posiciones
costo promedio
efectivo
valor actual
resultado
rentabilidad
resumen
```

Las diferencias deberán investigarse antes de considerar terminado el MVP.

---

# FASE 14 — Refinamiento final

Una vez que las reglas funcionales estén correctas:

- revisar UX real en Angular;
- realizar ajustes responsive;
- revisar accesibilidad;
- optimizar consultas únicamente si es necesario;
- eliminar código innecesario;
- revisar mensajes de error;
- revisar tests;
- actualizar documentación;
- ejecutar revisión final del MVP.

El prototipo visual puede ajustarse durante esta fase si la implementación real demuestra que una decisión puede mejorarse.

---

# FASE 15 — Cierre del MVP

El MVP se considerará terminado cuando:

- las funcionalidades definidas estén implementadas;
- los cálculos principales tengan pruebas;
- los resultados sean coherentes con el Excel;
- backend y frontend estén integrados;
- Flyway controle completamente el esquema;
- no existan errores funcionales críticos;
- la interfaz sea utilizable en escritorio y móvil;
- la documentación refleje el estado real del sistema.

---

# Después del MVP

Quedan fuera de esta primera versión, entre otras posibles mejoras:

- autenticación;
- múltiples usuarios;
- múltiples portafolios;
- integración automática con brokers;
- precios automáticos;
- importación de movimientos;
- análisis fundamental;
- recomendaciones de inversión;
- alertas;
- notificaciones;
- aplicaciones móviles nativas.

Estas funcionalidades deberán evaluarse posteriormente y no deben incorporarse al MVP sin actualizar primero el alcance.
