# Guía de Interfaz

## 1. Objetivo

Este documento define las reglas visuales y de experiencia de usuario del frontend.

La interfaz debe ser:

- oscura,
- moderna,
- sobria,
- fácil de leer,
- coherente con una aplicación financiera,
- útil tanto en escritorio como en dispositivos móviles.

La prioridad será mostrar información financiera de forma comprensible y evitar ruido visual innecesario.

Este documento define las reglas generales de diseño.

La representación concreta de estas reglas se construirá posteriormente mediante un prototipo visual estático.

---

# 2. Principios visuales

La interfaz deberá seguir estos principios:

1. La información importante debe ser fácil de encontrar.
2. Los datos financieros deben ser legibles.
3. Los valores calculados deben diferenciarse de los datos editables.
4. Los estados positivos y negativos deben ser claros.
5. No se utilizará únicamente color para comunicar información.
6. Las pantallas deben mantener una estructura consistente.
7. Los formularios deben ser simples.
8. Las tablas deben priorizar legibilidad.
9. La ayuda debe estar disponible sin interrumpir el flujo del usuario.
10. El diseño debe evitar sobrecargar la pantalla.
11. El tema oscuro debe mantener suficiente contraste.
12. Los colores intensos deberán utilizarse principalmente para estados, acciones e indicadores.

---

# 3. Estilo general

El estilo visual principal será oscuro, moderno, minimalista y orientado a una aplicación financiera.

Características:

- fondo general oscuro,
- superficies y tarjetas ligeramente más claras que el fondo,
- buen espacio entre elementos,
- bordes suaves,
- sombras discretas,
- tipografía clara y de alto contraste,
- uso controlado de colores,
- énfasis visual en indicadores financieros,
- sensación moderna y profesional.

La interfaz deberá recordar visualmente a una aplicación moderna de seguimiento de inversiones, sin copiar directamente el diseño de ninguna plataforma existente.

No se utilizarán:

- colores excesivamente saturados en grandes superficies,
- degradados excesivos,
- animaciones innecesarias,
- demasiados colores simultáneamente,
- elementos decorativos sin función,
- negro puro como fondo general cuando perjudique la jerarquía visual.

---

# 4. Paleta de colores

La interfaz utilizará una paleta oscura.

Los colores definidos son una referencia inicial.

Durante la creación del prototipo visual podrán realizarse pequeños ajustes si mejoran:

- contraste,
- legibilidad,
- consistencia,
- accesibilidad.

Los cambios importantes deberán quedar documentados.

---

## 4.1 Fondo principal

El fondo general será un tono oscuro azulado o gris oscuro.

Referencia inicial:

#0B1120

No se recomienda utilizar negro puro como fondo principal.

---

## 4.2 Fondo secundario

Se utilizará para:

- navegación lateral,
- encabezados,
- secciones secundarias.

Referencia inicial:

#111827

---

## 4.3 Fondo de tarjetas

Las tarjetas deberán diferenciarse ligeramente del fondo principal.

Referencia inicial:

#172033

---

## 4.4 Fondo elevado

Podrá utilizarse para:

- diálogos,
- menús,
- elementos emergentes,
- controles destacados.

Referencia inicial:

#1F2937

---

## 4.5 Bordes

Los bordes deberán ser discretos.

Referencia inicial:

#2D3748

---

## 4.6 Color principal

Se utilizará azul como color principal de interacción.

Uso:

- botones principales,
- enlaces,
- elementos seleccionados,
- navegación activa,
- indicadores interactivos.

Referencia inicial:

#3B82F6

---

## 4.7 Color principal destacado

Podrá utilizarse una variante más clara para:

- estados activos,
- enfoque,
- elementos seleccionados.

Referencia inicial:

#60A5FA

---

## 4.8 Texto principal

El texto principal deberá tener alto contraste.

Referencia inicial:

#F1F5F9

---

## 4.9 Texto secundario

Referencia inicial:

#94A3B8

---

## 4.10 Texto menos relevante

Valor aprobado:

#7C8AA0

Se utilizará únicamente para información secundaria.

