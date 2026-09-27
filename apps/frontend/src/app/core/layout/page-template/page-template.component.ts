import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { RouterLink } from '@angular/router';

import { MainMenuComponent } from '../../../core/layout/main-menu/main-menu.component';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-page-template',
  imports: [TranslatePipe, RouterLink, MainMenuComponent],
  templateUrl: './page-template.component.html',
  styleUrl: './page-template.component.css',
})
export class PageTemplateComponent {
  readonly appNameKey = input.required<string>();

  readonly pageNameKey = input.required<string>();
}
