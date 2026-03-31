import { Component, Input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-page-template',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './page-template.component.html',
  styleUrl: './page-template.component.css',
})
export class PageTemplateComponent {
  @Input({ required: true }) appNameKey = '';
  @Input({ required: true }) pageNameKey = '';
}
