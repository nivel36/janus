/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, booleanAttribute, Component, input, output } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';

/**
 * Visual intent styles that can be applied to the button.
 *
 * `text` is intended for low-emphasis actions that still need button semantics,
 * rather than for navigation (which must use a link).
 */
export type ButtonVariant = 'default' | 'main' | 'secondary' | 'text';

/**
 * Native button `type` values supported by this component.
 */
export type ButtonType = 'button' | 'submit' | 'reset';

/**
 * Shared application button.
 *
 * Use `app-button` for application actions, form submission, cancellation,
 * retry actions and secondary actions. Use `routerLink` when the control
 * navigates to a fixed application destination so it renders as a semantic
 * link.
 *
 * Native buttons are reserved for the internal implementation of compound
 * controls whose semantics and keyboard interaction are owned by that control,
 * such as tabs, the paginator and the toggle button.
 */

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-button',
  imports: [NgTemplateOutlet, RouterLink],
  templateUrl: './button.component.html',
  styleUrl: './button.component.css',
})
export class ButtonComponent {
  /**
   * Fixed destination that renders this control as a semantic link.
   */
  readonly routerLink = input<string | readonly unknown[]>();

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

  /** Optional native tooltip text. */
  readonly title = input<string>();

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
