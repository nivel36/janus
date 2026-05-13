/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { beforeEach, describe, expect, it } from 'vitest';

import { TabItemDirective } from './tab-item.directive';
import { TabsComponent } from './tabs.component';

@Component({
  standalone: true,
  imports: [TabsComponent, TabItemDirective],
  template: `
    <section id="tabs-a">
      <app-tabs>
        <ng-template appTabItem="Resumen">
          <p class="summary-content">Contenido resumen</p>
        </ng-template>

        <ng-template appTabItem="Detalles">
          <p class="details-content">Contenido detalles</p>
        </ng-template>
      </app-tabs>
    </section>

    <section id="tabs-b">
      <app-tabs>
        <ng-template appTabItem="Uno">
          <p class="one-content">Contenido uno</p>
        </ng-template>

        <ng-template appTabItem="Dos">
          <p class="two-content">Contenido dos</p>
        </ng-template>
      </app-tabs>
    </section>
  `,
})
class TestHostComponent {}

describe('TabsComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    const tabsDebugEls = fixture.debugElement.queryAll(By.directive(TabsComponent));
    expect(tabsDebugEls.length).toBe(2);
  });

  it('should render only the first tab panel on initial load', () => {
    const firstTabs = fixture.nativeElement.querySelector('#tabs-a');

    expect(firstTabs.querySelector('.summary-content')).toBeTruthy();
    expect(firstTabs.querySelector('.details-content')).toBeNull();
  });

  it('should lazy-load a panel only when its tab is clicked', () => {
    const firstTabs = fixture.nativeElement.querySelector('#tabs-a');
    const tabButtons: NodeListOf<HTMLButtonElement> = firstTabs.querySelectorAll('button[role="tab"]');

    tabButtons[1].click();
    fixture.detectChanges();

    expect(firstTabs.querySelector('.summary-content')).toBeNull();
    expect(firstTabs.querySelector('.details-content')).toBeTruthy();
  });

  it('should generate unique ids across different app-tabs instances', () => {
    const tabElements: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll('[role="tab"]');
    const panelElements: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll('[role="tabpanel"]');

    const tabIds = Array.from(tabElements, (tab) => tab.id);
    const panelIds = Array.from(panelElements, (panel) => panel.id);

    expect(new Set(tabIds).size).toBe(tabIds.length);
    expect(new Set(panelIds).size).toBe(panelIds.length);

    tabElements.forEach((tab) => {
      const controlledPanelId = tab.getAttribute('aria-controls');
      expect(controlledPanelId).toBeTruthy();
      expect(fixture.nativeElement.querySelector(`#${controlledPanelId}`)).toBeTruthy();
    });

    panelElements.forEach((panel) => {
      const labelledById = panel.getAttribute('aria-labelledby');
      expect(labelledById).toBeTruthy();
      expect(fixture.nativeElement.querySelector(`#${labelledById}`)).toBeTruthy();
    });
  });
});
