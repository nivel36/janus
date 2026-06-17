/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { NgTemplateOutlet } from '@angular/common';
import { Component, TemplateRef, contentChild, input } from '@angular/core';
import { createUuid } from '../../utils/uuid.utils';

/**
 * Reusable card container with optional header and footer template slots.
 *
 * <p>The component exposes ARIA metadata so the rendered {@code <section>}
 * can be announced as a named region by assistive technologies.</p>
 *
 * <p>The card id is resolved as follows:</p>
 * <ul>
 *   <li>If the consumer provides {@code id}, that value is used.</li>
 *   <li>Otherwise, a generated identifier based on {@link createUuid} is used.</li>
 * </ul>
 */
@Component({
  selector: 'app-card',
  standalone: true,
  imports: [NgTemplateOutlet],
  templateUrl: './card.component.html',
  styleUrl: './card.component.css',
})
export class CardComponent {
  readonly titleElementId = `card-${createUuid()}-title`;

  /**
   * Plain text title rendered in the card header.
   *
   * <p>When present, this title is also used to label the card region through
   * {@code aria-labelledby}.</p>
   */
  readonly title = input<string>();

  /**
   *  Extra CSS classes applied to the outer card container.
   */
  readonly styleClass = input<string>();

  /**
   * Accessible name used when no title is rendered.
   *
   * <p>Use this input to provide a meaningful label for screen readers when the
   * card has no visible heading.</p>
   */
  readonly ariaLabel = input<string>();

  /**
   * Optional projected template rendered inside the header container.
   */
  readonly headerTpl = contentChild<string, TemplateRef<unknown>>('cardHeader', {
    read: TemplateRef,
  });

  /**
   * Optional projected template rendered inside the footer container.
   */
  readonly footerTpl = contentChild<string, TemplateRef<unknown>>('cardFooter', {
    read: TemplateRef,
  });

  /**
   * Indicates whether the card header should be rendered.
   *
   * <p>The header is shown when there is either a title or a projected header
   * template.</p>
   */
  get hasHeader(): boolean {
    return !!this.headerTpl() || !!this.title();
  }

  /**
   * Indicates whether the card footer should be rendered.
   */
  get hasFooter(): boolean {
    return !!this.footerTpl();
  }
}
