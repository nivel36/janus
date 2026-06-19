# Frontend styles

This document explains how frontend styles are conceptually organized and when each layer should be used. It is not meant to be an inventory of CSS variables, but a guide to understanding the application's design system.

## Global entry point

The global style entry point is `src/styles.css`. This file does not define visual rules directly: it works as the loading manifest and sets the import order for the system layers.

The order matters because later layers depend on earlier ones:

1. **Primitives**: base values with no product semantics.
2. **Themes**: color and surface assignments for each visual mode.
3. **Semantic tokens**: names oriented around interface usage.
4. **Component tokens**: specific adjustments for reusable UI pieces.
5. **Base styles**: general document rules and global utilities.
6. **Generic component styles**: shared `app-*` classes used across screens.

## Basic variable files

Basic variables live under `src/styles/tokens/` and are grouped by abstraction level.

### Primitives

`src/styles/tokens/00-primitives.css` contains the most basic system values: color palette, size scales, radii, stroke widths, and elementary type sizes. These tokens should not be used to express interface intent; they are the raw material used to build higher-level tokens.

For example, a primitive color represents a palette value; it does not say whether it is used for text, borders, backgrounds, error states, or accents.

### Themes

`src/styles/tokens/theme.dark.css` and `src/styles/tokens/theme.light.css` translate primitives into theme-dependent visual decisions. This is where concepts such as page background, text color, accent, borders, panels, selection, focus, and highlighted controls are defined.

The application uses the dark theme as the global base and can override it with the `data-theme='light'` attribute for light mode. Component rules should not duplicate theme decisions; they should consume semantic or component tokens.

### Semantic tokens

Files under `src/styles/tokens/semantic/` name variables by their role in the interface:

- `base.tokens.css`: semantic typography, focus, and common border definitions.
- `layout.tokens.css`: page structure, sections, panels, and summary cards.
- `forms.tokens.css`: forms, fields, hints, errors, controls, ranges, switches, and search bars.
- `controls.tokens.css`: small visual controls such as chips and their state variants.

This layer lets components speak in terms of intent: primary text, panel, focus, section, error, control, or action.

### Component tokens

Specific token files (`avatar.tokens.css`, `brand.tokens.css`, `button.tokens.css`, `list.tokens.css`, `autocomplete.tokens.css`, etc.) tune concrete interface pieces.

Their purpose is to isolate decisions for a component family without moving them into structural CSS. Some shared component styles keep their values beside the rules that consume them; for example, table values live directly in `src/styles/components/table.css` while still building on list and panel tokens.

## Base styles

`src/styles/base.css` defines cross-cutting rules that affect the document and global utilities. It sets the type family, basic `html` and `body` behavior, the application background, the common page content container, and accessibility utilities.

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

- **Application base** (`src/styles/base.css`)
  - `app-page-content`: common flexible container for the main content of pages.
  - `app-visually-hidden`: accessibility utility that visually hides content while keeping it available to screen readers.

- **Cards and panels** (`src/styles/components/card.css`)
  - `app-card`: main container for content panels, forms, widgets, and highlighted blocks.
  - `app-card__header`: top area of a card, usually for a title or contextual actions.
  - `app-card__title`: visual title of a card.
  - `app-card__body`: main content area with growth and internal scroll handling.
  - `app-card__footer`: bottom area for actions, totals, or secondary content.

- **Forms** (`src/styles/components/forms.css`)
  - `app-form`: common vertical structure for creation, editing, and preferences forms.

- **Form actions** (`src/styles/components/form-actions.css`)
  - `app-form-actions`: aligned group of submit, cancel, or other action buttons at the end of a form.

- **Detail headers** (`src/styles/components/header-detail.css`)
  - `app-header-detail`: highlighted header for entity detail pages.
  - `app-header-detail__identity`: identity block that groups icon, title, and main information.
  - `app-header-detail__icon`: visual container for the entity icon or avatar.
  - `app-header-detail__title`: main title of the displayed entity.
  - `app-header-detail__info`: metadata line or secondary information.
  - `app-header-detail__status`: reserved area for status, label, or chip associated with the detail view.

- **Sections** (`src/styles/components/section.css`)
  - `app-section`: reusable vertical block that groups related content inside a page or card.
  - `app-section__header`: section header with title and optional actions.
  - `app-section__title`: section title with consistent visual treatment.
  - `app-section__message`: informational message, empty state, or notice associated with a section.
  - `app-section__message--error`: message variant for errors inside a section.

- **Tables and tabular lists** (`src/styles/components/table.css`)
  - `app-table`: generic table for result lists, records, schedules, worksites, and time logs.
  - `app-table thead`: visual table header.
  - `app-table th`: compact, highlighted header cells.
  - `app-table td`: body data cells.
  - `app-table tbody tr`: result rows with visual alternation and hover state.
