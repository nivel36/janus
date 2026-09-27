// @ts-check
const eslint = require("@eslint/js");
const { defineConfig } = require("eslint/config");
const tseslint = require("typescript-eslint");
const angular = require("angular-eslint");

module.exports = defineConfig([
  {
    ignores: ["dist/**", ".angular/**", "coverage/**", "node_modules/**", "src/app/api/generated/**"],
  },
  {
    files: ["**/*.ts"],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      "@typescript-eslint/no-unused-vars": "error",
      "no-restricted-imports": [
        "error",
        {
          paths: [
            {
              name: "zone.js",
              message: "Janus uses Angular's zoneless change detection in production and tests.",
            },
            {
              name: "zone.js/testing",
              message: "Use Angular's native zoneless test support instead of zone.js/testing.",
            },
            {
              name: "@angular/common",
              importNames: ["CommonModule"],
              message: "Import only the Angular directives and pipes used by the standalone declaration.",
            },
            {
              name: "@angular/core",
              importNames: [
                "HostBinding",
                "HostListener",
                "NgZone",
                "provideZoneChangeDetection",
                "ViewChild",
                "ViewChildren",
                "ContentChild",
                "ContentChildren",
              ],
              message: "Use zoneless Angular APIs, host metadata, and signal queries.",
            },
            {
              name: "@angular/forms",
              importNames: ["FormsModule"],
              message: "Use ReactiveFormsModule instead of the template-driven FormsModule.",
            },
          ],
        },
      ],
      "@angular-eslint/directive-selector": [
        "error",
        {
          type: "attribute",
          prefix: "app",
          style: "camelCase",
        },
      ],
      "@angular-eslint/component-selector": [
        "error",
        {
          type: "element",
          prefix: "app",
          style: "kebab-case",
        },
      ],
      "@angular-eslint/no-implicit-take-until-destroyed": "error",
      "@angular-eslint/prefer-inject": "error",
      "@angular-eslint/prefer-output-emitter-ref": "error",
      "@angular-eslint/prefer-output-readonly": "error",
      "@angular-eslint/prefer-signals": "error",
      "@angular-eslint/prefer-standalone": "error",
      "@angular-eslint/use-lifecycle-interface": "error",
    },
  },
  {
    files: ["src/app/**/*.ts"],
    ignores: ["**/*.spec.ts"],
    rules: {
      "@angular-eslint/prefer-on-push-component-change-detection": "error",
    },
  },
  {
    files: ["**/*.html"],
    extends: [
      angular.configs.templateRecommended,
      angular.configs.templateAccessibility,
    ],
    rules: {},
  }
]);
