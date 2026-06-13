/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, DestroyRef, OnInit, inject, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { debounceTime, distinctUntilChanged, filter, map } from 'rxjs';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faMagnifyingGlass } from '@fortawesome/free-solid-svg-icons';

import { ButtonComponent } from '../button/button.component';

/**
 * Standalone search bar component.
 */
@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe, FontAwesomeModule, ButtonComponent],
  templateUrl: './search-bar.component.html',
  styleUrl: './search-bar.component.css',
})
export class SearchBarComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Debounce time, in milliseconds, applied to user typing.
   */
  readonly debounceMs = input(350);

  /**
   * Minimum number of non-empty characters required to launch a search.
   */
  readonly minChars = input(3);

  /**
   * Emits the effective query that has started processing.
   */
  readonly queryChange = output<string>();

  /**
   * Icon displayed in the submit button.
   */
  readonly faMagnifyingGlass = faMagnifyingGlass;

  /**
   * DOM identifier of the main input element.
   */
  protected readonly inputId = 'search-bar-input';

  /**
   * DOM identifier of the hidden helper text associated with the input.
   */
  protected readonly helpTextId = 'search-bar-help';

  /**
   * Reactive control bound to the visible search input.
   */
  protected readonly queryControl = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    this.validateInputs();

    this.queryControl.valueChanges
      .pipe(
        map((value) => value.trim()),
        debounceTime(this.debounceMs()),
        distinctUntilChanged(),
        filter((query) => query.length === 0 || query.length >= this.minChars()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((query) => this.queryChange.emit(query));
  }

  /**
   * Emits the current query when the user explicitly submits it.
   */
  protected submitCurrentQuery(): void {
    const query = this.queryControl.value.trim();

    if (query.length === 0 || query.length >= this.minChars()) {
      this.queryChange.emit(query);
    }
  }

  /**
   * Validates required inputs and basic invariants.
   */
  private validateInputs(): void {
    if (this.debounceMs() < 0) {
      throw new Error('SearchBarComponent: debounceMs cannot be negative.');
    }

    if (this.minChars() < 0) {
      throw new Error('SearchBarComponent: minChars cannot be negative.');
    }
  }
}
