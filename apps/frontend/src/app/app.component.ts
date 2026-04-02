/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { UserProfileService } from './core/user-profile/user-profile.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  constructor() {
    inject(UserProfileService);
  }
}