La referencia inicial era #64748B. Se reemplazó por el valor del prototipo aprobado porque sobre el fondo principal (#0B1120) #64748B no alcanza el contraste mínimo de 4.5:1 para texto pequeño, y #7C8AA0 sí lo supera.

---

## 4.11 Estado positivo

Verde.

Uso:

- ganancias,
- rentabilidad positiva,
- indicadores favorables.

Referencia inicial:

#22C55E

---

## 4.12 Estado negativo

Rojo.

Uso:

- pérdidas,
- errores,
- indicadores negativos.

Referencia inicial:

#EF4444

---

## 4.13 Precaución

Ámbar.

Uso:

- advertencias,
- asignaciones alejadas del objetivo,
- información que requiere atención.

Referencia inicial:

#F59E0B

---

## 4.14 Información adicional

Azul claro.

Referencia inicial:

#38BDF8

Podrá utilizarse para:

- ayuda,
- información adicional,
- ciertos gráficos.

---

## 4.15 Uso de colores

Los colores positivos y negativos deberán destacar sobre el fondo oscuro sin resultar agresivos.

Las grandes superficies de la aplicación deberán permanecer en tonos neutros y oscuros.

Los colores intensos se reservarán principalmente para:

- estados,
- acciones,
- gráficos,
- indicadores,
- elementos seleccionados.

---

# 5. Regla de color

El color nunca será el único indicador.

Ejemplo incorrecto:

mostrar una pérdida únicamente en rojo.

Ejemplo correcto:

↓ -2.99%

utilizando:

- color rojo,
- símbolo,
- valor numérico.

---

# 6. Tipografía

Se utilizará una tipografía sans-serif moderna y legible.

Tipografía aprobada en el prototipo:

- Hanken Grotesk como tipografía principal;
- Geist Mono para tickers y valores que requieren precisión visual.

No se cargarán familias tipográficas adicionales a estas dos.

---

# 7. Jerarquía de texto

## 7.1 Título principal

Uso:

nombre de pantalla.

Ejemplo:

Resumen del portafolio

---

## 7.2 Título de sección

Uso:

bloques dentro de una pantalla.

Ejemplo:

Distribución actual

---

## 7.3 Etiqueta

Uso:

- formularios,
- indicadores,
- filtros.

Ejemplo:

Total aportado

---

## 7.4 Valor destacado

Uso:

indicadores principales.

Ejemplo:

$557.81

---

## 7.5 Texto secundario

Uso:

- explicaciones,
- fechas,
- información complementaria.

---

# 8. Navegación principal

La aplicación tendrá navegación lateral en escritorio.

Opciones previstas:

- Resumen
- Activos
- Aportes
- Transacciones
- Referidos
- Dividendos
- Precios
- Posiciones
- Rendimiento
- Ayuda

La opción seleccionada deberá diferenciarse visualmente.

La distribución exacta se validará mediante el prototipo visual.

---

# 9. Navegación móvil

En dispositivos pequeños la navegación lateral podrá convertirse en:

- menú lateral oculto,
- menú desplegable,
- barra superior.

La navegación debe seguir siendo sencilla y fácil de utilizar.

El comportamiento concreto deberá probarse durante el prototipo.

---

# 10. Pantalla de resumen

La pantalla principal será:

Resumen

Debe permitir conocer rápidamente el estado del portafolio.

Debe priorizar los datos más importantes antes que los detalles.

---

# 11. Indicadores principales

Se utilizarán tarjetas para mostrar indicadores.

Ejemplos:

Total aportado

$575.00

---

Valor actual

$557.81

---

Resultado

-$17.19

---

Rentabilidad

-2.99%

---

Efectivo

$55.04

---

# 12. Tarjetas

Las tarjetas deberán utilizar una superficie ligeramente más clara que el fondo principal.

Deberán tener:

- fondo oscuro elevado,
- bordes discretos,
- separación clara,
- título secundario,
- valor principal destacado,
- información secundaria opcional,
- contraste suficiente.

Las tarjetas principales del resumen podrán utilizar pequeños acentos de color para identificar el tipo de indicador.

No deberán utilizar grandes superficies verdes o rojas para representar ganancias o pérdidas.

