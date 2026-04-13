import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { CardComponent } from '../../../shared/ui/card/card.component';

@Component({
  selector: 'app-worksites-page',
  standalone: true,
  imports: [CommonModule, CardComponent, PageTemplateComponent],
  templateUrl: './worksites-page.component.html',
  styleUrl: './worksites-page.component.css',
})
export class WorksitesPageComponent {}
