/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, DestroyRef, EventEmitter, Output, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { debounceTime, distinctUntilChanged, map } from 'rxjs';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faMagnifyingGlass } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe, FontAwesomeModule],
  templateUrl: './search-bar.component.html',
  styleUrl: './search-bar.component.css',
})
export class SearchBarComponent {
  private readonly destroyRef = inject(DestroyRef);

  readonly faMagnifyingGlass = faMagnifyingGlass;

  protected readonly inputId = 'search-bar-input';
  protected readonly helpTextId = 'search-bar-help';

  protected readonly queryControl = new FormControl('', { nonNullable: true });

  @Output() readonly queryChange = new EventEmitter<string>();

  constructor() {
    this.queryControl.valueChanges
      .pipe(
        map((value) => value.trim()),
        debounceTime(250),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((query) => this.queryChange.emit(query));
  }

  protected submitCurrentQuery(): void {
    this.queryChange.emit(this.queryControl.value.trim());
  }
}
