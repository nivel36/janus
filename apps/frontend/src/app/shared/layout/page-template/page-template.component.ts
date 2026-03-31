import { Component, Input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { MainMenuComponent } from '../../../core/layout/main-menu/main-menu.component';

@Component({
  selector: 'app-page-template',
  standalone: true,
  imports: [TranslatePipe, MainMenuComponent],
  templateUrl: './page-template.component.html',
  styleUrl: './page-template.component.css',
})
export class PageTemplateComponent {
  @Input({ required: true }) appNameKey = '';
  @Input({ required: true }) pageNameKey = '';
}
