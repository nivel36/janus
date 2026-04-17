/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, input } from '@angular/core';

/**
 * Visual variants supported by the chip component.
 */
type ChipType = 'default' | 'primary' | 'secondary' | 'tertiary';

@Component({
  selector: 'app-chip',
  standalone: true,
  imports: [],
  templateUrl: './chip.component.html',
  styleUrl: './chip.component.css',
})
export class ChipComponent {
  /**
   * Text rendered inside the chip.
   */
  readonly label = input.required<string>();

  /**
   * Visual type used to style the chip background.
   */
  readonly type = input<ChipType>('default');

  /**
   * Extra CSS classes added to the chip root element.
   */
  readonly styleClass = input<string>('');

  /**
   * Builds the BEM modifier class for the selected type.
   */
  get typeClass(): string {
    return `chip--${this.type()}`;
  }

  /**
   * Builds the complete CSS class list for the root chip element.
   */
  get chipClass(): string {
    const extraClass = this.styleClass().trim();
    return extraClass ? `app-chip ${this.typeClass} ${extraClass}` : `app-chip ${this.typeClass}`;
  }
}
