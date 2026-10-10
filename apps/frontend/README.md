# Frontend

## Generated API client

Transport routes and DTOs are generated with OpenAPI Generator's
`typescript-angular` generator from the versioned backend contract at
`../backend/src/main/resources/janus.yaml`. Generated sources live under
`src/app/client`; do not edit them manually.

Run `npm run generate:api` after changing the API specification. CI runs
`npm run check:api` and fails when the committed client is out of date.

## Testing

Run the unit test suite through the Angular builder:

```sh
npm test -- --watch=false
```

The project uses Vitest as the runner, but Angular CLI prepares component
templates, styles and Angular metadata before handing the tests to Vitest.

## Entity forms

Use `FormComponent` (`app-form`) for create/edit screens. It owns the native
form, Save/Cancel actions, validation before submission, the save subscription,
busy state and translated error feedback. It blocks repeated submissions,
disables fields and cancellation while saving, and unsubscribes on destruction.
Errors preserve entered values and allow another attempt.

```html
<app-form
  [form]="form"
  [save]="save"
  saveErrorKey="worksite.errors.create"
  cancelLink="/worksites"
  (saved)="onSaved()"
>
  <app-field controlId="name" [label]="'worksite.name' | translate" required>
    <app-input inputId="name" [formControl]="form.controls.name" />
  </app-field>
</app-form>
```

Import `FormComponent` and `ReactiveFormsModule` in the page. Bind projected
fields with `[formControl]`; the form group directive is inside the shared
component. Supply `save` as an arrow function returning a cold Observable;
do not subscribe in the page. Build the entity payload in that function and
handle the typed API response in `(saved)` for navigation or resetting values.

`saveErrorKey` accepts a translation key or an error-to-key function (for example,
to distinguish conflicts). Use `errorKey` for load errors and `disabled` while
loading or when editing is unavailable. `showSave` hides the save action for
read-only screens. `saveLabel` and `cancelLabel` accept alternative translation
keys. Provide `cancelLink` for navigation, or handle `(cancelled)` when cancelling
requires page-specific logic. Save is disabled for invalid, pending or disabled
form groups. Blocking fields while saving preserves each control's model state,
including controls that were already disabled.

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
