# Estilos del frontend

Este documento describe cómo se organizan conceptualmente los estilos del frontend y cuándo conviene utilizar cada capa. No pretende ser un inventario de variables CSS, sino una guía para entender el sistema de diseño de la aplicación.

## Punto de entrada global

El punto de entrada de estilos globales es `src/styles.css`. Este fichero no define reglas visuales directamente: actúa como manifiesto de carga y fija el orden de importación de las capas del sistema.

El orden es importante porque las capas posteriores dependen de las anteriores:

1. **Primitivos**: valores base sin semántica de producto.
2. **Temas**: asignación de colores y superficies por modo visual.
3. **Tokens semánticos**: nombres orientados al uso dentro de la interfaz.
4. **Tokens de componentes**: ajustes específicos para piezas reutilizables.
5. **Estilos base**: reglas generales de documento y utilidades globales.
6. **Estilos genéricos de componentes**: clases `app-*` compartidas por distintas pantallas.

## Ficheros de variables básicas

Las variables básicas viven bajo `src/styles/tokens/` y se agrupan por nivel de abstracción.

### Primitivos

`src/styles/tokens/00-primitives.css` contiene los valores más básicos del sistema: paleta de colores, escalas de tamaño, radios, grosores y tamaños tipográficos elementales. Estos tokens no deberían usarse para expresar intención de interfaz, sino como materia prima para construir tokens superiores.

Por ejemplo, un color primitivo representa un valor de paleta; no dice si se utiliza para texto, borde, fondo, estado de error o acento.

### Temas

`src/styles/tokens/theme.dark.css` y `src/styles/tokens/theme.light.css` traducen los primitivos a decisiones visuales dependientes del tema. Aquí se definen conceptos como fondo de página, color de texto, acento, bordes, paneles, selección, foco y controles destacados.

La aplicación usa el tema oscuro como base global y permite sobrescribirlo con el atributo `data-theme='light'` para el modo claro. Las reglas de componentes no deberían duplicar decisiones de tema; deben consumir tokens semánticos o de componente.

### Tokens semánticos

Los ficheros de `src/styles/tokens/semantic/` nombran las variables por su función en la interfaz:

- `base.tokens.css`: tipografía semántica, foco y definiciones comunes de borde.
- `layout.tokens.css`: estructura de página, secciones, paneles y tarjetas resumen.
- `forms.tokens.css`: formularios, campos, ayudas, errores, controles, rangos, switches y buscadores.
- `controls.tokens.css`: controles visuales pequeños como chips y sus variantes de estado.

Esta capa permite que los componentes hablen en términos de intención: texto principal, panel, foco, sección, error, control o acción.

### Tokens de componente

Los ficheros de tokens específicos (`avatar.tokens.css`, `brand.tokens.css`, `button.tokens.css`, `header-detail.tokens.css`, `list.tokens.css`, `table.tokens.css`, `autocomplete.tokens.css`, etc.) ajustan piezas concretas de la interfaz.

Su objetivo es aislar decisiones de una familia de componentes sin llevarlas al CSS estructural. Por ejemplo, las tablas consumen tokens propios que a su vez se apoyan en los tokens de listas y paneles; así se mantiene una apariencia coherente entre listados, tablas y resultados interactivos.

## Estilos base

`src/styles/base.css` define reglas transversales que afectan al documento y a utilidades globales. Aquí se fija la familia tipográfica, el comportamiento básico de `html` y `body`, el fondo de la aplicación, el contenedor común de página y utilidades de accesibilidad.

Estos estilos deben mantenerse pequeños y genéricos. Si una regla describe una pieza de UI reutilizable, debería vivir en `src/styles/components/`; si describe una pantalla concreta, debería quedarse en el CSS del componente Angular correspondiente.

## Estilos genéricos

Los estilos genéricos viven en `src/styles/components/` y exponen clases globales con prefijo `app-*`. Son bloques reutilizables para patrones de interfaz que aparecen en varias funcionalidades.

La idea es evitar que cada feature recree estructura, espaciado, bordes, sombras o estados vacíos por su cuenta. Cada pantalla puede añadir clases propias de feature para composición local, pero debería apoyarse en estos estilos comunes cuando el patrón ya existe.

## Relación con los estilos de componentes Angular

Los componentes Angular mantienen sus estilos específicos junto al propio componente (`*.component.css`). Esa capa debe encargarse de composición local, variantes de una pantalla concreta o ajustes que no sean reutilizables.

Como criterio general:

- Si el estilo expresa una decisión global de diseño, debe ser un token.
- Si el estilo define un patrón reutilizable entre pantallas, debe ser un estilo genérico `app-*`.
- Si el estilo solo tiene sentido para una feature o componente concreto, debe quedarse en su CSS local.

## Esquema de estilos genéricos de la app

- **Base de aplicación** (`src/styles/base.css`)
  - `app-page-content`: contenedor flexible común para el contenido principal de las páginas.
  - `app-visually-hidden`: utilidad de accesibilidad para ocultar contenido visualmente manteniéndolo disponible para lectores de pantalla.

- **Tarjetas y paneles** (`src/styles/components/card.css`)
  - `app-card`: contenedor principal para paneles de contenido, formularios, widgets y bloques destacados.
  - `app-card__header`: zona superior de una tarjeta, normalmente para título o acciones contextuales.
  - `app-card__title`: título visual de una tarjeta.
  - `app-card__body`: zona de contenido principal con gestión de crecimiento y scroll interno.
  - `app-card__footer`: zona inferior para acciones, totales o contenido secundario.

- **Formularios** (`src/styles/components/forms.css`)
  - `app-form`: estructura vertical común para formularios de creación, edición y preferencias.

- **Acciones de formulario** (`src/styles/components/form-actions.css`)
  - `app-form-actions`: agrupación alineada de botones de envío, cancelación u otras acciones al final de un formulario.

- **Cabeceras de detalle** (`src/styles/components/header-detail.css`)
  - `app-header-detail`: cabecera destacada para páginas de detalle de entidad.
  - `app-header-detail__identity`: bloque de identidad que agrupa icono, título e información principal.
  - `app-header-detail__icon`: contenedor visual del icono o avatar de la entidad.
  - `app-header-detail__title`: título principal de la entidad mostrada.
  - `app-header-detail__info`: línea de metadatos o información secundaria.
  - `app-header-detail__status`: zona reservada para estado, etiqueta o chip asociado al detalle.

- **Secciones** (`src/styles/components/section.css`)
  - `app-section`: bloque vertical reutilizable para agrupar contenido relacionado dentro de una página o tarjeta.
  - `app-section__header`: cabecera de sección con título y posibles acciones.
  - `app-section__title`: título de sección con tratamiento visual uniforme.
  - `app-section__message`: mensaje informativo, estado vacío o aviso asociado a una sección.
  - `app-section__message--error`: variante de mensaje para errores dentro de una sección.

- **Tablas y listados tabulares** (`src/styles/components/table.css`)
  - `app-table`: tabla genérica para listados de resultados, registros, horarios, centros de trabajo y fichajes.
  - `app-table thead`: cabecera visual de la tabla.
  - `app-table th`: celdas de encabezado con estilo compacto y texto destacado.
  - `app-table td`: celdas de datos del cuerpo.
  - `app-table tbody tr`: filas de resultados con alternancia visual y estado hover.
