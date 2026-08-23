/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { NgTemplateOutlet } from '@angular/common';
import { FocusKeyManager } from '@angular/cdk/a11y';
import { Component, contentChildren, effect, signal, untracked, viewChildren } from '@angular/core';
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

  private keyManager?: FocusKeyManager<TabTriggerDirective>;

  constructor() {
    effect((onCleanup) => {
      const triggers = this.tabTriggers();
      const keyManager = new FocusKeyManager(triggers).withHorizontalOrientation('ltr').withWrap();

      keyManager.setActiveItem(untracked(this.activeIndex));
      const subscription = keyManager.change.subscribe((index) => this.activateTab(index));
      this.keyManager = keyManager;

      onCleanup(() => subscription.unsubscribe());
    });
  }

  selectTab(index: number): void {
    if (this.keyManager) {
      this.keyManager.setActiveItem(index);
    } else {
      this.activateTab(index);
    }
  }

  onKeydown(event: KeyboardEvent): void {
    this.keyManager?.onKeydown(event);
  }

  private activateTab(index: number): void {
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

  triggerId(index: number): string {
    return `${this.instanceId}-trigger-${index}`;
  }

  panelId(index: number): string {
    return `${this.instanceId}-panel-${index}`;
  }
}
