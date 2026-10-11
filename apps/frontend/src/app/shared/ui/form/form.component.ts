/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  booleanAttribute,
  ChangeDetectorRef,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormGroup, FormResetEvent, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { defer, finalize, map, Observable, startWith, switchMap, tap } from 'rxjs';

import { ButtonComponent } from '../button/button.component';
import { MessageComponent } from '../message/message.component';

/**
 * Common create/edit form. Project fields bound with [formControl] and supply a
 * cold save operation. The component owns validation, the request subscription,
 * busy state and save feedback; the page owns payloads and success navigation.
 */
@Component({
  selector: 'app-form',
  imports: [ReactiveFormsModule, TranslatePipe, ButtonComponent, MessageComponent],
  templateUrl: './form.component.html',
  styleUrl: './form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormComponent<T = unknown> {
  private readonly destroyRef = inject(DestroyRef);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  readonly form = input.required<FormGroup>();
  readonly save = input.required<() => Observable<T>>();
  readonly saveErrorKey = input.required<string | ((error: unknown) => string)>();
  readonly errorKey = input('');
  readonly disabled = input(false, { transform: booleanAttribute });
  readonly saveDisabled = input(false, { transform: booleanAttribute });
  readonly showSave = input(true, { transform: booleanAttribute });
  readonly cancelLink = input<string | readonly unknown[]>();
  readonly saveLabel = input('actions.save');
  readonly cancelLabel = input('actions.cancel');

  readonly saved = output<T>();
  readonly cancelled = output<void>();
  readonly saving = signal(false);
  readonly saveError = signal('');

  // Subscribe again when the form instance changes and refresh OnPush views
  // when asynchronous validation finishes.
  protected readonly status = toSignal(
    toObservable(this.form).pipe(
      switchMap((form) => {
        this.saveError.set('');
        return form.events.pipe(
          tap((event) => {
            if (event instanceof FormResetEvent) {
              this.saveError.set('');
            }
          }),
          map(() => form.status),
          startWith(form.status),
        );
      }),
      // Projected field messages belong to the owning page's OnPush view.
      tap(() => this.changeDetectorRef.markForCheck()),
    ),
  );

  submit(): void {
    if (this.saving() || this.disabled() || this.saveDisabled() || !this.showSave()) {
      return;
    }
    const form = this.form();
    if (!form.valid) {
      form.markAllAsTouched();
      return;
    }

    this.saveError.set('');
    this.saving.set(true);
    // defer also turns synchronous failures while constructing the request
    // into recoverable errors, so the form never remains stuck saving.
    defer(() => this.save()())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (result) => this.saved.emit(result),
        error: (error: unknown) => {
          const key = this.saveErrorKey();
          this.saveError.set(typeof key === 'function' ? key(error) : key);
        },
      });
  }

  cancel(): void {
    if (!this.saving()) {
      this.cancelled.emit();
    }
  }
}
