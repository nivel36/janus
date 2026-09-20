/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-verify-email',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main>
      <h1>Verify your email</h1>
      <p>Verify your email address before continuing to Janus.</p>
      <button type="button" (click)="resend()">Resend verification email</button>
      <button type="button" (click)="continue()" [disabled]="refreshing()">
        I have verified my email
      </button>
      @if (verificationPending()) {
        <p>Your email is not verified yet. Follow the link in the email and try again.</p>
      }
    </main>
  `,
})
export class VerifyEmailComponent {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly refreshing = signal(false);
  readonly verificationPending = signal(false);

  resend(): void {
    void this.auth.requestEmailVerification(this.returnUrl());
  }

  async continue(): Promise<void> {
    this.refreshing.set(true);
    try {
      if (await this.auth.refreshAfterEmailVerification()) {
        await this.router.navigateByUrl(this.returnUrl());
      } else {
        this.verificationPending.set(true);
      }
    } finally {
      this.refreshing.set(false);
    }
  }

  private returnUrl(): string {
    const requested = this.route.snapshot.queryParamMap.get('returnUrl');
    return requested?.startsWith('/') && !requested.startsWith('//') ? requested : '/';
  }
}
