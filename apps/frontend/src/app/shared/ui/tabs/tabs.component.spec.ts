/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { describe, expect, it, beforeEach } from 'vitest';

import { TabItemDirective } from './tab-item.directive';
import { TabsComponent } from './tabs.component';

@Component({
  standalone: true,
  imports: [TabsComponent, TabItemDirective],
  template: `
    <app-tabs>
      <ng-template appTabItem="Resumen">
        <p id="summary-content">Contenido resumen</p>
      </ng-template>

      <ng-template appTabItem="Detalles">
        <p id="details-content">Contenido detalles</p>
      </ng-template>

      <ng-template appTabItem="Histórico">
        <p id="history-content">Contenido histórico</p>
      </ng-template>
    </app-tabs>
  `,
})
class TestHostComponent {}

describe('TabsComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;

  beforeEach(async () => {
    TestBed.overrideComponent(TabsComponent, {
      set: {
        template: `
          <div class="tabs" role="tablist" aria-label="Tabs navigation">
              @for (tabItem of tabItems(); track $index; let index = $index) {
                  <button
                      type="button"
                      class="tabs-trigger"
                      role="tab"
                      [id]="'tab-trigger-' + index"
                      [attr.aria-controls]="'tab-panel-' + index"
                      [attr.aria-selected]="activeIndex() === index"
                      [class.tabs-trigger-active]="activeIndex() === index"
                      (click)="selectTab(index)"
                  >
                      {{ tabItem.label() }}
                  </button>
              }
          </div>

          @for (tabItem of tabItems(); track $index; let index = $index) {
              @if (isLoaded(index) && activeIndex() === index) {
                  <section
                      class="tabs-panel"
                      role="tabpanel"
                      [id]="'tab-panel-' + index"
                      [attr.aria-labelledby]="'tab-trigger-' + index"
                  >
                      <ng-container [ngTemplateOutlet]="tabItem.template"></ng-container>
                  </section>
              }
          }
        `,
        styles: [''],
      },
    });

    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    const tabsDebugEl = fixture.debugElement.query(By.directive(TabsComponent));
    expect(tabsDebugEl).toBeTruthy();
  });

  it('should render only the first tab panel on initial load', () => {
    expect(fixture.nativeElement.querySelector('#summary-content')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('#details-content')).toBeNull();
    expect(fixture.nativeElement.querySelector('#history-content')).toBeNull();
  });

  it('should lazy-load a panel only when its tab is clicked', () => {
    const tabButtons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll('button[role="tab"]');

    tabButtons[1].click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#summary-content')).toBeNull();
    expect(fixture.nativeElement.querySelector('#details-content')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('#history-content')).toBeNull();

    tabButtons[2].click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#details-content')).toBeNull();
    expect(fixture.nativeElement.querySelector('#history-content')).toBeTruthy();
  });
});
