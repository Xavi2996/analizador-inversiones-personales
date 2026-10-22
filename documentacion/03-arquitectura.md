# Arquitectura

## 1. Objetivo

Este documento define la arquitectura inicial del MVP de portafolio.

La arquitectura debe priorizar:

- simplicidad,
- claridad,
- mantenibilidad,
- facilidad de aprendizaje,
- facilidad de pruebas,
- evolución progresiva.

No se introducirán tecnologías o patrones complejos sin una necesidad real.

---

# 2. Enfoque general

El proyecto se desarrollará como una aplicación web compuesta por:

- frontend,
- backend,
- base de datos.

Flujo general:

Usuario
↓
Angular
↓
API REST
↓
Spring Boot
↓
PostgreSQL

---

# 3. Estructura del repositorio

El proyecto utilizará un único repositorio.

Estructura general:

analizador-inversiones-personales/
│
├── backend/
├── frontend/
├── documentacion/
├── specs/
├── referencia/
├── .claude/
├── CLAUDE.md
├── README.md
├── .gitignore
└── docker-compose.yml

---

# 4. Motivo de utilizar un único repositorio

Frontend, backend, documentación, especificaciones y archivos de referencia forman parte del mismo producto.

Mantenerlos juntos facilita:

- versionado,
- revisión de cambios,
- trabajo con Claude Code,
- validación contra el Excel,
- coordinación entre frontend y backend,
- mantenimiento de documentación.

No se crearán repositorios separados para frontend y backend durante el MVP.

---

# 5. Enfoque guiado por especificaciones

El proyecto utilizará un enfoque guiado por especificaciones.

Antes de implementar una funcionalidad importante se deberá:

1. definir su comportamiento,
2. crear su especificación,
3. revisar la especificación,
4. aprobarla,
5. implementar,
6. probar,
7. validar contra las reglas del proyecto.

Las especificaciones se almacenarán en:

specs/

Ejemplo futuro:

specs/
├── 001-activos.md
├── 002-aportes.md
├── 003-transacciones.md
└── ...

---

# 6. Documentación y especificaciones

Se diferenciarán dos tipos de documentación.

## 6.1 Documentación general

Se almacenará en:

documentacion/

Define:

- alcance,
- modelo funcional,
- base de datos,
- arquitectura,
- contratos API,
- interfaz,
- convenciones,
- hoja de ruta.

---

## 6.2 Especificaciones

Se almacenarán en:

specs/

Definen funcionalidades concretas.

Ejemplo:

Una especificación de aportes podrá incluir:

- objetivo,
- reglas,
- validaciones,
- datos,
- API,
- interfaz,
- criterios de aceptación,
- pruebas esperadas.

---

# 7. Excel de referencia

El archivo:

referencia/registro_inversiones_v4_1.xlsx

será utilizado como referencia funcional y numérica.

Permitirá:

- validar cálculos,
- comprobar posiciones,
- comprobar efectivo,
- comprobar resumen,
- comprobar referidos,
- construir casos de prueba.

El Excel no forma parte de la ejecución de la aplicación.

Es únicamente una referencia del comportamiento esperado.

---

# 8. Backend

El backend utilizará:

- Java 21,
- Spring Boot,
- Spring Web,
- Spring Data JPA,
- Validation,
- PostgreSQL,
- Flyway,
- Maven.

El backend será una sola aplicación.

No se utilizarán microservicios en el MVP.

---

# 9. Organización del backend

El código se organizará principalmente por funcionalidad.

Ejemplo conceptual:

com.personal.inversiones
│
├── activos
├── aportes
├── transacciones
├── referidos
├── dividendos
├── precios
├── portafolio
└── compartido

Cada funcionalidad podrá contener los elementos técnicos que necesite.

Ejemplo:

aportes/
├── controlador/
├── servicio/
├── repositorio/
├── modelo/
└── dto/

Esta estructura podrá ajustarse durante la implementación si aparece una necesidad real.

---

# 10. Decisión sobre patrones arquitectónicos

No se aplicará DDD como metodología formal.

No se utilizará inicialmente:

- arquitectura hexagonal estricta,
- CQRS,
- Event Sourcing,
- microservicios,
- arquitectura limpia aplicada de forma rígida.

Se priorizará una arquitectura sencilla y organizada por funcionalidad.

Si durante el crecimiento del proyecto aparece una necesidad clara, se podrá evolucionar.

---

# 11. Capas del backend

Aunque la estructura principal sea por funcionalidad, se mantendrá una separación clara de responsabilidades.

