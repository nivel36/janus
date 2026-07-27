/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import {
  AsyncEmptyDirective,
  AsyncErrorDirective,
  AsyncLoadingDirective,
  AsyncStateComponent,
} from './async-state.component';

@Component({
  standalone: true,
  imports: [AsyncStateComponent, AsyncLoadingDirective, AsyncErrorDirective, AsyncEmptyDirective],
  template: `
    <app-async-state [loading]="loading" [error]="error" [empty]="empty">
      <ng-template appAsyncLoading><span data-state="loading">Loading</span></ng-template>
      <ng-template appAsyncError><span data-state="error">Error</span></ng-template>
      <ng-template appAsyncEmpty><span data-state="empty">Empty</span></ng-template>
    </app-async-state>
  `,
})
class TestHostComponent {
  loading = false;
  error = false;
  empty = false;
}

describe('AsyncStateComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TestHostComponent] }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
  });

  it.each([
    ['loading', { loading: true, error: false, empty: false }, 'polite'],
    ['error', { loading: false, error: true, empty: false }, 'assertive'],
    ['empty', { loading: false, error: false, empty: true }, 'polite'],
  ] as const)('shows only the %s state', (state, values, ariaLive) => {
    Object.assign(host, values);
    fixture.detectChanges();

    expect(visibleStates()).toEqual([state]);
    expect(message()?.getAttribute('aria-live')).toBe(ariaLive);
    expect(message()?.getAttribute('role')).toBe(state === 'error' ? 'alert' : null);
  });

  it('prioritizes error when incompatible states are simultaneously true', () => {
    host.loading = true;
    host.error = true;
    host.empty = true;
    fixture.detectChanges();

    expect(visibleStates()).toEqual(['error']);
    expect(message()?.getAttribute('role')).toBe('alert');
  });

  it('prioritizes loading over empty and renders nothing without an active state', () => {
    host.loading = true;
    host.empty = true;
    fixture.detectChanges();
    expect(visibleStates()).toEqual(['loading']);

    host.loading = false;
    host.empty = false;
    fixture.detectChanges();
    expect(visibleStates()).toEqual([]);
    expect(message()).toBeNull();
  });

  function visibleStates(): string[] {
    return Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('[data-state]')).map(
      (element) => element.dataset['state']!,
    );
  }

  function message(): HTMLElement | null {
    return fixture.nativeElement.querySelector('.message');
  }
});
