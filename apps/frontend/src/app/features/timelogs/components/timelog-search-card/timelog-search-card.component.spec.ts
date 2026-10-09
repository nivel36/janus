import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { TimelogSearchCardComponent } from './timelog-search-card.component';

describe('TimelogSearchCardComponent', () => {
  let fixture: ComponentFixture<TimelogSearchCardComponent>;
  const preferences = signal<{ defaultTimezone: string } | null>({ defaultTimezone: 'Europe/Madrid' });
  beforeEach(async () => {
    preferences.set({ defaultTimezone: 'Europe/Madrid' });
    await TestBed.configureTestingModule({
      imports: [TimelogSearchCardComponent],
      providers: [provideTranslateService(), { provide: CurrentUserFacade, useValue: { preferences } }],
    }).compileComponents();
    fixture = TestBed.createComponent(TimelogSearchCardComponent);
    fixture.detectChanges();
  });
  function dates(from: string, to: string): void {
    const inputs = fixture.nativeElement.querySelectorAll('input') as NodeListOf<HTMLInputElement>;
    [from, to].forEach((value, index) => {
      inputs[index].value = value;
      inputs[index].dispatchEvent(new Event('input'));
    });
    fixture.detectChanges();
  }
  function submit(): void {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
  }
  it('searches only on submit and clears the interval with empty fields', () => {
    const emit = vi.spyOn(fixture.componentInstance.searchRequested, 'emit');
    dates('2026-10-01', '2026-10-01');
    expect(emit).not.toHaveBeenCalled();
    submit();
    expect(emit).toHaveBeenLastCalledWith({ start: '2026-09-30T22:00:00.000Z', end: '2026-10-01T22:00:00.000Z' });
    dates('', '');
    submit();
    expect(emit).toHaveBeenLastCalledWith(undefined);
  });
  it.each([['2026-10-01', ''], ['', '2026-10-01'], ['2026-10-02', '2026-10-01']])(
    'shows an accessible error for an invalid interval', (from, to) => {
      const emit = vi.spyOn(fixture.componentInstance.searchRequested, 'emit');
      dates(from, to);
      submit();
      expect(emit).not.toHaveBeenCalled();
      expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
      expect(fixture.nativeElement.querySelector('input').getAttribute('aria-invalid')).toBe('true');
    },
  );
  it('falls back to the browser timezone', () => {
    preferences.set(null);
    const emit = vi.spyOn(fixture.componentInstance.searchRequested, 'emit');
    dates('2026-10-01', '2026-10-01');
    submit();
    expect(emit).toHaveBeenCalledWith({ start: new Date(2026, 9, 1).toISOString(), end: new Date(2026, 9, 2).toISOString() });
  });
});
