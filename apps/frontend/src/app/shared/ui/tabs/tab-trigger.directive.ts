/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { FocusableOption } from '@angular/cdk/a11y';
import { Directive, ElementRef, inject } from '@angular/core';

/** A tab trigger that can be managed by the CDK's keyboard focus manager. */
@Directive({
  selector: 'button[appTabTrigger]',
  standalone: true,
})
export class TabTriggerDirective implements FocusableOption {
  private readonly elementRef = inject<ElementRef<HTMLButtonElement>>(ElementRef);

  focus(): void {
    this.elementRef.nativeElement.focus();
  }
}