El color deberá concentrarse principalmente en:

- valor,
- porcentaje,
- icono,
- indicador visual.

---

# 13. Valores monetarios

Los valores monetarios deberán mostrar el símbolo de moneda cuando corresponda.

Ejemplo:

$575.00

Los valores grandes deberán utilizar separadores.

Ejemplo:

$12,540.75

La aplicación trabajará inicialmente principalmente con USD.

---

# 14. Porcentajes

Los porcentajes deberán mostrar el símbolo:

%

Ejemplo:

85.00%

Para valores de rendimiento:

+3.25%

-2.99%

---

# 15. Valores negativos

Los valores negativos deberán mostrar:

- signo negativo,
- color,
- indicador visual cuando sea útil.

Ejemplo:

↓ -$17.19

---

# 16. Valores positivos

Los valores positivos podrán mostrarse como:

↑ +$25.30

---

# 17. Tablas

Las tablas serán uno de los principales componentes de la aplicación.

Se utilizarán en:

- activos,
- aportes,
- transacciones,
- referidos,
- dividendos,
- precios,
- posiciones,
- rendimiento.

---

# 18. Diseño de tablas

Las tablas deberán:

- tener encabezados claros,
- utilizar fondos compatibles con el tema oscuro,
- permitir lectura horizontal sencilla,
- alinear números a la derecha,
- alinear texto a la izquierda,
- mantener fechas en un formato consistente,
- evitar columnas innecesarias,
- mantener una altura de filas cómoda,
- diferenciar filas y encabezados sin depender únicamente de bordes.

---

# 19. Acciones en tablas

Las acciones se mostrarán al final de cada fila cuando sea necesario.

Ejemplos:

- Ver
- Editar

---

# 20. Tablas responsivas

En pantallas pequeñas las tablas podrán:

- desplazarse horizontalmente,
- ocultar columnas secundarias,
- transformarse en tarjetas cuando resulte más legible.

La decisión concreta se realizará después de validar el prototipo.

---

# 21. Formularios

Los formularios deberán tener:

- etiquetas visibles,
- campos claramente separados,
- mensajes de error debajo del campo,
- botones consistentes,
- separación visual adecuada.

Los campos deberán diferenciarse claramente del fondo general.

---

# 22. Campos obligatorios

Los campos obligatorios deberán indicarse claramente.

Ejemplo:

Monto aportado \*

No se deberá depender únicamente del color.

---

# 23. Campos numéricos

Los campos de dinero deberán indicar su unidad cuando sea útil.

Ejemplo:

Monto aportado

[ 250.00 ] USD

---

# 24. Campos calculados

Los datos calculados no deberán parecer editables.

Ejemplo:

Costo de fondeo

$4.42

Calculado automáticamente.

Podrán mostrarse mediante:

- texto,
- campo visualmente deshabilitado,
- bloque informativo.

Se deberá evitar confundirlos con datos que el usuario debe ingresar.

---

# 25. Formularios de transacciones

Una transacción deberá permitir elegir:

- activo,
- tipo de transacción,
- fecha,
- cantidad,
- precio,
- comisión,
- comentario.

El formulario deberá mostrar el monto bruto calculado antes de guardar.

Ejemplo:

Cantidad:

0.10

Precio:

$770.00

Monto aproximado:

$77.00

---

# 26. Formularios de referidos

El formulario deberá diferenciar claramente:

Datos históricos:

- cantidad,
- precio al recibir,
- valor recibido.

Datos actuales:

- precio actual,
- valor actual.

Los datos actuales no deberán ingresarse manualmente dentro del referido.

---

# 27. Estados

Los activos no tienen estado ni se activan o desactivan. Todos los activos registrados aparecen en el catálogo y en los selectores. Un activo con cantidad actual cero no aparece en Posiciones.

# 28. Posiciones

La pantalla Posiciones debe ser principalmente de consulta.

No permitirá modificar cantidades manualmente.

Debe mostrar:

- símbolo,
- cantidad actual,
- costo promedio,
- precio actual,
- valor actual,
- ganancia o pérdida,
- rentabilidad,
- porcentaje actual,
- porcentaje objetivo,
- diferencia contra objetivo.

