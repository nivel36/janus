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

  readonly label = input<string>('');
  readonly hint = input<string>('');
  readonly error = input<string>('');

  readonly required = input(false, { transform: booleanAttribute });
  readonly disabled = input(false, { transform: booleanAttribute });
  readonly invalid = input(false, { transform: booleanAttribute });

  readonly hintId = computed(() => `${this.controlId()}-hint`);
  readonly errorId = computed(() => `${this.controlId()}-error`);
}
