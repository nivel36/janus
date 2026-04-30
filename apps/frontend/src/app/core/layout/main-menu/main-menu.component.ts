import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faPowerOff } from '@fortawesome/free-solid-svg-icons';
import { faGear } from '@fortawesome/free-solid-svg-icons';
import { faCalendarDays } from '@fortawesome/free-solid-svg-icons';
import { faIndustry } from '@fortawesome/free-solid-svg-icons';

import { CurrentUserFacade } from '../../user/services/current-user.facade';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-main-menu',
  standalone: true,
  imports: [CommonModule, TranslatePipe, FontAwesomeModule],
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

  readonly faPowerOff = faPowerOff;
  readonly faGear = faGear;
  readonly faCalendarDays = faCalendarDays;
  readonly faIndustry = faIndustry;

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  goToUserPreferences(): void {
    this.router.navigate(['/user-preferences']);
  }

  goToApplicationSettings(): void {
    this.router.navigate(['/application-settings']);
  }

  goToSchedules(): void {
    this.router.navigate(['/schedules']);
  }

  goToWorksites(): void {
    this.router.navigate(['/worksites']);
  }
}
