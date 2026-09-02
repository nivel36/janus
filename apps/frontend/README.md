# Frontend

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