## 11.1 Controlador

Responsable de:

- recibir solicitudes HTTP,
- validar estructura básica,
- llamar al servicio correspondiente,
- devolver respuestas.

No deberá contener cálculos financieros importantes.

---

## 11.2 Servicio

Responsable de:

- coordinar reglas funcionales,
- ejecutar casos de uso,
- realizar cálculos,
- interactuar con repositorios.

Ejemplos:

- registrar aporte,
- registrar transacción,
- calcular posición,
- calcular resumen.

---

## 11.3 Repositorio

Responsable de:

- acceso a datos,
- consultas,
- persistencia.

Se utilizará Spring Data JPA.

---

## 11.4 Modelo

Representará la información persistente y los objetos utilizados por la funcionalidad.

---

## 11.5 DTO

Los DTO se utilizarán para comunicación con la API.

No se expondrán directamente entidades JPA.

---

# 12. Flujo de una solicitud

Ejemplo:

Angular
↓
POST /api/v1/aportes
↓
Controlador de aportes
↓
Servicio de aportes
↓
Repositorio de aportes
↓
PostgreSQL

Respuesta:

PostgreSQL
↓
Repositorio
↓
Servicio
↓
DTO de respuesta
↓
Controlador
↓
Angular

---

# 13. Reglas de negocio

Las reglas financieras deberán residir principalmente en el backend.

Ejemplos:

- cálculo de costo de fondeo,
- cálculo de cantidad actual,
- cálculo de efectivo,
- cálculo de rentabilidad,
- validación de una venta,
- cálculo de resumen.

El frontend no será la fuente oficial de estos cálculos.

---

# 14. Frontend

El frontend utilizará:

- Angular,
- TypeScript,
- componentes standalone,
- Reactive Forms,
- Signals,
- HttpClient,
- Router.

---

# 15. Organización del frontend

El frontend se organizará por funcionalidad.

Estructura conceptual:

src/app/
│
├── activos/
├── aportes/
├── transacciones/
├── referidos/
├── dividendos/
├── precios/
├── portafolio/
├── compartido/
└── nucleo/

---

# 16. Responsabilidad del frontend

El frontend será responsable de:

- formularios,
- navegación,
- presentación de datos,
- experiencia de usuario,
- validaciones de interfaz,
- mensajes,
- ayuda contextual,
- gráficos.

No deberá ser la fuente oficial para cálculos financieros críticos.

---

# 17. Estado en Angular

Se utilizarán principalmente:

- Signals,
- propiedades calculadas,
- Reactive Forms.

RxJS continuará utilizándose donde sea natural, especialmente con HttpClient.

No se incorporará una solución compleja de estado global durante el MVP.

---

# 18. Base de datos

La base de datos será PostgreSQL.

Contendrá:

- activos,
- aportes,
- transacciones,
- referidos,
- dividendos,
- precios,
- asignaciones objetivo,
- fotografías del portafolio.

No almacenará directamente:

- posiciones,
- resumen,
- efectivo calculado.

Estos valores serán derivados.

---

# 19. Flyway

Flyway será responsable de evolucionar la estructura de PostgreSQL.

Flujo:

Spring Boot inicia
↓
Flyway busca migraciones
↓
Flyway ejecuta migraciones pendientes
↓
PostgreSQL queda actualizado
↓
Hibernate valida el esquema

Las migraciones estarán en:

backend/src/main/resources/db/migration/

---

# 20. Hibernate y JPA

Spring Data JPA se utilizará para persistencia.

Hibernate no modificará automáticamente las tablas.

Configuración esperada:

spring.jpa.hibernate.ddl-auto=validate

Responsabilidades:

Flyway
→ estructura de base de datos

JPA/Hibernate
→ persistencia y validación del mapeo

---

# 21. Docker

Docker Compose se utilizará inicialmente para PostgreSQL.

Flujo de desarrollo:

docker-compose.yml
↓
PostgreSQL
↓
Spring Boot local
↓
Angular local

Posteriormente podrá evaluarse dockerizar backend y frontend.

No es necesario hacerlo durante las primeras funcionalidades.

---

# 22. Ambientes

Inicialmente se considerarán:

- desarrollo,
- pruebas,
- producción futura.

El MVP comenzará únicamente con el ambiente local de desarrollo.

---

# 23. Configuración

Los valores que puedan variar según el ambiente no deberán escribirse directamente en el código.

Ejemplos:

- URL de PostgreSQL,
- usuario,
- contraseña,
- puerto,
- configuración CORS.

