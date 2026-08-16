# Frontend styles

This document explains how frontend styles are conceptually organized and when each layer should be used. It is not meant to be an inventory of CSS variables, but a guide to understanding the application's design system.

## Global entry points

The Angular build declares two global entry points in `angular.json`, in this order:

1. `src/reset.css` normalizes browser defaults.
2. `src/styles.css` loads the application design system.

`src/styles.css` does not define visual rules directly: it works as the design-system loading
manifest and sets the import order for its layers.

The order matters because later layers depend on earlier ones:

1. **Primitives**: base values with no product semantics.
2. **Themes**: color and surface assignments for each visual mode.
3. **Semantic tokens**: names oriented around interface usage.
4. **Component tokens**: specific adjustments for reusable UI pieces.
5. **Base styles**: general document rules and global utilities.
6. **Generic component styles**: shared `app-*` classes used across screens.

## Style layers and files

The paths below are the design-system files currently loaded by `src/styles.css`. Keep this list
aligned with that manifest; files that are not imported there are not part of the design-system
pipeline managed by `src/styles.css`. The separate `src/reset.css` entry point remains part of
the Angular global style pipeline described above.

### Primitives

`src/styles/00-primitives.css` contains the most basic system values: color palette, size scales,
radii, stroke widths, font sizes and weights, letter spacing, and line heights. These tokens
should not be used to express interface intent; they are the raw material used to build
higher-level tokens.

For example, a primitive color represents a palette value; it does not say whether it is used for text, borders, backgrounds, error states, or accents.

### Themes

`src/styles/themes/10-theme.dark.css` and `src/styles/themes/11-theme.light.css` translate primitives
into theme-dependent visual decisions. This is where concepts such as page background, text
color, accent, borders, panels, selection, focus, and highlighted controls are defined.

The application uses the dark theme as the global base and can override it with the `data-theme='light'` attribute for light mode. Component rules should not duplicate theme decisions; they should consume semantic or component tokens.

### Semantic tokens

Semantic token files live directly under `src/styles/tokens/` and name variables by their role
in the interface:

- `src/styles/tokens/20-base.tokens.css`: focus rings and common border definitions.
- `src/styles/tokens/21-literals.tokens.css`: the semantic typography roles and capitalization
  helpers.
- `src/styles/tokens/22-layout.tokens.css`: page, section, and panel structure.
- `src/styles/tokens/23-forms.tokens.css`: forms, fields, hints, errors, controls, ranges, switches,
  and search bars.

This layer lets components speak in terms of intent: primary text, panel, focus, section, error, control, or action.

### Component tokens

`src/styles/tokens/30-list.tokens.css` is the component-token file currently loaded by the
manifest. It tunes list spacing, borders, radius, shadow, and alternating-row background. Its
purpose is to isolate decisions for that component family without moving them into structural
CSS. Other shared component styles keep their values beside the rules that consume them; for
example, table rules live directly in `src/styles/components/55-table.css` while building on list
and panel tokens.

There are currently no `semantic/` or component-specific subdirectories below
`src/styles/tokens/`. Do not document or import a proposed token file until it exists and has
been added to `src/styles.css` in the intended cascade position.

## Typography token convention

Typography primitives (`--font-size-*`, `--font-weight-*`, and `--line-height-*`) are defined in
`src/styles/00-primitives.css`. Components should normally consume the semantic aliases in
`src/styles/tokens/21-literals.tokens.css` instead of those primitives directly.

For roles that have multiple sizes, the naming convention is
`--type-{role}-{size}-{font-size|font-weight|line-height}`. The current roles and sizes are:

- `heading`: `lg`, `md`, and `sm`;
- `body`: `md` and `sm`;
- `label`: `lg`, `md`, and `sm`;
- `caption`: `md` and `sm`.

For example, a medium body style is composed from `--type-body-md-font-size`,
`--type-body-md-font-weight`, and `--type-body-md-line-height`. The single brand style is the
intentional size-less exception: it uses `--type-brand-font-size`, `--type-brand-font-weight`,
and `--type-brand-line-height`.

