/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { map } from 'rxjs';
import {
  faCalendarDays,
  faClock,
  faExclamationTriangle,
  faUsers,
} from '@fortawesome/free-solid-svg-icons';

import { PageTemplateComponent } from '../../../core/layout/page-template/page-template.component';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { CardComponent } from '../../../shared/ui/card/card.component';
import { retryTransientHttpErrors } from '../../../shared/utils/http-retry.util';
import { Worksite } from '../models/worksite';
import { WorksiteApiService } from '../services/worksite-api.service';
import { TabItemDirective } from '../../../shared/ui/tabs/tab-item.directive';
import { TabsComponent } from '../../../shared/ui/tabs/tabs.component';
import { SummaryCardComponent } from '../../../shared/ui/summary-card/summary-card.component';
import { ChipComponent } from '../../../shared/ui/chip/chip.component';
import { WorksiteHeroComponent } from '../components/worksite-hero/worksite-hero.component';

@Component({
  selector: 'app-worksite-detail-page',
  standalone: true,
  imports: [
    ButtonComponent,
    CardComponent,
    PageTemplateComponent,
    TranslatePipe,
    TabsComponent,
    TabItemDirective,
    SummaryCardComponent,
    ChipComponent,
    WorksiteHeroComponent,
  ],
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

  protected readonly faCalendarDays = faCalendarDays;
  protected readonly faClock = faClock;
  protected readonly faExclamationTriangle = faExclamationTriangle;
  protected readonly faUsers = faUsers;

  protected goBack(): void {
    this.router.navigate(['/worksites']);
  }

  protected editWorksite(worksite: Worksite): void {
    this.router.navigate(['/worksites', worksite.code, 'edit']);
  }
}
