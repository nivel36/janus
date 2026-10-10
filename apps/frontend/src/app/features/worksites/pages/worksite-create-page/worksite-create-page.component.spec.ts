/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { FormComponent } from '../../../../shared/ui/form/form.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEventType } from 'keycloak-angular';

import { WorksiteFormComponent } from '../../components/worksite-form/worksite-form.component';
import { WorksiteApiService } from '../../services/worksite-api.service';
import { WorksiteCreatePageComponent } from './worksite-create-page.component';

describe('WorksiteCreatePageComponent', () => {
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
        { provide: Keycloak, useValue: { authenticated: false, tokenParsed: undefined } },
        {
          provide: KEYCLOAK_EVENT_SIGNAL,
          useValue: signal({ type: KeycloakEventType.KeycloakAngularNotInitialized }),
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorksiteCreatePageComponent);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  function formComponent(): WorksiteFormComponent {
    return fixture.debugElement.query(By.directive(WorksiteFormComponent)).componentInstance;
  }

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
    expect(formComponent().form.controls.code.hasError('duplicatedCode')).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('worksite.errors.duplicatedCode');
  });

  it('does not call the backend when the code format is invalid', async () => {
    formComponent().form.controls.code.setValue('BAD CODE');

    await fixture.whenStable();

    expect(worksiteApiService.findByCode).not.toHaveBeenCalled();
    expect(formComponent().form.controls.code.hasError('pattern')).toBe(true);
  });

  it('leaves the code valid when the backend returns not found', async () => {
    worksiteApiService.findByCode.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );

    formComponent().form.controls.code.setValue('MAD-HUB');

    await fixture.whenStable();

    expect(worksiteApiService.findByCode).toHaveBeenCalledWith('MAD-HUB');
    expect(formComponent().form.controls.code.errors).toBeNull();
  });

  it('does not save while the code check is pending', () => {
    const codeLookup = new Subject<never>();
    worksiteApiService.findByCode.mockReturnValue(codeLookup.asObservable());

    formComponent().form.setValue({
      code: 'MAD-HUB',
      name: 'Madrid Hub',
      timeZone: 'Europe/Madrid',
      scope: 'GLOBAL',
      description: null,
      address: null,
    });

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));

    expect(formComponent().form.pending).toBe(true);
    expect(worksiteApiService.create).not.toHaveBeenCalled();
  });

  it('refreshes the view when asynchronous code validation completes', async () => {
    const codeLookup = new Subject<never>();
    worksiteApiService.findByCode.mockReturnValue(codeLookup.asObservable());

    formComponent().form.setValue({
      code: 'MAD-HUB',
      name: 'Madrid Hub',
      timeZone: 'Europe/Madrid',
      scope: 'GLOBAL',
      description: null,
      address: null,
    });
    fixture.detectChanges();

    const saveButton = fixture.nativeElement.querySelector(
      'button[type="submit"]',
    ) as HTMLButtonElement;
    expect(saveButton.disabled).toBe(true);

    codeLookup.error(new HttpErrorResponse({ status: 404 }));
    await fixture.whenStable();

    expect(formComponent().form.valid).toBe(true);
    expect(saveButton.disabled).toBe(false);
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

    formComponent().form.setValue({
      code: 'MAD-HUB',
      name: 'Madrid Hub',
      timeZone: 'Europe/Madrid',
      scope: 'GLOBAL',
      description: ' Main office ',
      address: ' Calle Mayor 1 ',
    });

    await fixture.whenStable();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));

    expect(worksiteApiService.create).toHaveBeenCalledWith({
      code: 'MAD-HUB',
      name: 'Madrid Hub',
      timeZone: 'Europe/Madrid',
      scope: 'GLOBAL',
      description: 'Main office',
      address: 'Calle Mayor 1',
    });
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/worksites']);
  });

  it('updates save feedback after an asynchronous RxJS callback without manual detection', async () => {
    worksiteApiService.findByCode.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );
    const create = new Subject<never>();
    worksiteApiService.create.mockReturnValue(create);
    formComponent().form.setValue({
      code: 'MAD-HUB',
      name: 'Madrid Hub',
      timeZone: 'Europe/Madrid',
      scope: 'GLOBAL',
      description: null,
      address: null,
    });
    await fixture.whenStable();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    create.error(new Error('request failed'));
    await fixture.whenStable();

    expect(fixture.debugElement.query(By.directive(FormComponent)).componentInstance.saving()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('worksite.errors.create');
  });
});
