---
target: Prototipo Interfaz v2 (todas las pantallas)
total_score: 24
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 2
target_identity: "file:C:\\Users\\xavie\\OneDrive\\Documents\\proyectos personales\\analizador-inversiones-personales\\referencia\\prototipo-interfaz\\Prototipo Interfaz v2.dc.html"
target_fingerprint: "sha256:b8a89f5186c6a39a20c18fa91f63a0b4847d5e7ad882faf39cb62fcd3cb98842"
target_path: "C:\\Users\\xavie\\OneDrive\\Documents\\proyectos personales\\analizador-inversiones-personales\\referencia\\prototipo-interfaz\\Prototipo Interfaz v2.dc.html"
timestamp: 2026-10-23T14-48-23Z
slug: po-interfaz-prototipo-interfaz-v2-dc-html-af684e91
---
# Crítica: Prototipo Interfaz v2 (todas las pantallas)

Método: dos agentes (A: revisión de diseño · B: detector + DOM renderizado en Chrome headless). Sin superposición en vivo; no hay automatización de navegador interactiva.

## Puntuación (heurísticas de Nielsen): 24/40, Aceptable

| # | Heurística | Nota | Problema clave |
|---|---|---|---|
| 1 | Visibilidad del estado | 3 | Fecha de precios y vistas previas Σ claras; nada confirma "Registrar …" |
| 2 | Lenguaje del usuario | 3 | Español financiero claro; "pp" sin explicar |
| 3 | Control y libertad | 2 | Registros inmutables sin forma visible de corregir |
| 4 | Consistencia | 2 | 3 patrones de KPI; "Crear fotografía" secundario en Resumen y primario en Rendimiento |
| 5 | Prevención de errores | 3 | Buenas vistas previas; no se ven validaciones (neto > aportado, venta > posición) |
| 6 | Reconocer antes que recordar | 3 | Leyendas presentes; "Desvío ±2 pp" (escala) se confunde con el umbral de 0.5 pp |
| 7 | Flexibilidad | 1 | No se puede ordenar, filtrar por fechas ni exportar |
| 8 | Estética minimalista | 3 | Sobrio; Posiciones con 12 columnas |
| 9 | Recuperación de errores | 1 | No hay estados de error diseñados |
| 10 | Ayuda | 3 | Centro de ayuda por tareas y "?" contextual |

## Especificidad
Producto específico, piel genérica. Los bloques "Del aporte al valor actual", las cajas Σ "Calculado", "Al recibir"/"Hoy" y la nota de saltos por aportes son propios de este producto. La paleta es Tailwind slate/blue por defecto, con tarjetas de radio 16 y un número héroe sobre área degradada: con el logo tapado podría ser cualquier plantilla. Detector: 10 hallazgos en el fuente; en el DOM renderizado, de 2 a 9 por pantalla (contraste de #64748B, texto de 11px, cramped-padding ×7 en Rendimiento que es falso positivo, overused-font Geist Mono que es falso positivo para cifras).

## Problemas prioritarios
1. [P1] Columnas recortadas en escritorio a 1440: Referidos (el grupo «Hoy» queda oculto), Posiciones (estado "Sobre objeti…") y Dividendos (nota cortada). Lo causa el formulario lateral fijo de ~390px.
2. [P1] 33 inputs/selects con outline:none inline que anula :focus-visible: sin indicador de foco en ningún formulario.
3. [P2] Contraste: #64748B como texto (3.7–4.0:1) en el pie lateral, las notas de navegación, los placeholders, "±2 pp" y los céntimos; bordes de input #253047 a 1.43:1 (falla 1.4.11); fila inactiva a 3.38:1; glifo del radio a 2.48:1; hover del botón primario a 3.68:1.
4. [P2] Texto de los gráficos escalado por viewBox: unos 9px en Resumen y unos 20px en Rendimiento; el gráfico de Precios sin ejes.
5. [P2] Jerarquía inconsistente entre pantallas: tres patrones de KPI (40+24, 4×34, 4×44 en rojo), 21 tamaños de fuente con medios píxeles, y la jerarquía de acciones cambia de una pantalla a otra. Rendimiento exagera la pérdida con −$17.19 / −2.99% a 44px en rojo.

## Personas
- Alex: no puede ordenar ni filtrar por fechas, no hay atajos ni CSV; el formulario fijo resta espacio a las tablas.
- Sam: no hay foco en los campos; contraste bajo; no hay aria-live tras registrar; cabeceras agrupadas sin scope=colgroup; los contenedores con scroll no reciben foco.
- Casey: el 90–95% de los controles mide menos de 44px (el "?" mide 20px, los selectores de periodo 28px); Posiciones en móvil esconde Ganancia y Asignación; en Transacciones el formulario queda debajo de la tabla.

## Menores
"AbrirActivos" sin espacio (l.1067); el pie lateral duplica el valor en Resumen; los puntos de 4px del desglose son imperceptibles; "Viendo historial" solo se distingue por el color; la barra de asignación de Resumen no tiene etiquetas; ACTIVO/INACTIVO es el único texto en mayúsculas.

## Preguntas
- ¿El titular debería separar Mercado −$1.28 de Costos −$16.89 en vez de mostrar el resultado mezclado?
- ¿Registrar merece un panel fijo de 390px, o mejor un panel que se abre al pedirlo?
- ¿Qué gesto propio (por ejemplo, la línea punteada de "Aportado") haría reconocible la interfaz?
