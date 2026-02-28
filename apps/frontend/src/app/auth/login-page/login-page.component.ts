import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { take } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { LoginFormComponent } from '../login-form/login-form.component';
import { CardComponent } from '../../shared/ui/card/card.component';

@Component({
  standalone: true,
  selector: 'app-login-page',
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.css'],
  imports: [LoginFormComponent, CardComponent, TranslatePipe],
})
export class LoginPageComponent implements OnInit {
  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.authService.isAuthenticated$.pipe(take(1)).subscribe((isAuthenticated) => {
      if (isAuthenticated) {
        void this.router.navigate(['/']);
      }
    });
  }
}
