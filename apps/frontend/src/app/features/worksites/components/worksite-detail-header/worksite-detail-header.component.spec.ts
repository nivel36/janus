/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { WorksiteDetailHeaderComponent } from './worksite-detail-header.component';

describe('WorksiteDetailHeaderComponent', () => {
  let fixture: ComponentFixture<WorksiteDetailHeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorksiteDetailHeaderComponent],
      providers: [provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(WorksiteDetailHeaderComponent);
    fixture.componentRef.setInput('worksite', {
      code: 'BCN-HQ',
      name: 'Barcelona Headquarters',
      timeZone: 'Europe/Madrid',
      scope: 'GLOBAL',
      description: 'Main office',
      address: 'Carrer de la Marina',
      ownerEmployeeEmail: 'owner@example.com',
      active: true,
    });
    fixture.detectChanges();
  });

  it('uses the corrected selector', () => {
    expect((fixture.nativeElement as HTMLElement).matches('app-worksite-detail-header')).toBe(true);
  });

  it('renders the worksite identity', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Barcelona Headquarters');
    expect(element.textContent).toContain('BCN-HQ');
  });
});
