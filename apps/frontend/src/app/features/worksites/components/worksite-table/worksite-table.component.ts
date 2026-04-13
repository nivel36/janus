import { ChangeDetectionStrategy, Component, computed, inject, resource } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';

import { WorksiteApiService } from '../../services/worksite-api.service';
import { Worksite } from '../../models/worksite';

@Component({
  selector: 'app-worksite-table',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './worksite-table.component.html',
  styleUrl: './worksite-table.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorksiteTableComponent {
  private readonly worksiteApiService = inject(WorksiteApiService);

  protected readonly worksitesResource = resource<Worksite[], void>({
    loader: () => firstValueFrom(this.worksiteApiService.findAll()),
    defaultValue: [],
  });

  protected readonly worksites = computed(() => this.worksitesResource.value());

  protected readonly isEmpty = computed(
    () =>
      !this.worksitesResource.isLoading() &&
      this.worksitesResource.error() === undefined &&
      this.worksites().length === 0,
  );
}
