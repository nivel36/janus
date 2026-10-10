/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { PageTemplateComponent } from '../../../../core/layout/page-template/page-template.component';
import { WorksiteFormComponent } from '../../components/worksite-form/worksite-form.component';
import { CreateWorksitePayload } from '../../models/worksite';
import { WorksiteApiService } from '../../services/worksite-api.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-worksite-create-page',
  imports: [PageTemplateComponent, WorksiteFormComponent],
  templateUrl: './worksite-create-page.component.html',
})
export class WorksiteCreatePageComponent {
  private readonly router = inject(Router);
  private readonly worksiteApiService = inject(WorksiteApiService);

  readonly save = (value: CreateWorksitePayload) => this.worksiteApiService.create(value);

  onSaved(): void {
    this.router.navigate(['/worksites']);
  }
}
