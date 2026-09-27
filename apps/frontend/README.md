# Frontend

## Generated API client

Transport routes and DTOs are generated with OpenAPI Generator's
`typescript-angular` generator from the versioned backend contract at
`../backend/src/main/resources/janus.yaml`. Generated sources live under
`src/app/api/generated`; do not edit them manually.

Run `npm run generate:api` after changing the API specification. CI runs
`npm run check:api` and fails when the committed client is out of date.

## Testing

Run the unit test suite through the Angular builder:

```sh
npm test -- --watch=false
```

The project uses Vitest as the runner, but Angular CLI prepares component
templates, styles and Angular metadata before handing the tests to Vitest.

## Linting

Run the Angular lint and the CSS custom-property validation together before submitting frontend changes:

```sh
npm run lint
npm run lint:styles
```

`npm run lint:styles` scans every `*.css` file under `src/`, compares custom properties defined as `--name:` with usages written as `var(--name)`, and fails when a usage has no matching definition.

## CSS token naming

Use the `*-color` suffix only for tokens whose value is a single valid CSS color. For
composite values that represent an entire CSS property and may contain values such as
gradients, use the matching `*-background`, `*-border`, or `*-shadow` suffix instead.

## Style classification

Frontend styles belong to one of these three categories:

1. **Global foundations** define primitives, themes, semantic and component tokens, and
   element-level defaults. They live under `src/styles/` and are imported by
   `src/styles.css`; they must not contain the internal selectors of an Angular component.
2. **Global composition patterns** arrange several independent pieces of UI without owning
   their rendering. The deliberately reusable `.app-form`, `.app-form-actions`,
   `.app-header-detail`, `.app-section`, `.app-table`, and `.app-list-page` patterns remain in
   `src/styles/components/51-forms.css` through `56-list-page.css`. Consumers should opt into
   their public classes explicitly; composition rules must not select a shared component's
   private DOM or implementation classes.
3. **Encapsulated component styles** belong next to the Angular component that owns the
   markup, using `styleUrl` in its component metadata. For example, `app-card` owns
   `card.component.html` and `card.component.css`; feature pages use the component rather than
   styling `.app-card__body` or its other internal elements.

When a feature needs to configure an encapsulated component, expose a documented input, CSS
custom property, or shared directive instead of using `::ng-deep` or reaching into the
component's DOM. Promote a layout to global CSS only when it is intentionally reusable across
features and can be expressed solely through public composition classes.
