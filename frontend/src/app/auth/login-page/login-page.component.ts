import { Component } from '@angular/core';
import { LoginFormComponent } from '../login-form/login-form.component';
import { CardComponent } from '../../shared/ui/card/card.component'
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  standalone: true,
  selector: 'app-login-page',
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.css'],
  imports: [
    LoginFormComponent,
    CardComponent,
    TranslatePipe
  ],
})
export class LoginPageComponent {}
