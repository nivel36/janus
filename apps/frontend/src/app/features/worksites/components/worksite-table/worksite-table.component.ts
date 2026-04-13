import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  resource,
  signal,
} from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';

import { WorksiteApiService } from '../../services/worksite-api.service';
import { Worksite } from '../../models/worksite';
import { PaginatorComponent } from '../../../../shared/ui/paginator/paginator.component';

@Component({
  selector: 'app-worksite-table',
  standalone: true,
  imports: [TranslatePipe, PaginatorComponent],
  templateUrl: './worksite-table.component.html',
  styleUrl: './worksite-table.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorksiteTableComponent {
  private static readonly PAGE_SIZE = 5;

  private readonly worksiteApiService = inject(WorksiteApiService);

  protected readonly currentPage = signal(1);

  protected readonly worksitesResource = resource<Worksite[], void>({
    loader: () => firstValueFrom(this.worksiteApiService.findAll()),
    defaultValue: [],
  });

  protected readonly worksites = computed(() => this.worksitesResource.value());

  protected readonly totalItems = computed(() => this.worksites().length);

  protected readonly pagedWorksites = computed(() => {
    const start = (this.currentPage() - 1) * WorksiteTableComponent.PAGE_SIZE;
    const end = start + WorksiteTableComponent.PAGE_SIZE;

    return this.worksites().slice(start, end);
  });

  private readonly pageSyncEffect = effect(() => {
    if (this.worksitesResource.isLoading()) {
      return;
    }

    const maxPage = Math.max(1, Math.ceil(this.totalItems() / WorksiteTableComponent.PAGE_SIZE));

    if (this.currentPage() > maxPage) {
      this.currentPage.set(maxPage);
    }
  });

  protected readonly isEmpty = computed(
    () =>
      !this.worksitesResource.isLoading() &&
      this.worksitesResource.error() === undefined &&
      this.worksites().length === 0,
  );

  protected onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  protected get pageSize(): number {
    return WorksiteTableComponent.PAGE_SIZE;
  }
}
