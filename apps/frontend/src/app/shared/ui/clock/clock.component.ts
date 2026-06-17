/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { isPlatformBrowser } from '@angular/common';
import { Component, OnInit, OnDestroy, PLATFORM_ID, inject, input } from '@angular/core';

/**
 * Displays the current local time and refreshes it every second.
 *
 * The component renders semantic `<time>` markup and includes ARIA metadata
 * so assistive technologies can identify the element as a timer without being
 * interrupted each second.
 */
@Component({
  selector: 'app-clock',
  standalone: true,
  templateUrl: './clock.component.html',
  styleUrls: ['./clock.component.css'],
})
export class ClockComponent implements OnInit, OnDestroy {
  /**
   * Preferred BCP 47 locale used to format the visual time string.
   * If not provided, the component falls back to the browser locale or `en-US`.
   */
  readonly locale = input<string>();

  /**
   * Whether the rendered time should use a 12-hour clock format.
   */
  readonly use12Hour = input<boolean>(false);

  /**
   * Accessible name announced for the timer element.
   */
  readonly ariaLabel = input<string>('Current time');

  /**
   * Human-readable time shown in the UI (for example, `10:15:30`).
   */
  time = '';

  /**
   * ISO-8601 datetime value bound to the `<time datetime>` attribute.
   */
  isoDateTime = '';

  /**
   * Internal interval handle used to stop periodic updates on destroy.
   */
  private readonly platformId = inject(PLATFORM_ID);

  /**
   * Whether browser-only APIs can be used safely.
   */
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  private timerId?: ReturnType<typeof setInterval>;

  /**
   * Initializes the clock and starts a one-second refresh interval.
   */
  ngOnInit(): void {
    this.updateTime();

    if (this.isBrowser) {
      this.timerId = window.setInterval(() => this.updateTime(), 1000);
    }
  }

  /**
   * Stops the internal timer when the component is destroyed.
   */
  ngOnDestroy(): void {
    if (this.timerId !== undefined) {
      clearInterval(this.timerId);
    }
  }

  /**
   * Recomputes the formatted and machine-readable current time values.
   */
  private updateTime(): void {
    const currentLocale = this.locale() ?? globalThis.navigator?.language ?? 'en-US';
    const now = new Date();

    this.time = now.toLocaleTimeString(currentLocale, {
      hour12: this.use12Hour(),
    });

    this.isoDateTime = now.toISOString();
  }
}
