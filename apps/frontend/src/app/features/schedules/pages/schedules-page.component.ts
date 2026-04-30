import { Component } from '@angular/core';
import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { ScheduleTableComponent } from '../components/schedule-table/schedule-table.component';

@Component({
  selector: 'app-schedules-page',
  standalone: true,
  imports: [PageTemplateComponent, ScheduleTableComponent],
  templateUrl: './schedules-page.component.html',
  styleUrl: './schedules-page.component.css',
})
export class SchedulesPageComponent {}
