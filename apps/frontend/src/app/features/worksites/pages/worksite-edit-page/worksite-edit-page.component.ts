/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource, toObservable } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { EMPTY, filter, takeUntil } from 'rxjs';

import { PageTemplateComponent } from '../../../../core/layout/page-template/page-template.component';
import { ACTIVE_SCREEN_HTTP_RETRY_POLICY } from '../../../../core/http/http-retry.interceptor';
import {
  AsyncStateComponent,
  AsyncLoadingDirective,
  AsyncErrorDirective,
} from '../../../../shared/ui/async-state/async-state.component';
import { ButtonComponent } from '../../../../shared/ui/button/button.component';
import { WorksiteFormComponent } from '../../components/worksite-form/worksite-form.component';
import { CreateWorksitePayload, Worksite } from '../../models/worksite';
import { WorksiteApiService } from '../../services/worksite-api.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-worksite-edit-page',
  imports: [
    AsyncStateComponent,
    AsyncLoadingDirective,
    AsyncErrorDirective,
    ButtonComponent,
    TranslatePipe,
    WorksiteFormComponent,
    PageTemplateComponent,
  ],
  templateUrl: './worksite-edit-page.component.html',
})
export class WorksiteEditPageComponent {
  private readonly router = inject(Router);
  private readonly worksiteApiService = inject(WorksiteApiService);

  readonly code = input.required<string>();
  private readonly codeChanges = toObservable(this.code);

  protected readonly retryLoad = () => this.worksiteResource.reload();

  protected readonly worksiteResource = rxResource<Worksite, { code: string }>({
    params: () => ({ code: this.code() }),
    stream: ({ params }) =>
      this.worksiteApiService.findByCode(params.code, ACTIVE_SCREEN_HTTP_RETRY_POLICY),
  });

  protected readonly worksite = computed(() => {
    if (!this.worksiteResource.hasValue()) {
      return null;
    }

    const worksite = this.worksiteResource.value();
    return worksite?.code === this.code() ? worksite : null;
  });

  readonly loading = computed(() => this.worksiteResource.isLoading());

  readonly errorMessage = computed(() =>
    this.worksiteResource.error() ? 'worksite.detailLoadError' : '',
  );

  readonly save = ({ name, timeZone, scope, description, address }: CreateWorksitePayload) => {
    const worksite = this.worksite();
    if (!worksite) {
      return EMPTY;
    }
    return this.worksiteApiService
      .update(worksite.code, {
        name,
        timeZone,
        scope,
        description,
        address,
      })
      .pipe(takeUntil(this.codeChanges.pipe(filter((code) => code !== worksite.code))));
  };

  onSaved(worksite: Worksite): void {
    this.router.navigate(['/worksites', worksite.code]);
  }

  cancel(): void {
    this.router.navigate(['/worksites', this.code()]);
  }
}
