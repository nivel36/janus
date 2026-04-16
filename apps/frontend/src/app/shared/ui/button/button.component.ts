/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, input, output } from '@angular/core';

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
  readonly variant = input<ButtonVariant>('default');

  /**
   * Native button type used for form interaction.
   */
  readonly type = input<ButtonType>('button');

  /**
   * Optional decorative icon rendered as plain text.
   */
  readonly icon = input<string>();

  /**
   * Side where the decorative icon is rendered.
   */
  readonly iconPosition = input<IconPosition>('left');

  /**
   * Whether user interaction is disabled.
   */
  readonly disabled = input<boolean>(false);

  /**
   * Extra CSS classes appended to the root button element.
   */
  readonly styleClass = input<string>('');

  /**
   * Accessible name for icon-only usage.
   *
   * If no projected text is provided, set this input so assistive
   * technologies can announce a meaningful label.
   */
  readonly ariaLabel = input<string>();

  /**
   * Emits the native click event when the button is activated.
   */
  readonly clicked = output<MouseEvent>();

  /**
   * Builds the BEM modifier class for the selected variant.
   *
   * @returns CSS class suffix for the variant.
   */
  get variantClass(): string {
    return `button--${this.variant()}`;
  }

  /**
   * Indicates whether the icon must be rendered before the label.
   *
   * @returns `true` when an icon exists and the icon position is `left`.
   */
  get hasIconOnLeft(): boolean {
    return !!this.icon() && this.iconPosition() === 'left';
  }

  /**
   * Indicates whether the icon must be rendered after the label.
   *
   * @returns `true` when an icon exists and the icon position is `right`.
   */
  get hasIconOnRight(): boolean {
    return !!this.icon() && this.iconPosition() === 'right';
  }

  /**
   * Builds the complete CSS class list applied to the root button element.
   *
   * @returns Root CSS classes as a space-separated string
   */
  get buttonClass(): string {
    const extraClass = this.styleClass().trim();
    return extraClass
      ? `app-button ${this.variantClass} ${extraClass}`
      : `app-button ${this.variantClass}`;
  }
}
