/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, input } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { IconDefinition } from '@fortawesome/fontawesome-svg-core';

/**
 * Visual variants supported by the chip component.
 */
type ChipType = 'default' | 'primary' | 'secondary' | 'tertiary' | 'green' | 'red';

/**
 * Size variants supported by the chip component.
 */
type ChipSize = 'normal' | 'big' | 'small';

@Component({
  selector: 'app-chip',
  standalone: true,
  imports: [FontAwesomeModule],
  templateUrl: './chip.component.html',
  styleUrl: './chip.component.css',
})
export class ChipComponent {
  /**
   * Text rendered inside the chip.
   */
  readonly label = input.required<string>();

  /**
   * Optional decorative icon rendered before the text.
   */
  readonly icon = input<IconDefinition>();

  /**
   * Visual type used to style the chip background.
   */
  readonly type = input<ChipType>('default');

  /**
   * Size used to adjust the chip spacing and text size.
   */
  readonly size = input<ChipSize>('normal');

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
   * Builds the BEM modifier class for the selected size.
   */
  get sizeClass(): string {
    return `chip--size-${this.size()}`;
  }

  /**
   * Indicates whether the optional icon should be rendered.
   */
  get hasIcon(): boolean {
    return !!this.icon();
  }

  /**
   * Builds the complete CSS class list for the root chip element.
   */
  get chipClass(): string {
    const extraClass = this.styleClass().trim();
    return extraClass
      ? `app-chip ${this.typeClass} ${this.sizeClass} ${extraClass}`
      : `app-chip ${this.typeClass} ${this.sizeClass}`;
  }
}