---

# 29. Distribución del portafolio

La pantalla de resumen podrá incluir una visualización de distribución.

Ejemplo:

IVV 85%

VXUS 10%

BTC 5%

La visualización podrá utilizar:

- gráfico de dona,
- gráfico de barras.

La elección definitiva se validará visualmente en el prototipo.

---

# 30. Asignación objetivo

Se deberá diferenciar claramente:

Porcentaje actual

de:

Porcentaje objetivo

Ejemplo:

IVV

Actual:

82%

Objetivo:

85%

Diferencia:

-3%

---

# 31. Indicadores de asignación

Estados visuales posibles:

- Dentro del objetivo
- Cerca del objetivo
- Fuera del objetivo

Las reglas exactas podrán definirse posteriormente en la especificación correspondiente.

---

# 32. Rendimiento

La pantalla Rendimiento deberá permitir visualizar la evolución histórica.

Como mínimo podrá mostrar:

- total aportado,
- valor del portafolio,
- resultado,
- rentabilidad.

---

# 33. Gráficos

Los gráficos deberán:

- tener títulos claros,
- evitar información innecesaria,
- mostrar unidades,
- permitir interpretar tendencias,
- mantener consistencia visual con el tema oscuro,
- utilizar colores con buen contraste.

No se utilizarán gráficos únicamente por motivos decorativos.

---

# 34. Gráfico principal de rendimiento

Un gráfico importante podrá comparar:

Aportado acumulado

contra:

Valor del portafolio

Esto permitirá diferenciar crecimiento por nuevos aportes de crecimiento por rendimiento.

---

# 35. Estados de carga

Cuando se consulte información al backend se deberá mostrar un estado de carga.

Ejemplo:

Cargando...

También podrán utilizarse indicadores visuales de carga.

No se deberá dejar la pantalla sin respuesta visual.

---

# 36. Estado vacío

Cuando una sección no tenga información deberá mostrarse un mensaje claro.

Ejemplo:

No existen aportes registrados.

Y una acción:

Registrar primer aporte

---

# 37. Errores

Los errores deberán expresarse con lenguaje comprensible.

Ejemplo:

No fue posible registrar el aporte.

En lugar de:

Error 500.

Cuando sea posible se deberá mostrar una orientación para corregir el problema.

---

# 38. Confirmaciones

Las acciones importantes podrán mostrar confirmación.

---

# 39. Mensajes de éxito

Ejemplos:

Activo registrado correctamente.

Aporte registrado correctamente.

Precio registrado correctamente.

Estos mensajes deberán desaparecer sin bloquear al usuario.

---

# 40. Ayuda contextual

Las pantallas importantes deberán poder mostrar ayuda.

Ejemplo:

Monto neto recibido [?]

Al consultar la ayuda:

Es el dinero que realmente llegó al intermediario después de los costos de fondeo.

---

# 41. Forma de mostrar ayuda

La ayuda podrá mostrarse mediante:

- información emergente,
- panel lateral,
- ventana pequeña,
- bloque desplegable.

La opción definitiva dependerá del contexto y deberá validarse visualmente.

---

# 42. Tutoriales

La aplicación tendrá una sección:

Ayuda

con tutoriales como:

- Cómo registrar un aporte
- Cómo registrar una compra
- Cómo actualizar precios
- Cómo interpretar posiciones
- Cómo interpretar el resumen
- Cómo interpretar rendimiento

---

# 43. Diseño responsivo

La aplicación deberá funcionar correctamente en:

- escritorio,
- portátil,
- tableta,
- móvil.

El enfoque principal del MVP será:

- escritorio,
- portátil.

Sin embargo, la interfaz no deberá romperse en dispositivos móviles.

---

# 44. Componentes reutilizables

Se deberán crear componentes reutilizables cuando exista una necesidad real.

Ejemplos conceptuales:

- TarjetaIndicador
- TablaDatos
- EtiquetaEstado
- ValorMonetario
- ValorPorcentaje
- AyudaContextual
- EstadoVacio
- DialogoConfirmacion

Los nombres definitivos de los componentes se decidirán durante la implementación.

---

# 45. Consistencia