Uppercase treatments are independent helpers with the `--type-caps-*` prefix:
`--type-caps-letter-spacing`, `--type-caps-wide-letter-spacing`, and
`--type-caps-text-transform`. They can be combined with any semantic typography role and do not
define a font size, weight, or line height themselves.

## Base styles

`src/styles/40-base.css` defines cross-cutting rules that affect the document and global utilities. It sets the type family, basic `html` and `body` behavior, the application background, the common page content container, and accessibility utilities.

These styles should stay small and generic. If a rule describes a reusable UI piece, it should live in `src/styles/components/`; if it describes a specific screen, it should remain in the corresponding Angular component CSS.

## Generic styles

Generic styles live in `src/styles/components/` and expose global classes with the `app-*` prefix. They are reusable blocks for interface patterns that appear in multiple features.

The goal is to avoid each feature recreating structure, spacing, borders, shadows, or empty states on its own. Each screen can add feature-specific classes for local composition, but it should rely on these shared styles when the pattern already exists.

## Relationship with Angular component styles

Angular components keep their specific styles next to the component itself (`*.component.css`). This layer should handle local composition, variants for a concrete screen, or adjustments that are not reusable.

As a general rule:

- If the style expresses a global design decision, it should be a token.
- If the style defines a reusable pattern across screens, it should be a generic `app-*` style.
- If the style only makes sense for one feature or concrete component, it should stay in its local CSS file.

## Generic application style schema

- **Application base** (`src/styles/40-base.css`)
  - `app-page-content`: common flexible container for the main content of pages.
  - `app-visually-hidden`: accessibility utility that visually hides content while keeping it available to screen readers.

- **Cards and panels** (`src/styles/components/50-card.css`)
  - `app-card`: main container for content panels, forms, widgets, and highlighted blocks.
  - `app-card__header`: top area of a card, usually for a title or contextual actions.
  - `app-card__title`: visual title of a card.
  - `app-card__body`: main content area with growth and internal scroll handling.
  - `app-card__footer`: bottom area for actions, totals, or secondary content.

- **Forms** (`src/styles/components/51-forms.css`)
  - `app-form`: common vertical structure for creation, editing, and preferences forms.

- **Form actions** (`src/styles/components/52-form-actions.css`)
  - `app-form-actions`: aligned group of submit, cancel, or other action buttons at the end of a form.
  - `app-form-actions > * + *`: consistent separation between consecutive actions.

- **Detail headers** (`src/styles/components/53-header-detail.css`)
  - `app-header-detail`: highlighted header for entity detail pages.
  - `app-header-detail__identity`: identity block that groups icon, title, and main information.
  - `app-header-detail__icon`: visual container for the entity icon or avatar.
  - `app-header-detail__title`: main title of the displayed entity.
  - `app-header-detail__info`: metadata line or secondary information.
  - `app-header-detail__status`: reserved area for status, label, or chip associated with the detail view.

- **Sections** (`src/styles/components/54-section.css`)
  - `app-section`: reusable vertical block that groups related content inside a page or card.
  - `app-section__header`: section header with title and optional actions.
  - `app-section__title`: section title with consistent visual treatment.
  - `app-section__message`: informational message, empty state, or notice associated with a section.
  - `app-section__message--error`: message variant for errors inside a section.

- **Tables and tabular lists** (`src/styles/components/55-table.css`)
  - `app-table`: generic table for result lists, records, schedules, worksites, and time logs.
  - `app-table thead`: visual table header.
  - `app-table th`: compact, highlighted header cells.
  - `app-table td`: body data cells.
  - `app-table tbody tr`: result rows with visual alternation and hover state.

- **List pages** (`src/styles/components/56-list-page.css`)
  - `app-list-page`: vertical content layout for searchable result pages.
  - `app-list-page__toolbar`: responsive search and actions row.
  - `app-list-page__results`: growing results area with a stable minimum height.

Message visuals remain local to `shared/ui/message` because their variants are encapsulated by
the Angular component rather than exposed as global `app-*` classes.
