/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { isPlatformBrowser } from '@angular/common';
import { Component, DestroyRef, OnInit, PLATFORM_ID, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timer } from 'rxjs';

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
})
export class ClockComponent implements OnInit {
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

  private readonly platformId = inject(PLATFORM_ID);

  /**
   * Completes the browser timer subscription with the component lifecycle.
   */
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Whether browser-only APIs can be used safely.
   */
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  /**
   * Starts a browser-only clock which emits immediately and then every second.
   * SSR intentionally renders an empty value so server and hydrated output do
   * not depend on different wall-clock instants.
   */
  ngOnInit(): void {
    if (this.isBrowser) {
      timer(0, 1000)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(() => this.updateTime());
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