Una misma acción deberá verse igual en todas las pantallas.

Ejemplo:

Guardar

no deberá ser azul en una pantalla y verde en otra sin una razón definida.

---

# 46. Botones

Se contemplarán dos tipos principales.

## 46.1 Primario

Ejemplos:

- Guardar
- Registrar aporte
- Registrar transacción

Utilizará principalmente el color principal.

---

## 46.2 Secundario

Ejemplos:

- Cancelar
- Volver

Deberá diferenciarse del botón principal sin competir visualmente.

---

# 47. Iconos

Los iconos podrán apoyar la comprensión.

No deberán sustituir texto cuando la acción no sea evidente.

Ejemplo:

Editar

puede mostrarse como:

icono + texto

---

# 48. Accesibilidad

La interfaz deberá considerar:

- contraste suficiente,
- etiquetas visibles,
- navegación por teclado cuando sea posible,
- no depender solo del color,
- tamaños de texto legibles,
- áreas de interacción suficientemente grandes.

El tema oscuro no deberá sacrificar accesibilidad por apariencia.

---

# 49. Formato de fechas

La API utilizará:

AAAA-MM-DD

La interfaz podrá mostrar:

DD/MM/AAAA

Ejemplo:

API:

2026-10-22

Interfaz:

22/10/2026

---

# 50. Tema visual

El tema oscuro será el tema principal y oficial del MVP.

No se implementará inicialmente un selector entre tema claro y oscuro.

La aplicación deberá diseñarse desde el inicio para funcionar correctamente en modo oscuro.

Todos los componentes deberán considerar:

- contraste,
- legibilidad,
- estados de interacción,
- formularios,
- tablas,
- gráficos,
- mensajes,
- diálogos.

Un tema claro podrá evaluarse como mejora futura.

No forma parte del MVP inicial.

---

# 51. Librería visual

Decisión: el MVP utilizará componentes propios con CSS propio.

Se descartaron Angular Material y PrimeNG.

Motivos:

- el prototipo aprobado ya define cada estilo y la paleta de §4;
- reproducir el prototipo con una librería exigiría sobrescribir gran parte de sus estilos;
- no se agregan dependencias.

Una librería solo se reevaluará si aparece una necesidad concreta que los componentes propios no cubran razonablemente.

Esa evaluación deberá considerar:

- facilidad de aprendizaje,
- calidad visual,
- mantenimiento,
- accesibilidad,
- cantidad de componentes necesarios,
- facilidad para reproducir el prototipo aprobado.

No se agregará una librería únicamente por conveniencia.

---

# 52. Uso de Impeccable

Para la creación, revisión y mejora del diseño visual se utilizará Impeccable como referencia complementaria de diseño para agentes.

Referencia:

https://impeccable.style/

El objetivo de utilizar Impeccable será mejorar la calidad visual del proyecto y reducir la apariencia genérica habitual de interfaces generadas automáticamente.

Se utilizará especialmente para ayudar a:

- mejorar la jerarquía visual,
- mejorar la composición de las pantallas,
- utilizar correctamente espacios y tamaños,
- mantener consistencia entre componentes,
- mejorar la relación entre tipografía, color y distribución,
- detectar patrones visuales poco cuidados,
- reducir elementos innecesarios,
- evitar diseños genéricos,
- conseguir una identidad visual más definida.

Impeccable será una referencia de diseño y no una fuente de reglas funcionales.

No deberá modificar:

- reglas financieras,
- cálculos,
- estructura de datos,
- contratos de API,
- funcionalidades aprobadas,
- alcance del MVP.

Las decisiones funcionales definidas en la documentación y en las especificaciones tendrán prioridad sobre cualquier recomendación puramente visual.

---

## 52.1 Objetivo visual

La aplicación deberá evitar una apariencia genérica de panel administrativo.

El objetivo será construir una aplicación financiera oscura con una identidad visual:

- moderna,
- sobria,
- profesional,
- reconocible,
- consistente,
- orientada a inversiones.

El diseño deberá sentirse intencional y no como una colección de componentes colocados automáticamente.

---

## 52.2 Patrones que se deberán evitar

