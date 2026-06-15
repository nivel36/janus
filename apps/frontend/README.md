# Frontend

## Linting

Run the Angular lint and the CSS custom-property validation together before submitting frontend changes:

```sh
npm run lint
npm run lint:styles
```

`npm run lint:styles` scans every `*.css` file under `src/`, compares custom properties defined as `--name:` with usages written as `var(--name)`, and fails when a usage has no matching definition.
