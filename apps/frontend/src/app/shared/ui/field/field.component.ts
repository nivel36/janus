/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  booleanAttribute,
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
} from '@angular/core';

@Component({
  selector: 'app-field',
  standalone: true,
  templateUrl: './field.component.html',
  styleUrl: './field.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FieldComponent {
  readonly controlId = input.required<string>();

  readonly label = input.required<string>();
  readonly hint = input<string>('');
  readonly error = input<string>('');
  readonly styleClass = input<string>('');

  readonly required = input(false, { transform: booleanAttribute });
  readonly disabled = input(false, { transform: booleanAttribute });
  readonly invalid = input(false, { transform: booleanAttribute });

  readonly fieldClass = computed(() => {
    const classes = ['app-field'];
    const extraClass = this.styleClass().trim();

    if (this.invalid()) {
      classes.push('app-field--invalid');
    }

    if (this.disabled()) {
      classes.push('app-field--disabled');
    }

    if (extraClass) {
      classes.push(extraClass);
    }

    return classes.join(' ');
  });

  readonly hintId = computed(() => `${this.controlId()}-hint`);
  readonly errorId = computed(() => `${this.controlId()}-error`);
}
