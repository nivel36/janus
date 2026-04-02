/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { EmployeeCardComponent } from './employee-card.component';

describe('EmployyeCardComponent', () => {
  let component: EmployeeCardComponent;
  let fixture: ComponentFixture<EmployeeCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployeeCardComponent],
      providers: [provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeCardComponent);
    component = fixture.componentInstance;
    component.fullName = 'Jane Doe';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders the provided user data', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Jane Doe');
    expect(element.textContent).toContain('Barcelona Headquarters');
    expect(element.textContent).toContain('9:00 - 17:30');
  });
});
