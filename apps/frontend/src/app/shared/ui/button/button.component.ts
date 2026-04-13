/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Visual intent styles that can be applied to the button.
 */
type ButtonVariant = 'default' | 'main' | 'secondary';

/**
 * Native button `type` values supported by this component.
 */
type ButtonType = 'button' | 'submit' | 'reset';

/**
 * Allowed positions for the optional decorative icon.
 */
type IconPosition = 'left' | 'right';

@Component({
  selector: 'app-button',
  standalone: true,
  imports: [],
  templateUrl: './button.component.html',
  styleUrl: './button.component.css',
})
export class ButtonComponent {
  /**
   * Visual variant used to build the CSS modifier class.
   */
  @Input() variant: ButtonVariant = 'default';

  /**
   * Native button type used for form interaction.
   */
  @Input() type: ButtonType = 'button';

  /**
   * Optional decorative icon rendered as plain text.
   */
  @Input() icon: string | null = null;

  /**
   * Side where the decorative icon is rendered.
   */
  @Input() iconPosition: IconPosition = 'left';

  /**
   * Whether user interaction is disabled.
   */
  @Input() disabled = false;

  /**
   * Extra CSS classes appended to the root button element.
   */
  @Input() styleClass = '';

  /**
   * Accessible name for icon-only usage.
   *
   * If no projected text is provided, set this input so assistive
   * technologies can announce a meaningful label.
   */
  @Input() ariaLabel: string | null = null;

  /**
   * Emits the native click event when the button is activated.
   */
  @Output() clicked = new EventEmitter<MouseEvent>();

  /**
   * Builds the BEM modifier class for the selected variant.
   *
   * @returns CSS class suffix for the variant.
   */
  get variantClass(): string {
    return `button--${this.variant}`;
  }

  /**
   * Indicates whether the icon must be rendered before the label.
   *
   * @returns `true` when an icon exists and the icon position is `left`.
   */
  get hasIconOnLeft(): boolean {
    return !!this.icon && this.iconPosition === 'left';
  }

  /**
   * Indicates whether the icon must be rendered after the label.
   *
   * @returns `true` when an icon exists and the icon position is `right`.
   */
  get hasIconOnRight(): boolean {
    return !!this.icon && this.iconPosition === 'right';
  }
}
