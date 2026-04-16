/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { PaginatorComponent } from './paginator.component';

describe('PaginatorComponent', () => {
  let fixture: ComponentFixture<PaginatorComponent>;
  let component: PaginatorComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaginatorComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PaginatorComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('totalItems', 95);
    fixture.componentRef.setInput('pageSize', 10);
    fixture.componentRef.setInput('currentPage', 1);
    fixture.componentRef.setInput('ariaLabel', 'Pagination controls');

    fixture.detectChanges();
  });

  function getButtons(): HTMLButtonElement[] {
    return fixture.debugElement
      .queryAll(By.css('button'))
      .map((debugElement) => debugElement.nativeElement as HTMLButtonElement);
  }

  function getSummaryText(): string {
    return (fixture.debugElement.query(By.css('.paginator-text')).nativeElement as HTMLElement)
      .textContent
      ?.trim() ?? '';
  }

  it('should render first-page summary and disable previous button', () => {
    const [previousButton, nextButton] = getButtons();

    expect(getSummaryText()).toBe('1-10 of 95');
    expect(previousButton.disabled).toBe(true);
    expect(nextButton.disabled).toBe(false);
  });

  it('should emit next page when clicking next button', () => {
    const pageRequestedSpy = vi.fn();
    component.pageRequested.subscribe(pageRequestedSpy);

    const [, nextButton] = getButtons();
    nextButton.click();

    expect(pageRequestedSpy).toHaveBeenCalledTimes(1);
    expect(pageRequestedSpy).toHaveBeenCalledWith(2);
  });

  it('should emit previous page when clicking previous button', () => {
    const pageRequestedSpy = vi.fn();
    component.pageRequested.subscribe(pageRequestedSpy);

    fixture.componentRef.setInput('currentPage', 3);
    fixture.detectChanges();

    const [previousButton] = getButtons();
    previousButton.click();

    expect(pageRequestedSpy).toHaveBeenCalledTimes(1);
    expect(pageRequestedSpy).toHaveBeenCalledWith(2);
  });

  it('should disable next button on the last page', () => {
    fixture.componentRef.setInput('currentPage', 10);
    fixture.detectChanges();

    const [, nextButton] = getButtons();

    expect(getSummaryText()).toBe('91-95 of 95');
    expect(nextButton.disabled).toBe(true);
  });

  it('should clamp invalid current page and page size values', () => {
    fixture.componentRef.setInput('pageSize', 0);
    fixture.componentRef.setInput('currentPage', -4);
    fixture.detectChanges();

    const [previousButton, nextButton] = getButtons();

    expect(getSummaryText()).toBe('1-1 of 95');
    expect(previousButton.disabled).toBe(true);
    expect(nextButton.disabled).toBe(false);
  });

  it('should display empty range when there are no items', () => {
    fixture.componentRef.setInput('totalItems', 0);
    fixture.detectChanges();

    const [previousButton, nextButton] = getButtons();

    expect(getSummaryText()).toBe('0-0 of 0');
    expect(previousButton.disabled).toBe(true);
    expect(nextButton.disabled).toBe(true);
  });

  it('should expose custom labels for accessibility attributes', () => {
    fixture.componentRef.setInput('betweenLabel', 'de');
    fixture.componentRef.setInput('previousLabel', 'Página anterior');
    fixture.componentRef.setInput('nextLabel', 'Página siguiente');
    fixture.componentRef.setInput('ariaLabel', 'Paginación');
    fixture.detectChanges();

    const [previousButton, nextButton] = getButtons();
    const group = fixture.debugElement.query(By.css('.paginator')).nativeElement as HTMLElement;

    expect(getSummaryText()).toBe('1-10 de 95');
    expect(previousButton.getAttribute('aria-label')).toBe('Página anterior');
    expect(nextButton.getAttribute('aria-label')).toBe('Página siguiente');
    expect(group.getAttribute('aria-label')).toBe('Paginación');
  });
});
