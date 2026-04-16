import { Component, input, Input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { RouterLink } from '@angular/router';

import { MainMenuComponent } from '../../../core/layout/main-menu/main-menu.component';

@Component({
  selector: 'app-page-template',
  standalone: true,
  imports: [TranslatePipe, RouterLink, MainMenuComponent],
  templateUrl: './page-template.component.html',
  styleUrl: './page-template.component.css',
})
export class PageTemplateComponent {
  readonly appNameKey = input.required<string>();

  readonly pageNameKey = input.required<string>();
}
