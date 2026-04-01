/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { CommonModule } from '@angular/common';
import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { Observable, catchError, debounceTime, distinctUntilChanged, filter, finalize, from, of, switchMap, tap } from 'rxjs';

@Component({
  selector: 'app-autocomplete-textbox',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './autocomplete-textbox.component.html',
  styleUrl: './autocomplete-textbox.component.css',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AutocompleteTextboxComponent),
      multi: true,
    },
  ],
})
export class AutocompleteTextboxComponent<T = unknown> implements OnInit, ControlValueAccessor {
  @Input({ required: true })
  searchMethod!: (query: string) => Observable<T[]> | Promise<T[]>;

  @Input()
  displayWith: (option: T) => string = (option: T) => String(option);

  @Input() placeholder = '';
  @Input() debounceMs = 350;

  @Output() selectedChange = new EventEmitter<T | null>();

  readonly minChars = 3;
  textControl = new FormControl('', { nonNullable: true });

  results: T[] = [];
  selectedValue: T | null = null;
  isLoading = false;
  disabled = false;

  private onChange: (value: T | null) => void = () => {};
  private onTouched: () => void = () => {};

  ngOnInit(): void {
    this.textControl.valueChanges
      .pipe(
        distinctUntilChanged(),
        tap((value) => {
          if (value.trim().length <= this.minChars || this.hasSelection) {
            this.results = [];
          }
        }),
        filter((value) => value.trim().length > this.minChars && !this.hasSelection),
        debounceTime(this.debounceMs),
        switchMap((query) => {
          this.isLoading = true;
          return from(this.searchMethod(query.trim())).pipe(
            catchError(() => of([] as T[])),
            finalize(() => {
              this.isLoading = false;
            }),
          );
        }),
      )
      .subscribe({
        next: (results) => {
          this.results = results;
        },
        error: () => {
          this.results = [];
        },
      });
  }

  get hasSelection(): boolean {
    return this.selectedValue !== null;
  }

  onSelect(option: T): void {
    this.selectedValue = option;
    this.textControl.setValue(this.displayWith(option), { emitEvent: false });
    this.results = [];
    this.selectedChange.emit(option);
    this.onChange(option);
    this.onTouched();
  }

  clearSelection(): void {
    if (this.disabled) {
      return;
    }

    this.selectedValue = null;
    this.results = [];
    this.textControl.setValue('', { emitEvent: false });
    this.selectedChange.emit(null);
    this.onChange(null);
    this.onTouched();
  }

  writeValue(value: T | null): void {
    this.selectedValue = value;

    if (value === null) {
      this.textControl.setValue('', { emitEvent: false });
      return;
    }

    this.textControl.setValue(this.displayWith(value), { emitEvent: false });
  }

  registerOnChange(fn: (value: T | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;

    if (isDisabled) {
      this.textControl.disable({ emitEvent: false });
      return;
    }

    this.textControl.enable({ emitEvent: false });
  }
}
