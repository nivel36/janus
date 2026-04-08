/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { NgTemplateOutlet } from '@angular/common';
import {
  AfterContentInit,
  Component,
  ContentChild,
  Input,
  TemplateRef,
} from '@angular/core';

/**
 * Reusable card container with optional header and footer template slots.
 *
 * The component exposes ARIA metadata so the rendered `<section>` can be
 * announced as a named region by assistive technologies.
 */
@Component({
  selector: 'app-card',
  standalone: true,
  imports: [NgTemplateOutlet],
  templateUrl: './card.component.html',
  styleUrls: ['./card.component.css'],
})
export class CardComponent implements AfterContentInit {
  private static nextTitleId = 0;
  /**
   * Plain text title rendered in the card header.
   *
   * When present, this title is also used to label the card region through
   * `aria-labelledby`.
   */
  @Input() title: string | null = null;
  /** Extra CSS classes applied to the outer card container. */
  @Input() styleClass = '';
  /** Extra CSS classes applied to the `<h2>` title element. */
  @Input() titleClass = '';
  /** Extra CSS classes applied to the card header element. */
  @Input() headerClass = '';
  /** Extra CSS classes applied to the card footer element. */
  @Input() footerClass = '';
  /**
   * Accessible name used when no title is rendered.
   *
   * Use this input to provide a meaningful label for screen readers when the
   * card has no visible heading.
   */
  @Input() ariaLabel: string | null = null;

  /** Stable id used by `aria-labelledby` when a title exists. */
  readonly titleId = `card-title-${CardComponent.nextTitleId++}`;

  /** Optional projected template rendered inside the header container. */
  @ContentChild('cardHeader', { read: TemplateRef }) headerTpl?: TemplateRef<unknown>;
  /** Optional projected template rendered inside the footer container. */
  @ContentChild('cardFooter', { read: TemplateRef }) footerTpl?: TemplateRef<unknown>;

  /** Indicates whether the card header should be rendered. */
  hasHeader = false;
  /** Indicates whether the card footer should be rendered. */
  hasFooter = false;

  /**
   * Computes the visibility state of optional card regions after content
   * projection resolves.
   */
  ngAfterContentInit(): void {
    this.hasHeader = !!this.headerTpl || !!this.title;
    this.hasFooter = !!this.footerTpl;
  }
}