Durante la creación del prototipo y del frontend se deberá evitar:

- utilizar tarjetas para absolutamente toda la información,
- colocar tarjetas dentro de otras tarjetas sin necesidad,
- utilizar demasiadas cajas y bordes,
- saturar las pantallas con indicadores,
- utilizar demasiados colores simultáneamente,
- utilizar degradados únicamente como decoración,
- utilizar sombras excesivas,
- utilizar tamaños de texto sin una jerarquía clara,
- mostrar demasiadas acciones con la misma importancia,
- utilizar iconos sin propósito,
- abusar de etiquetas de estado,
- dejar grandes espacios vacíos sin intención,
- utilizar textos genéricos como contenido final,
- repetir el mismo patrón visual en todas las secciones sin considerar su función.

---

## 52.3 Jerarquía visual

Cada pantalla deberá tener una jerarquía clara.

Como regla general deberá poder distinguirse rápidamente:

1. qué pantalla se está consultando,
2. cuál es la información más importante,
3. qué información es secundaria,
4. qué acciones puede realizar el usuario.

No todos los elementos deberán tener el mismo peso visual.

Los indicadores financieros principales deberán tener mayor importancia que la información auxiliar.

---

## 52.4 Uso de componentes

Los componentes reutilizables deberán mantener consistencia, pero no obligarán a que todas las pantallas tengan exactamente la misma composición.

Por ejemplo:

- Resumen podrá priorizar indicadores y gráficos.
- Posiciones podrá priorizar una tabla financiera.
- Transacciones podrá priorizar historial y formulario.
- Rendimiento podrá priorizar gráficos y evolución histórica.

La reutilización de componentes no deberá provocar una interfaz repetitiva o artificial.

---

## 52.5 Relación con el prototipo visual

Los criterios de Impeccable deberán aplicarse principalmente durante la creación del prototipo ubicado en:

referencia/prototipo-interfaz/

El prototipo será el lugar donde se validará visualmente:

- composición,
- jerarquía,
- espaciado,
- densidad de información,
- tipografía,
- colores,
- navegación,
- tablas,
- formularios,
- gráficos.

Una vez aprobado el prototipo, este tendrá prioridad como referencia concreta para la implementación Angular.

---

## 52.6 Prioridad de referencias

Para decisiones visuales se utilizará el siguiente orden:

1. requisitos funcionales aprobados,
2. especificaciones aprobadas,
3. esta guía de interfaz,
4. prototipo visual aprobado,
5. criterios de diseño proporcionados por Impeccable.

Impeccable deberá complementar el diseño del proyecto, no reemplazar sus decisiones funcionales.

# 53. Referencias visuales del proyecto

Las decisiones de interfaz se apoyarán en dos referencias principales.

## 53.1 Guía de interfaz

Este documento define:

- principios visuales,
- colores,
- comportamiento esperado,
- reglas de formularios,
- reglas de tablas,
- estados,
- accesibilidad,
- diseño responsivo,
- criterios generales de experiencia de usuario.

## 53.2 Prototipo visual

El prototipo visual aprobado se almacenará en:

referencia/prototipo-interfaz/

El prototipo permitirá observar de manera concreta:

- distribución de las pantallas,
- navegación,
- tarjetas,
- tablas,
- formularios,
- jerarquía visual,
- espacios,
- tamaños,
- uso real de los colores,
- comportamiento visual de los indicadores.

La implementación Angular deberá utilizar ambas referencias.

Cuando exista una diferencia visual, se deberá revisar primero si el prototipo representa una evolución aprobada de esta guía.

No se deberán realizar cambios visuales importantes de forma silenciosa.

---

# 54. Principios finales de interfaz

1. Priorizar claridad.
2. Reducir ruido visual.
3. Mostrar información financiera de forma comprensible.
4. Diferenciar datos editables de datos calculados.
5. Utilizar colores consistentemente.
6. No depender únicamente del color.
7. Mantener navegación sencilla.
8. Crear componentes reutilizables cuando aporten valor.
9. Mantener una experiencia consistente.
10. La interfaz debe ayudar a entender el portafolio, no solamente mostrar números.
11. La guía define las reglas visuales.
12. El prototipo aprobado muestra su aplicación concreta.
13. Angular deberá respetar ambas referencias.
14. El tema oscuro será el estilo oficial del MVP.

