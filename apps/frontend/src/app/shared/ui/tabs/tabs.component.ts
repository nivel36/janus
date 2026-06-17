/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { NgTemplateOutlet } from '@angular/common';
import { Component, ElementRef, contentChildren, inject, signal } from '@angular/core';
import { createUuid } from '../../utils/uuid.utils';
import { TabItemDirective } from './tab-item.directive';

/**
 * Reusable tab container with lazy panel instantiation.
 *
 * <p>Panels are only created after selecting their tab, so expensive content
 * is not rendered upfront.</p>
 */
@Component({
  selector: 'app-tabs',
  standalone: true,
  imports: [NgTemplateOutlet],
  templateUrl: './tabs.component.html',
  styleUrls: ['./tabs.component.css'],
})
export class TabsComponent {
  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly tabItems = contentChildren(TabItemDirective);

  readonly activeIndex = signal(0);

  private readonly instanceId = `tabs-${createUuid()}`;

  private readonly loadedIndices = signal<Set<number>>(new Set([0]));

  selectTab(index: number): void {
    this.activateTab(index);
  }

  onTabKeydown(event: KeyboardEvent, index: number): void {
    // Currently tabs are horizontal. If configurable orientation is added,
    // route vertical tablists through ArrowUp/ArrowDown and keep Home/End.
    const tabCount = this.tabItems().length;

    if (tabCount === 0) {
      return;
    }

    let nextIndex: number;

    switch (event.key) {
      case 'ArrowRight':
        nextIndex = (index + 1) % tabCount;
        break;
      case 'ArrowLeft':
        nextIndex = (index - 1 + tabCount) % tabCount;
        break;
      case 'Home':
        nextIndex = 0;
        break;
      case 'End':
        nextIndex = tabCount - 1;
        break;
      default:
        return;
    }

    event.preventDefault();
    this.activateTab(nextIndex);
    this.focusTab(nextIndex);
  }

  private activateTab(index: number): void {
    this.activeIndex.set(index);
    this.loadedIndices.update((loaded) => {
      const next = new Set(loaded);
      next.add(index);
      return next;
    });
  }

  private focusTab(index: number): void {
    const tabs =
      this.elementRef.nativeElement.querySelectorAll<HTMLButtonElement>('button[role="tab"]');
    tabs.item(index)?.focus();
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
