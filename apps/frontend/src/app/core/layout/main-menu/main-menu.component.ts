import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faCalendarDays, faBuilding } from '@fortawesome/free-regular-svg-icons';
import { faPowerOff, faGear, faUserCircle } from '@fortawesome/free-solid-svg-icons';
import { CurrentUserFacade } from '../../user/services/current-user.facade';
import { AuthService } from '../../auth/auth.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-main-menu',
  imports: [RouterLink, TranslatePipe, FontAwesomeModule],
  templateUrl: './main-menu.component.html',
  styleUrl: './main-menu.component.css',
})
export class MainMenuComponent {
  private readonly currentUser = inject(CurrentUserFacade);
  private readonly auth = inject(AuthService);

  readonly user = this.currentUser.currentUser;

  readonly faPowerOff = faPowerOff;
  readonly faGear = faGear;
  readonly faCalendarDays = faCalendarDays;
  readonly faBuilding = faBuilding;
  readonly faUserCircle = faUserCircle;

  async logout(): Promise<void> {
    await this.auth.logout();
  }
}