---

# 55. Prototipo visual de referencia

Antes de implementar el frontend en Angular se construirá un prototipo visual estático.

El prototipo se almacenará en:

referencia/prototipo-interfaz/

Podrá utilizar:

- HTML,
- CSS,
- JavaScript únicamente cuando sea necesario para representar interacciones visuales.

El prototipo deberá construirse utilizando el tema oscuro definido en esta guía.

No se deberá generar inicialmente una versión clara alternativa.

El prototipo:

- no consumirá la API,
- no tendrá lógica financiera real,
- no será código productivo,
- utilizará datos simulados,
- no reemplazará las especificaciones funcionales,
- servirá como referencia visual para el frontend definitivo.

La primera versión del prototipo deberá cubrir como mínimo:

- Resumen,
- Activos,
- Aportes,
- Transacciones,
- Posiciones,
- Rendimiento.

El prototipo deberá permitir validar antes de comenzar Angular:

- navegación,
- distribución general,
- paleta de colores,
- jerarquía visual,
- tarjetas,
- tablas,
- formularios,
- estados positivos y negativos,
- comportamiento responsivo básico,
- calidad visual del tema oscuro,
- contraste entre superficies,
- legibilidad de tablas sobre fondo oscuro,
- legibilidad de gráficos sobre fondo oscuro.

Una vez aprobado, la implementación Angular deberá respetar en la medida de lo posible:

- distribución,
- navegación,
- colores,
- jerarquía visual,
- tablas,
- tarjetas,
- formularios,
- comportamiento responsivo.

Si durante la implementación se requiere modificar de forma importante el diseño aprobado, el cambio deberá revisarse antes de realizarse.

---

# 56. Relación entre guía, prototipo e implementación

El flujo visual del proyecto será:

Guía de interfaz
↓
Prototipo visual
↓
Revisión
↓
Aprobación
↓
Implementación Angular

La guía establece las reglas.

El prototipo permite comprobar visualmente esas reglas.

Angular transforma el diseño aprobado en la aplicación real.

No se deberá utilizar el prototipo como código base del frontend productivo.

---

# 57. Datos del prototipo

Los datos mostrados en el prototipo serán simulados.

Podrán utilizarse valores similares al Excel de referencia para conseguir una representación realista.

Ejemplos:

- total aportado,
- valor del portafolio,
- rentabilidad,
- posiciones,
- BTC referido,
- asignaciones objetivo.

Estos valores tendrán únicamente propósito visual.

No deberán interpretarse como cálculos implementados.

---

# 58. Alcance inicial del prototipo

La primera versión del prototipo se centrará en seis pantallas.

## Resumen

Permitirá validar:

- navegación,
- tarjetas,
- indicadores,
- distribución del portafolio,
- gráficos.

## Activos

Permitirá validar:

- tabla,
- asignación objetivo,
- acciones de edición,
- formulario de registro.

## Aportes

Permitirá validar:

- tabla,
- formulario,
- campos monetarios,
- valores calculados.

## Transacciones

Permitirá validar:

- formulario,
- compra y venta,
- selector de activo,
- cálculos visuales.

## Posiciones

Permitirá validar:

- tabla financiera,
- ganancias y pérdidas,
- distribución actual contra objetivo.

## Rendimiento

Permitirá validar:

- gráficos,
- fotografías históricas,
- evolución del portafolio.

Las demás pantallas deberán seguir posteriormente el mismo lenguaje visual.

---

# 59. Aprobación del prototipo

Antes de iniciar el frontend productivo se deberá revisar el prototipo.

La revisión deberá comprobar:

- claridad,
- facilidad de navegación,
- consistencia,
- legibilidad,
- uso correcto de colores,
- comprensión de indicadores,
- cantidad de información mostrada,
- comportamiento en diferentes tamaños de pantalla,
- calidad visual del tema oscuro,
- contraste entre superficies,
- legibilidad de tablas en fondo oscuro,
- legibilidad de gráficos.

