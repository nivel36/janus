import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { CurrentUserFacade } from '../../auth/current-user.facade';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-main-menu',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './main-menu.component.html',
  styleUrls: ['./main-menu.component.css'],
})
export class MainMenuComponent {
  private readonly currentUser = inject(CurrentUserFacade);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser$ = this.currentUser.currentUser$;
  readonly fullName$ = this.currentUser.fullName$;
  readonly isAdmin$ = this.currentUser.isAdmin$;

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  goToSettings(): void {
    this.router.navigate(['/settings']);
  }
}
