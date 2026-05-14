/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { WorksiteApiService } from '../services/worksite-api.service';
import { WorksiteEditPageComponent } from './worksite-edit-page.component';

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

    await TestBed.configureTestingModule({
      imports: [WorksiteEditPageComponent],
      providers: [
        provideTranslateService(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'code' ? 'BCN-HQ' : null),
              },
            },
          },
        },
        { provide: Router, useValue: router },
        { provide: WorksiteApiService, useValue: worksiteApiService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorksiteEditPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the worksite and keeps the code field immutable', () => {
    expect(worksiteApiService.findByCode).toHaveBeenCalledWith('BCN-HQ');
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
});
