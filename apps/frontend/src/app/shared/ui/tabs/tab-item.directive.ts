/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Directive, TemplateRef, inject, input } from '@angular/core';

/**
 * Tab panel template definition consumed by {@link TabsComponent}.
 */
@Directive({
  selector: 'ng-template[appTabItem]',
  standalone: true,
})
export class TabItemDirective {
  /**
   * Visible tab label.
   */
  readonly label = input.required<string>({ alias: 'appTabItem' });

  readonly template = inject(TemplateRef<unknown>);
}
