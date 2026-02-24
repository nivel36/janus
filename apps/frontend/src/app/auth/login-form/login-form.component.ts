import { Component } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
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
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8)],
    }),
  });

  constructor(private authService: AuthService, private router: Router) {}

  submit(): void {
    if (this.form.invalid || this.isLoading) return;

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
