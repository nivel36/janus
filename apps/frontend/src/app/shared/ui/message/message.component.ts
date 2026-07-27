/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type MessageType = 'error' | 'success' | 'warning' | 'info';
export type MessagePresentation = 'inline' | 'panel';

@Component({
  selector: 'app-message',
  standalone: true,
  templateUrl: './message.component.html',
  styleUrl: './message.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MessageComponent {
  readonly type = input<MessageType>('info');
  readonly presentation = input<MessagePresentation>('panel');

  protected readonly classes = computed(
    () => `message message--${this.type()} message--${this.presentation()}`,
  );
  protected readonly role = computed(() => (this.type() === 'error' ? 'alert' : null));
  protected readonly ariaLive = computed(() => (this.type() === 'error' ? 'assertive' : 'polite'));
}
