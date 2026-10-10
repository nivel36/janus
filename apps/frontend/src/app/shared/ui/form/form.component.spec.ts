/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { EMPTY, of, Subject } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { InputComponent } from '../input/input.component';
import { FormComponent } from './form.component';

@Component({
  imports: [ReactiveFormsModule, FormComponent, InputComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-form
      [form]="form()"
      [save]="save"
      [saveErrorKey]="errorKey()"
      [disabled]="disabled()"
      [showSave]="showSave()"
      [cancelLink]="cancelLink()"
      (saved)="saved($event)"
      (cancelled)="cancelled()"
    >
      <app-input inputId="name" [formControl]="form().controls.name" />
      <app-input inputId="code" [formControl]="form().controls.code" />
      @if (form().controls.name.touched && form().controls.name.invalid) {
        <span class="field-error">Invalid name</span>
      }
    </app-form>
  `,
})
class TestHostComponent {
  readonly form = signal(new FormGroup({
    name: new FormControl('Name', { nonNullable: true, validators: Validators.required }),
    code: new FormControl({ value: 'CODE', disabled: true }, { nonNullable: true }),
  }));
  response = new Subject<string>();
  readonly save = vi.fn(() => this.response.asObservable());
  readonly saved = vi.fn();
  readonly cancelled = vi.fn();
  readonly errorKey = signal<string | ((error: unknown) => string)>('save.failed');
  readonly disabled = signal(false);
  readonly showSave = signal(true);
  readonly cancelLink = signal<string | undefined>(undefined);
}

describe('FormComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;
  let component: FormComponent<string>;
  const submit = () => fixture.nativeElement.querySelector('form').dispatchEvent(
    new Event('submit', { bubbles: true, cancelable: true }),
  );
  const saveButton = (): HTMLButtonElement => fixture.nativeElement.querySelector('button[type="submit"]');
  const cancelButton = (): HTMLButtonElement => fixture.nativeElement.querySelector('button[type="button"]');

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [provideRouter([]), provideTranslateService()],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
    component = fixture.debugElement.query(By.directive(FormComponent)).componentInstance;
  });

  it('submits projected fields and locks editing and cancellation until completion', async () => {
    const input = fixture.nativeElement.querySelector('#name') as HTMLInputElement;
    input.value = 'Changed';
    input.dispatchEvent(new Event('input'));
    expect(host.form().controls.name.value).toBe('Changed');
    submit();
    submit();
    await fixture.whenStable();

    expect(host.save).toHaveBeenCalledOnce();
    expect(saveButton().disabled).toBe(true);
    expect(saveButton().textContent).toContain('actions.saving');
    expect(cancelButton().disabled).toBe(true);
    expect(input.matches(':disabled')).toBe(true);
    expect(fixture.nativeElement.querySelector('form').getAttribute('aria-busy')).toBe('true');
    component.cancel();
    expect(host.cancelled).not.toHaveBeenCalled();

    host.response.next('saved entity');
    host.response.complete();
    await fixture.whenStable();
    expect(host.saved).toHaveBeenCalledWith('saved entity');
    expect(saveButton().disabled).toBe(false);
    expect(cancelButton().disabled).toBe(false);
    expect(input.matches(':disabled')).toBe(false);
    expect(host.form().controls.code.disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('#code').disabled).toBe(true);
  });

  it('marks invalid fields as touched and blocks native submit events', async () => {
    host.form().controls.name.setValue('');
    submit();
    await fixture.whenStable();
    expect(host.save).not.toHaveBeenCalled();
    expect(host.form().controls.name.touched).toBe(true);
    expect(saveButton().disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('.field-error')).not.toBeNull();
  });

  it('waits for asynchronous validators and refreshes projected validation feedback', async () => {
    const validation = new Subject<{ unavailable: boolean } | null>();
    host.form().controls.name.setAsyncValidators(() => validation);
    host.form().controls.name.markAsTouched();
    host.form().controls.name.updateValueAndValidity();
    await fixture.whenStable();
    expect(saveButton().disabled).toBe(true);
    submit();
    expect(host.save).not.toHaveBeenCalled();

    validation.next({ unavailable: true });
    validation.complete();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.field-error')).not.toBeNull();
    expect(saveButton().disabled).toBe(true);

    host.form().controls.name.setAsyncValidators(() => of(null));
    host.form().controls.name.updateValueAndValidity();
    await fixture.whenStable();
    expect(saveButton().disabled).toBe(false);
    expect(fixture.nativeElement.querySelector('.field-error')).toBeNull();
  });

  it('preserves values after an error, renders the mapped error and clears it on retry', async () => {
    host.errorKey.set(() => 'save.conflict');
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges();
    submit();
    host.response.error(new Error('Conflict'));
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('save.conflict');
    expect(host.form().getRawValue()).toEqual({ name: 'Name', code: 'CODE' });
    expect(saveButton().disabled).toBe(false);
    expect(host.saved).not.toHaveBeenCalled();

    host.response = new Subject<string>();
    submit();
    await fixture.whenStable();
    expect(host.save).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });

  it('recovers when the save factory throws synchronously or completes without a value', async () => {
    host.save.mockImplementationOnce(() => { throw new Error('Failed'); });
    submit();
    await fixture.whenStable();
    expect(component.saving()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('save.failed');
    host.save.mockReturnValueOnce(EMPTY);
    submit();
    await fixture.whenStable();
    expect(component.saving()).toBe(false);
    expect(host.saved).not.toHaveBeenCalled();
  });

  it('clears previous save feedback when the form is reset for another entity', async () => {
    submit();
    host.response.error(new Error('Failed'));
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('save.failed');

    host.form().reset({ name: 'Another entity', code: 'OTHER' });
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });

  it('cancels the save subscription when destroyed', () => {
    submit();
    expect(host.response.observed).toBe(true);
    fixture.destroy();
    expect(host.response.observed).toBe(false);
    host.response.next('late result');
    expect(host.saved).not.toHaveBeenCalled();
  });

  it('disables link cancellation during saving and restores its destination afterwards', async () => {
    host.cancelLink.set('/entities');
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/entities"]')).not.toBeNull();
    submit();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('a')).toBeNull();
    expect(cancelButton().disabled).toBe(true);
    host.response.complete();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('a[href="/entities"]')).not.toBeNull();
  });

  it('emits cancellation without submitting', () => {
    cancelButton().click();
    expect(host.cancelled).toHaveBeenCalledOnce();
    expect(host.save).not.toHaveBeenCalled();
  });

  it('blocks submission while externally disabled, hidden or the form model is disabled', () => {
    host.disabled.set(true);
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges();
    submit();
    expect(saveButton().disabled).toBe(true);
    expect(cancelButton().disabled).toBe(false);
    host.disabled.set(false);
    host.showSave.set(false);
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges();
    submit();
    expect(saveButton()).toBeNull();
    host.showSave.set(true);
    host.form().disable();
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges();
    submit();
    expect(host.save).not.toHaveBeenCalled();
  });

  it('uses the current form after the form instance is replaced', async () => {
    host.form.set(new FormGroup({
      name: new FormControl('', { nonNullable: true, validators: Validators.required }),
      code: new FormControl({ value: 'OTHER', disabled: true }, { nonNullable: true }),
    }));
    fixture.changeDetectorRef.markForCheck();
    await fixture.whenStable();
    expect(saveButton().disabled).toBe(true);
    host.form().controls.name.setValue('Other');
    await fixture.whenStable();
    expect(saveButton().disabled).toBe(false);
    submit();
    expect(host.save).toHaveBeenCalledOnce();
  });
});
