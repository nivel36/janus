/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { NgTemplateOutlet } from '@angular/common';
import { Component, contentChildren, signal } from '@angular/core';
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
  readonly tabItems = contentChildren(TabItemDirective);

  readonly activeIndex = signal(0);

  private readonly loadedIndices = signal<Set<number>>(new Set([0]));

  selectTab(index: number): void {
    this.activeIndex.set(index);
    this.loadedIndices.update((loaded) => {
      const next = new Set(loaded);
      next.add(index);
      return next;
    });
  }

  isLoaded(index: number): boolean {
    return this.loadedIndices().has(index);
  }
}
