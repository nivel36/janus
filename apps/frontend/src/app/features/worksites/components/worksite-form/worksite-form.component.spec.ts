/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { Worksite } from '../../models/worksite';
import { WorksiteApiService } from '../../services/worksite-api.service';
import { WorksiteFormComponent } from './worksite-form.component';

describe('WorksiteFormComponent', () => {
  let fixture: ComponentFixture<WorksiteFormComponent>;
  let component: WorksiteFormComponent;
  let findByCode: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    findByCode = vi.fn().mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );
    await TestBed.configureTestingModule({
      imports: [WorksiteFormComponent],
      providers: [
        provideRouter([]),
        provideTranslateService(),
        { provide: WorksiteApiService, useValue: { findByCode } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorksiteFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('save', () => of(null));
    fixture.componentRef.setInput('saveErrorKey', 'worksite.errors.create');
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('preserves the initial values and scope options in both modes', async () => {
    for (const editMode of [false, true]) {
      fixture.componentRef.setInput('editMode', editMode);
      await fixture.whenStable();

      expect(component.form.getRawValue()).toEqual({
        code: '',
        name: '',
        timeZone: 'Europe/Madrid',
        scope: 'GLOBAL',
        description: null,
        address: null,
      });
      expect(component.form.controls.code.disabled).toBe(editMode);
      expect(component.scopeOptions.map((option) => option.value)).toEqual(['GLOBAL', 'ASSIGNED']);
    }
  });

  it('preserves the shared validation rules in both modes', async () => {
    for (const editMode of [false, true]) {
      fixture.componentRef.setInput('editMode', editMode);
      await fixture.whenStable();
      const controls = component.form.controls;

      controls.name.setValue('');
      expect(controls.name.hasError('required')).toBe(true);
      controls.name.setValue('invalid/name');
      expect(controls.name.hasError('pattern')).toBe(true);
      controls.name.setValue('a'.repeat(251));
      expect(controls.name.hasError('maxlength')).toBe(true);

      controls.timeZone.setValue('a'.repeat(65));
      expect(controls.timeZone.hasError('maxlength')).toBe(true);
      controls.timeZone.setValue(null);
      expect(controls.timeZone.hasError('required')).toBe(true);
      controls.scope.setValue(null as never);
      expect(controls.scope.hasError('required')).toBe(true);

      controls.description.setValue('Información\n追加 😀');
      expect(controls.description.valid).toBe(true);
      for (const control of [controls.description, controls.address]) {
        control.setValue('before\u0000after');
        expect(control.hasError('pattern')).toBe(true);
        control.setValue('a'.repeat(501));
        expect(control.hasError('maxlength')).toBe(true);
      }
    }
  });

  it('populates and resets edit fields without checking code uniqueness', async () => {
    const worksite: Worksite = {
      code: 'BCN-HQ',
      name: 'Barcelona HQ',
      timeZone: 'Europe/Paris',
      scope: 'ASSIGNED',
      description: 'Main office',
      address: 'Carrer de la Marina',
      ownerEmployeeEmail: null,
      active: true,
    };
    fixture.componentRef.setInput('editMode', true);
    fixture.componentRef.setInput('worksite', worksite);
    await fixture.whenStable();

    expect(component.form.getRawValue()).toEqual({
      code: worksite.code,
      name: worksite.name,
      timeZone: worksite.timeZone,
      scope: worksite.scope,
      description: worksite.description,
      address: worksite.address,
    });
    expect(component.form.controls.code.disabled).toBe(true);
    expect(findByCode).not.toHaveBeenCalled();

    component.form.controls.name.markAsTouched();
    fixture.componentRef.setInput('worksite', { ...worksite, code: 'MAD-HUB', name: 'Madrid Hub' });
    await fixture.whenStable();

    expect(component.form.getRawValue().code).toBe('MAD-HUB');
    expect(component.form.controls.name.value).toBe('Madrid Hub');
    expect(component.form.controls.name.touched).toBe(false);
    expect(findByCode).not.toHaveBeenCalled();
  });
});
