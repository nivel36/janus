import { Component } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { TranslatePipe } from "@ngx-translate/core";
import { AuthService } from "../../core/auth/auth.service";
import { Router } from "@angular/router";
import { ButtonComponent } from '../../shared/ui/button/button.component';

@Component({
  standalone: true,
  selector: 'app-login-form',
  templateUrl: './login-form.component.html',
  styleUrls: ['./login-form.component.css'],
  imports: [ReactiveFormsModule, TranslatePipe, ButtonComponent],
})
export class LoginFormComponent {
  isLoading = false;
  errorMessage = '';

  readonly form = new FormGroup({
    username: new FormControl('', { nonNullable: true }),
    password: new FormControl('', { nonNullable: true }),
  });

  constructor(private authService: AuthService, private router: Router) {}

  submit(): void {
    if (this.form.invalid || this.isLoading) return;

    this.isLoading = true;
    this.errorMessage = '';

    const { username, password } = this.form.getRawValue();

    this.authService.login(username, password).subscribe({
      next: () => this.router.navigate(['/']),
      error: () => {
        this.errorMessage = 'login.error'; // o el texto directo, o clave i18n
        this.isLoading = false;
      },
      complete: () => (this.isLoading = false),
    });
  }
}
