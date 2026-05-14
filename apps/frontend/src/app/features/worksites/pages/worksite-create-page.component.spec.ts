/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { WorksiteApiService } from '../services/worksite-api.service';
import { WorksiteCreatePageComponent } from './worksite-create-page.component';

describe('WorksiteCreatePageComponent', () => {
  let component: WorksiteCreatePageComponent;
  let fixture: ComponentFixture<WorksiteCreatePageComponent>;
  let worksiteApiService: {
    findByCode: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    worksiteApiService = {
      findByCode: vi.fn(),
      create: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [WorksiteCreatePageComponent],
      providers: [
        provideRouter([]),
        provideTranslateService(),
        { provide: WorksiteApiService, useValue: worksiteApiService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorksiteCreatePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('checks the code when the field loses focus', async () => {
    worksiteApiService.findByCode.mockReturnValue(
      of({
        code: 'BCN-HQ',
        name: 'Barcelona Headquarters',
        timeZone: 'Europe/Madrid',
        scope: 'GLOBAL',
        ownerEmployeeEmail: null,
        active: true,
      }),
    );

    const input = fixture.nativeElement.querySelector('#worksite-code') as HTMLInputElement;
    input.value = 'BCN-HQ';
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    fixture.detectChanges();

    await fixture.whenStable();
    fixture.detectChanges();

    expect(worksiteApiService.findByCode).toHaveBeenCalledWith('BCN-HQ');
    expect(component.form.controls.code.hasError('duplicatedCode')).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('worksite.errors.duplicatedCode');
  });

  it('does not call the backend when the code format is invalid', async () => {
    component.form.controls.code.setValue('BAD CODE');

    await fixture.whenStable();

    expect(worksiteApiService.findByCode).not.toHaveBeenCalled();
    expect(component.form.controls.code.hasError('pattern')).toBe(true);
  });

  it('leaves the code valid when the backend returns not found', async () => {
    worksiteApiService.findByCode.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );

    component.form.controls.code.setValue('MAD-HUB');

    await fixture.whenStable();

    expect(worksiteApiService.findByCode).toHaveBeenCalledWith('MAD-HUB');
    expect(component.form.controls.code.errors).toBeNull();
  });

  it('does not save while the code check is pending', () => {
    const codeLookup = new Subject<never>();
    worksiteApiService.findByCode.mockReturnValue(codeLookup.asObservable());

    component.form.setValue({
      code: 'MAD-HUB',
      name: 'Madrid Hub',
      timeZone: 'Europe/Madrid',
      scope: 'GLOBAL',
      description: null,
      address: null,
    });

    component.save();

    expect(component.form.pending).toBe(true);
    expect(worksiteApiService.create).not.toHaveBeenCalled();
  });

  it('saves optional address and description fields', async () => {
    worksiteApiService.findByCode.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );
    worksiteApiService.create.mockReturnValue(
      of({
        code: 'MAD-HUB',
        name: 'Madrid Hub',
        timeZone: 'Europe/Madrid',
        scope: 'GLOBAL',
        description: 'Main office',
        address: 'Calle Mayor 1',
        ownerEmployeeEmail: null,
        active: true,
      }),
    );

    component.form.setValue({
      code: 'MAD-HUB',
      name: 'Madrid Hub',
      timeZone: 'Europe/Madrid',
      scope: 'GLOBAL',
      description: ' Main office ',
      address: ' Calle Mayor 1 ',
    });

    await fixture.whenStable();

    component.save();

    expect(worksiteApiService.create).toHaveBeenCalledWith({
      code: 'MAD-HUB',
      name: 'Madrid Hub',
      timeZone: 'Europe/Madrid',
      scope: 'GLOBAL',
      description: 'Main office',
      address: 'Calle Mayor 1',
    });
  });
});
