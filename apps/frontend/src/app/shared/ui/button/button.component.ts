/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { booleanAttribute, Component, input, output } from '@angular/core';

/**
 * Visual intent styles that can be applied to the button.
 */
type ButtonVariant = 'default' | 'main' | 'secondary';

/**
 * Native button `type` values supported by this component.
 */
type ButtonType = 'button' | 'submit' | 'reset';

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
   * Whether user interaction is disabled.
   */
  readonly disabled = input(false, { transform: booleanAttribute });

  /**
   * Whether the button is rendered as an icon-only button.
   *
   * Icon buttons are square, centered and require an accessible label.
   */
  readonly icon = input(false, { transform: booleanAttribute });

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
    return `app-button--${this.variant()}`;
  }

  /**
   * Builds the complete CSS class list applied to the root button element.
   *
   * @returns Root CSS classes as a space-separated string.
   */
  get buttonClass(): string {
    return [
      'app-button',
      this.variantClass,
      this.icon() ? 'app-button--icon' : '',
      this.styleClass().trim(),
    ]
      .filter(Boolean)
      .join(' ');
  }
}