Después de aprobar el prototipo se podrán ajustar las reglas de este documento si se detectan mejoras necesarias.

La versión aprobada del prototipo será la referencia visual principal para la primera implementación Angular.

## Estado del prototipo visual

El prototipo visual del MVP fue consolidado en:

`referencia/prototipo-interfaz/Prototipo Interfaz v2.dc.html`

El prototipo cubre las 10 pantallas del MVP:

1. Resumen
2. Activos
3. Aportes
4. Transacciones
5. Referidos
6. Dividendos
7. Precios
8. Posiciones
9. Rendimiento
10. Ayuda

### Estado actual

El prototipo fue revisado y refinado con Claude Design e Impeccable.

Se validaron y ajustaron especialmente:

- jerarquía visual;
- layout de tablas en escritorio;
- comportamiento responsive;
- representación móvil de tablas densas;
- legibilidad de gráficos;
- tamaños táctiles;
- consistencia visual entre pantallas;
- tipografía;
- accesibilidad básica;
- navegación móvil.

El prototipo queda aprobado como referencia visual para la implementación en Angular.

### Criterios aprobados

- tema oscuro como tema oficial del MVP;
- navegación lateral en escritorio;
- navegación compacta en móvil;
- Hanken Grotesk como tipografía principal;
- Geist Mono para tickers y valores que requieren precisión visual;
- formularios laterales en pantallas de registro cuando el ancho lo permite;
- representación compacta de datos en móvil;
- evitar scroll horizontal innecesario;
- mantener scroll horizontal únicamente en tablas analíticas donde aporte valor;
- mantener franjas de color asociadas a activos;
- evitar cards, bordes, sombras y degradados innecesarios;
- mantener una composición sobria y específica del producto.

### Responsividad

El prototipo fue validado en distintos anchos, incluyendo:

- 390 px;
- 768 px;
- 1024 px;
- 1280 px;
- 1440 px;
- 1920 px.

En pantallas pequeñas se prioriza:

- información financiera esencial;
- bloques compactos en lugar de tablas extensas;
- objetivos táctiles cómodos;
- gráficos con etiquetas legibles;
- navegación clara hacia formularios.

### Pendientes funcionales

Los siguientes puntos no se resuelven en el prototipo visual y deben definirse mediante especificaciones:

- validación de ventas contra cantidad disponible;
- confirmación de fotografías del portafolio;
- estrategia para corregir registros inmutables;
- método de costo para ventas;
- umbral definitivo de desviación de asignación;
- validaciones de negocio adicionales.

Estas decisiones no deben ser inferidas por la interfaz ni por herramientas de diseño.

### Diferencias conocidas con el prototipo

La pantalla Activos del prototipo es anterior a la decisión de que los
activos no tienen estado. No deben implementarse:

- la columna Estado;
- las acciones Activar / Desactivar y su diálogo de confirmación;
- el activo de ejemplo VT marcado como inactivo;
- el texto "Los activos no se eliminan, se desactivan";
- la nota del tutorial "Registrar activo" que indica marcarlo como INACTIVO.

La columna `Activo?` de la hoja Activos del Excel de referencia tampoco
se traslada a la aplicación.

El campo "Porcentaje objetivo" del formulario de activo del prototipo no
es un dato del activo. Se guarda mediante el recurso de asignación
objetivo (`PUT /api/v1/asignaciones/{activoId}`).

El tipo `CRIPTO` de la hoja Activos del Excel corresponde al tipo
`CRIPTOMONEDA` de la aplicación.

Al editar un activo, el prototipo muestra el símbolo siempre como
"No editable". En la aplicación el símbolo es editable mientras el
activo no tenga registros históricos (`simboloEditable` = `true`).

### Regla para implementación

El prototipo es una referencia visual aprobada, no una restricción absoluta.

Durante la implementación Angular pueden realizarse ajustes si:

- mejoran la usabilidad;
- resuelven limitaciones del framework o librerías;
- mejoran responsive o accesibilidad;
- no contradicen las reglas funcionales aprobadas.

Los cambios importantes de diseño deberán reflejarse nuevamente en esta guía o en el sistema de diseño correspondiente.
