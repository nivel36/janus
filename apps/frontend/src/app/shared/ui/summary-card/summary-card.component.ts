/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, input } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { IconDefinition } from '@fortawesome/fontawesome-svg-core';

@Component({
  selector: 'app-summary-card',
  standalone: true,
  imports: [FontAwesomeModule],
  templateUrl: './summary-card.component.html',
  styleUrl: './summary-card.component.css',
})
export class SummaryCardComponent {
  /**
   * Decorative icon rendered above the summary text.
   */
  readonly icon = input.required<IconDefinition>();

  /**
   * Short label that describes the summarized metric.
   */
  readonly label = input.required<string>();

  /**
   * Main metric value shown in the card.
   */
  readonly value = input.required<string | number>();

  /**
   * Extra CSS classes added to the card root element.
   */
  readonly styleClass = input<string>('');

  /**
   * Builds the complete CSS class list for the root card element.
   */
  get summaryCardClass(): string {
    const extraClass = this.styleClass().trim();
    return extraClass ? `summary-card ${extraClass}` : 'summary-card';
  }
}
