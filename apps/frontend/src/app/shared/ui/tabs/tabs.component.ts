/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { NgTemplateOutlet } from '@angular/common';
import { Component, contentChildren, signal, viewChildren } from '@angular/core';
import { createUuid } from '../../utils/uuid.utils';
import { TabItemDirective } from './tab-item.directive';
import { TabTriggerDirective } from './tab-trigger.directive';

/**
 * Reusable tab container with lazy panel instantiation.
 *
 * <p>Panels are only created after selecting their tab, so expensive content
 * is not rendered upfront.</p>
 */
@Component({
  selector: 'app-tabs',
  standalone: true,
  imports: [NgTemplateOutlet, TabTriggerDirective],
  templateUrl: './tabs.component.html',
  styleUrl: './tabs.component.css',
})
export class TabsComponent {
  readonly tabItems = contentChildren(TabItemDirective);

  private readonly tabTriggers = viewChildren(TabTriggerDirective);

  readonly activeIndex = signal(0);

  private readonly instanceId = `tabs-${createUuid()}`;

  private readonly loadedIndices = signal<Set<number>>(new Set([0]));

  selectTab(index: number): void {
    this.activateTab(index);
  }

  onKeydown(event: KeyboardEvent): void {
    const triggerCount = this.tabTriggers().length;
    if (triggerCount === 0) {
      return;
    }

    const currentIndex = this.activeIndex();
    let nextIndex: number | null = null;

    if (event.key === 'ArrowRight') {
      nextIndex = (currentIndex + 1) % triggerCount;
    } else if (event.key === 'ArrowLeft') {
      nextIndex = (currentIndex - 1 + triggerCount) % triggerCount;
    } else if (event.key === 'Home') {
      nextIndex = 0;
    } else if (event.key === 'End') {
      nextIndex = triggerCount - 1;
    }

    if (nextIndex != null) {
      event.preventDefault();
      this.activateTab(nextIndex);
      this.tabTriggers()[nextIndex]?.focus();
    }
  }

  private activateTab(index: number): void {
    if (this.activeIndex() !== index) {
      this.activeIndex.set(index);
    }

    if (this.loadedIndices().has(index)) {
      return;
    }

    this.loadedIndices.update((loaded) => {
      const next = new Set(loaded);
      next.add(index);
      return next;
    });
  }

  isLoaded(index: number): boolean {
    return this.loadedIndices().has(index);
  }

  triggerId(index: number): string {
    return `${this.instanceId}-trigger-${index}`;
  }

  panelId(index: number): string {
    return `${this.instanceId}-panel-${index}`;
  }
}
