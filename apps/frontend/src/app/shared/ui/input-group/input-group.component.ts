/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, booleanAttribute, Component, input } from '@angular/core';

/**
 * Horizontal group for text inputs and addons.
 *
 * Project addons, InputComponent and ButtonComponent instances as children to
 * compose prefixed, suffixed, multiple-addon or button-addon fields.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-input-group',
  templateUrl: './input-group.component.html',
  styleUrl: './input-group.component.css',
})
export class InputGroupComponent {
  /** Accessible label for the grouped input controls. */
  readonly ariaLabel = input<string>();

  /** References visible labelling text when an external label is used. */
  readonly ariaLabelledBy = input<string>();

  /** Applies a disabled visual treatment to non-form addon content. */
  readonly disabled = input(false, { transform: booleanAttribute });
}
