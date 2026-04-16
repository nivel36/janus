/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ClockComponent } from './clock.component';

describe('ClockComponent', () => {
  let fixture: ComponentFixture<ClockComponent>;
  let component: ClockComponent;

  beforeEach(async () => {
    vi.useFakeTimers();

    await TestBed.configureTestingModule({
      imports: [ClockComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ClockComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render a time string after init', () => {
    vi.spyOn(Date.prototype, 'toLocaleTimeString').mockReturnValue('10:15:30');

    fixture.detectChanges();
    vi.advanceTimersByTime(0);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.clock-time')?.textContent?.trim()).toBe('10:15:30');
  });

  it('should prefer locale input over navigator.language', () => {
    const spy = vi.spyOn(Date.prototype, 'toLocaleTimeString').mockReturnValue('x');

    fixture.componentRef.setInput('locale', 'fr-FR');
    fixture.detectChanges();
    vi.advanceTimersByTime(0);

    expect(spy).toHaveBeenCalledWith('fr-FR', expect.objectContaining({ hour12: false }));
  });

  it('should fall back to navigator.language when locale is not provided', () => {
    vi.spyOn(navigator, 'language', 'get').mockReturnValue('es-ES');
    const fmtSpy = vi.spyOn(Date.prototype, 'toLocaleTimeString').mockReturnValue('x');

    fixture.detectChanges();
    vi.advanceTimersByTime(0);

    expect(fmtSpy).toHaveBeenCalledWith('es-ES', expect.any(Object));
  });

  it('should respect 12-hour format when use12Hour is true', () => {
    const spy = vi.spyOn(Date.prototype, 'toLocaleTimeString').mockReturnValue('x');

    fixture.componentRef.setInput('use12Hour', true);
    fixture.detectChanges();
    vi.advanceTimersByTime(0);

    expect(spy).toHaveBeenCalledWith(expect.any(String), expect.objectContaining({ hour12: true }));
  });

  it('should update the time every second', () => {
    const spy = vi
      .spyOn(Date.prototype, 'toLocaleTimeString')
      .mockReturnValueOnce('t0')
      .mockReturnValueOnce('t1')
      .mockReturnValueOnce('t2')
      .mockReturnValueOnce('t3');

    fixture.detectChanges();
    vi.advanceTimersByTime(0);
    vi.advanceTimersByTime(1000);
    vi.advanceTimersByTime(1000);

    expect(spy).toHaveBeenCalledTimes(3);
  });

  it('should clear the interval on destroy', () => {
    const clearSpy = vi.spyOn(window, 'clearInterval');

    fixture.detectChanges();
    vi.advanceTimersByTime(0);
    component.ngOnDestroy();

    expect(clearSpy).toHaveBeenCalled();
  });

  it('should expose semantic and ARIA attributes for assistive technologies', () => {
    vi.spyOn(Date.prototype, 'toLocaleTimeString').mockReturnValue('10:15:30');

    fixture.componentRef.setInput('ariaLabel', 'Live clock');
    fixture.detectChanges();
    vi.advanceTimersByTime(0);
    fixture.detectChanges();

    const timeEl = fixture.nativeElement.querySelector('time.clock-time') as HTMLElement | null;

    expect(timeEl).not.toBeNull();
    expect(timeEl?.getAttribute('role')).toBe('timer');
    expect(timeEl?.getAttribute('aria-live')).toBe('off');
    expect(timeEl?.getAttribute('aria-atomic')).toBe('true');
    expect(timeEl?.getAttribute('aria-label')).toBe('Live clock');
    expect(timeEl?.getAttribute('datetime')).toMatch(
      /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/,
    );
  });
});
