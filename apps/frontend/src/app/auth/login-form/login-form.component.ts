import { Component } from "@angular/core";
import { TranslatePipe } from "@ngx-translate/core";
import { AuthService } from "../../core/auth/auth.service";
import { ButtonComponent } from '../../shared/ui/button/button.component';

@Component({
  standalone: true,
  selector: 'app-login-form',
  templateUrl: './login-form.component.html',
  styleUrls: ['./login-form.component.css'],
  imports: [TranslatePipe, ButtonComponent],
})
export class LoginFormComponent {
  isLoading = false;
  errorMessage = '';

  constructor(private authService: AuthService) {}

  login(): void {
    if (this.isLoading) return;

    this.isLoading = true;
    this.errorMessage = '';

    void this.authService
      .login()
      .then(() => this.router.navigate(['/']))
      .catch(() => {
        this.errorMessage = 'login.error';
      })
      .finally(() => {
        this.isLoading = false;
      });
  }
}
