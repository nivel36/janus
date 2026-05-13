/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { map } from 'rxjs';

import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { CardComponent } from '../../../shared/ui/card/card.component';
import { ChipComponent } from '../../../shared/ui/chip/chip.component';
import { retryTransientHttpErrors } from '../../../shared/utils/http-retry.util';
import { Worksite } from '../models/worksite';
import { WorksiteApiService } from '../services/worksite-api.service';

@Component({
  selector: 'app-worksite-detail-page',
  standalone: true,
  imports: [ButtonComponent, CardComponent, ChipComponent, PageTemplateComponent, TranslatePipe],
  templateUrl: './worksite-detail-page.component.html',
  styleUrl: './worksite-detail-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorksiteDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly worksiteApiService = inject(WorksiteApiService);

  protected readonly worksiteCode = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('code') ?? '')),
    { initialValue: this.route.snapshot.paramMap.get('code') ?? '' },
  );

  protected readonly worksiteResource = rxResource<Worksite, { code: string }>({
    params: () => ({ code: this.worksiteCode() }),
    stream: ({ params }) =>
      this.worksiteApiService.findByCode(params.code).pipe(retryTransientHttpErrors()),
  });

  protected readonly worksite = computed(() => this.worksiteResource.value());

  protected goBack(): void {
    this.router.navigate(['/worksites']);
  }
}
