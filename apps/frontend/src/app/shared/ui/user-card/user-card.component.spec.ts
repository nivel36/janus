/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { UserCardComponent } from './user-card.component';

describe('UserCardComponent', () => {
  let component: UserCardComponent;
  let fixture: ComponentFixture<UserCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserCardComponent],
      providers: [provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(UserCardComponent);
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