Se utilizarán:

- archivos de configuración de Spring,
- variables de entorno.

Los secretos no deberán versionarse en Git.

---

# 24. API

El frontend se comunicará con el backend mediante REST.

Prefijo previsto:

/api/v1

Ejemplos conceptuales:

/api/v1/activos

/api/v1/aportes

/api/v1/transacciones

/api/v1/portafolio/posiciones

/api/v1/portafolio/resumen

Los contratos exactos se definirán en:

documentacion/04-contratos-api.md

---

# 25. Cálculos del portafolio

Los cálculos principales serán responsabilidad del backend.

Ejemplos:

cantidad actual

costo promedio

valor actual

efectivo disponible

resultado frente al bolsillo

rentabilidad

peso actual

diferencia contra objetivo

---

# 26. Posiciones

No se almacenarán como tabla.

El backend las calculará a partir de:

- transacciones,
- referidos,
- precios,
- asignaciones objetivo.

---

# 27. Resumen

No se almacenará como tabla.

El backend lo calculará utilizando:

- aportes,
- transacciones,
- referidos,
- dividendos,
- precios.

---

# 28. Rendimiento histórico

Las fotografías del portafolio sí se almacenarán.

Esto permite conservar un estado histórico aunque los precios cambien posteriormente.

---

# 29. Precisión financiera

En Java se utilizará:

BigDecimal

para:

- dinero,
- precios,
- cantidades,
- comisiones,
- porcentajes cuando sea necesario.

No se utilizará:

float

ni:

double

para cálculos financieros oficiales.

---

# 30. Fechas

Las fechas de negocio utilizarán:

LocalDate

Ejemplos:

- fecha de aporte,
- fecha de compra,
- fecha de referido,
- fecha de precio.

Las fechas técnicas utilizarán tipos adecuados de fecha y hora.

---

# 31. Validaciones

Las validaciones podrán existir en diferentes niveles.

## Interfaz

Para mejorar experiencia de usuario.

## Backend

Para proteger reglas funcionales.

## Base de datos

Para proteger integridad.

Ejemplo:

cantidad > 0

podrá validarse en los tres niveles cuando sea conveniente.

---

# 32. Manejo de errores

El backend deberá utilizar una estructura común de errores.

No se devolverán errores diferentes en cada controlador.

Se implementará un manejador global.

Los detalles se definirán en:

documentacion/04-contratos-api.md

---

# 33. Pruebas

Se dará prioridad a las pruebas de:

- cálculos financieros,
- reglas funcionales,
- servicios,
- endpoints principales.

El Excel de referencia permitirá construir casos de prueba conocidos.

---

# 34. Interfaz visual

La interfaz tendrá reglas visuales propias.

Se definirán en:

documentacion/05-guia-interfaz.md

Ahí se especificarán:

- colores,
- tipografía,
- tarjetas,
- tablas,
- formularios,
- estados,
- gráficos,
- responsive,
- ayuda contextual.

---

# 35. Claude Code

Claude Code se utilizará como asistente de implementación.

Deberá:

- consultar CLAUDE.md,
- consultar documentación relevante,
- consultar especificaciones,
- respetar contratos aprobados,
- implementar únicamente el alcance solicitado.

No deberá cambiar decisiones importantes sin aprobación.

---

# 36. Dependencias

No se agregarán librerías sin una necesidad clara.

Antes de añadir una dependencia deberá evaluarse:

- qué problema resuelve,
- si el framework ya proporciona una solución,
- mantenimiento,
- complejidad añadida.

---

# 37. Seguridad inicial

El MVP local no tendrá autenticación inicialmente.

Sin embargo:

- no se versionarán secretos,
- las credenciales irán mediante configuración,
- la arquitectura no deberá impedir añadir autenticación posteriormente.

---

# 38. Evolución futura

El diseño deberá permitir considerar posteriormente:

- múltiples usuarios,
- múltiples portafolios,
- autenticación,
- fuentes automáticas de precios,
- importación desde intermediarios,
- análisis fundamental,
- notificaciones.

No se implementarán anticipadamente.

---

# 39. Decisión arquitectónica principal

Para el MVP se utilizará:

- un único repositorio,
- Angular,
- Spring Boot,
- REST,
- PostgreSQL,
- Flyway,
- JPA,
- Docker Compose,
- organización por funcionalidad,
- desarrollo guiado por especificaciones.

La prioridad será:

simplicidad

> claridad
>
> correctitud
>
> mantenibilidad
>
> complejidad técnica
