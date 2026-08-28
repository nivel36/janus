/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Highlightable, ListKeyManagerOption } from '@angular/cdk/a11y';
import {
  Directive,
  ElementRef,
  HostBinding,
  InjectionToken,
  OnDestroy,
  inject,
} from '@angular/core';

/** Registration contract that keeps the option directive independent of its host component. */
export interface AutocompleteOptionController {
  registerOption(option: AutocompleteOptionDirective): void;
  unregisterOption(option: AutocompleteOptionDirective): void;
}

export const AUTOCOMPLETE_OPTION_CONTROLLER = new InjectionToken<AutocompleteOptionController>(
  'AUTOCOMPLETE_OPTION_CONTROLLER',
);

/** Lightweight option adapter used by the CDK active-descendant key manager. */
@Directive({
  selector: '[appAutocompleteOption]',
  standalone: true,
})
export class AutocompleteOptionDirective implements ListKeyManagerOption, Highlightable, OnDestroy {
  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly controller = inject(AUTOCOMPLETE_OPTION_CONTROLLER);

  /** Options rendered by this control are always available for navigation. */
  readonly disabled = false;

  constructor() {
    this.controller.registerOption(this);
  }

  ngOnDestroy(): void {
    this.controller.unregisterOption(this);
  }

  /** Whether this option is currently managed as the active descendant. */
  @HostBinding('class.autocomplete__option--active')
  active = false;

  @HostBinding('attr.aria-selected')
  get ariaSelected(): string {
    return String(this.active);
  }

  /** DOM id referenced by the combobox's aria-activedescendant attribute. */
  get id(): string {
    return this.elementRef.nativeElement.id;
  }

  setActiveStyles(): void {
    this.active = true;
    this.elementRef.nativeElement.scrollIntoView({ block: 'nearest' });
  }

  setInactiveStyles(): void {
    this.active = false;
  }
}
