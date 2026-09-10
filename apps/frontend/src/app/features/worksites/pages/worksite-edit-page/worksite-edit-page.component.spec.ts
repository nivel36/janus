/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap, Router } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEventType } from 'keycloak-angular';

import { WorksiteApiService } from '../../services/worksite-api.service';
import { WorksiteEditPageComponent } from './worksite-edit-page.component';
import { ACTIVE_SCREEN_HTTP_RETRY_POLICY } from '../../../../core/http/http-retry.interceptor';

describe('WorksiteEditPageComponent', () => {
  let component: WorksiteEditPageComponent;
  let fixture: ComponentFixture<WorksiteEditPageComponent>;
  let worksiteApiService: {
    findByCode: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let router: {
    navigate: ReturnType<typeof vi.fn>;
  };
  let paramMap: BehaviorSubject<ParamMap>;

  beforeEach(async () => {
    worksiteApiService = {
      findByCode: vi.fn().mockReturnValue(
        of({
          code: 'BCN-HQ',
          name: 'Barcelona Headquarters',
          timeZone: 'Europe/Madrid',
          scope: 'GLOBAL',
          description: 'Main office',
          address: 'Carrer de la Marina',
          ownerEmployeeEmail: 'owner@example.com',
          active: true,
        }),
      ),
      update: vi.fn(),
    };
    router = {
      navigate: vi.fn(),
    };
    paramMap = new BehaviorSubject(convertToParamMap({ code: 'BCN-HQ' }));

    await TestBed.configureTestingModule({
      imports: [WorksiteEditPageComponent],
      providers: [
        provideTranslateService(),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap,
          },
        },
        { provide: Router, useValue: router },
        { provide: WorksiteApiService, useValue: worksiteApiService },
        { provide: Keycloak, useValue: { authenticated: false, tokenParsed: undefined } },
        {
          provide: KEYCLOAK_EVENT_SIGNAL,
          useValue: signal({ type: KeycloakEventType.KeycloakAngularNotInitialized }),
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorksiteEditPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the worksite and keeps the code field immutable', async () => {
    await fixture.whenStable();

    expect(worksiteApiService.findByCode).toHaveBeenCalledWith(
      'BCN-HQ',
      ACTIVE_SCREEN_HTTP_RETRY_POLICY,
    );
    expect(component.form.controls.code.disabled).toBe(true);
    expect(component.form.getRawValue().code).toBe('BCN-HQ');
  });

  it('updates editable fields without sending a new code', () => {
    worksiteApiService.update.mockReturnValue(
      of({
        code: 'BCN-HQ',
        name: 'Barcelona HQ',
        timeZone: 'Europe/Madrid',
        scope: 'ASSIGNED',
        description: null,
        address: null,
        ownerEmployeeEmail: 'owner@example.com',
        active: true,
      }),
    );

    component.form.patchValue({
      code: 'MAD-HUB',
      name: ' Barcelona HQ ',
      scope: 'ASSIGNED',
      description: ' ',
      address: ' ',
    });

    component.save();

    expect(worksiteApiService.update).toHaveBeenCalledWith('BCN-HQ', {
      name: 'Barcelona HQ',
      timeZone: 'Europe/Madrid',
      scope: 'ASSIGNED',
      description: null,
      address: null,
      ownerEmployeeEmail: 'owner@example.com',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/worksites', 'BCN-HQ']);
  });

  it('updates save feedback after an asynchronous RxJS callback without manual detection', async () => {
    const update = new Subject<never>();
    worksiteApiService.update.mockReturnValue(update);
    component.form.patchValue({ name: 'Barcelona HQ' });

    component.save();
    update.error(new Error('request failed'));
    await fixture.whenStable();

    expect(component.saving()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('worksite.errors.update');
  });

  it('shows the load error without reading the resource value', async () => {
    worksiteApiService.findByCode.mockReturnValue(
      throwError(() => new Error('request failed')),
    );

    paramMap.next(convertToParamMap({ code: 'MISSING' }));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('worksite.detailLoadError');
  });

  it('cancels the previous load and updates only the worksite for the latest route code', async () => {
    const firstLoad = new Subject<{
      code: string;
      name: string;
      timeZone: string;
      scope: 'GLOBAL';
      description: null;
      address: null;
      ownerEmployeeEmail: string;
      active: boolean;
    }>();
    const secondLoad = new Subject<{
      code: string;
      name: string;
      timeZone: string;
      scope: 'ASSIGNED';
      description: null;
      address: null;
      ownerEmployeeEmail: string;
      active: boolean;
    }>();
    worksiteApiService.findByCode.mockImplementation((code: string) =>
      code === 'FIRST' ? firstLoad : secondLoad,
    );
    worksiteApiService.update.mockImplementation((_code: string, worksite: object) =>
      of({ code: 'SECOND', ...worksite }),
    );
    worksiteApiService.findByCode.mockClear();

    paramMap.next(convertToParamMap({ code: 'FIRST' }));
    fixture.detectChanges();
    expect(firstLoad.observed).toBe(true);
    paramMap.next(convertToParamMap({ code: 'SECOND' }));
    fixture.detectChanges();
    expect(firstLoad.observed).toBe(false);
    firstLoad.next({
      code: 'FIRST',
      name: 'First worksite',
      timeZone: 'Europe/Madrid',
      scope: 'GLOBAL',
      description: null,
      address: null,
      ownerEmployeeEmail: 'first@example.com',
      active: true,
    });
    secondLoad.next({
      code: 'SECOND',
      name: 'Second worksite',
      timeZone: 'Europe/Madrid',
      scope: 'ASSIGNED',
      description: null,
      address: null,
      ownerEmployeeEmail: 'second@example.com',
      active: true,
    });
    secondLoad.complete();
    await fixture.whenStable();

    component.save();

    expect(worksiteApiService.findByCode).toHaveBeenCalledTimes(2);
    expect(worksiteApiService.findByCode).toHaveBeenNthCalledWith(
      1,
      'FIRST',
      ACTIVE_SCREEN_HTTP_RETRY_POLICY,
    );
    expect(worksiteApiService.findByCode).toHaveBeenNthCalledWith(
      2,
      'SECOND',
      ACTIVE_SCREEN_HTTP_RETRY_POLICY,
    );
    expect(component.form.getRawValue().code).toBe('SECOND');
    expect(worksiteApiService.update).toHaveBeenCalledTimes(1);
    expect(worksiteApiService.update).toHaveBeenCalledWith('SECOND', {
      name: 'Second worksite',
      timeZone: 'Europe/Madrid',
      scope: 'ASSIGNED',
      description: null,
      address: null,
      ownerEmployeeEmail: 'second@example.com',
    });
  });
});
