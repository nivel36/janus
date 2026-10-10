/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { HttpErrorResponse } from '@angular/common/http';
import { NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  Directive,
  TemplateRef,
  contentChild,
  computed,
  inject,
  input,
} from '@angular/core';

import { TranslatePipe } from '@ngx-translate/core';

import { ButtonComponent } from '../button/button.component';
import { MessageComponent } from '../message/message.component';

@Directive({
  selector: 'ng-template[appAsyncLoading]',
})
export class AsyncLoadingDirective {
  readonly template = inject(TemplateRef<unknown>);
}

@Directive({
  selector: 'ng-template[appAsyncError]',
})
export class AsyncErrorDirective {
  readonly template = inject(TemplateRef<unknown>);
}

@Directive({
  selector: 'ng-template[appAsyncEmpty]',
})
export class AsyncEmptyDirective {
  readonly template = inject(TemplateRef<unknown>);
}

@Component({
  selector: 'app-async-state',
  imports: [MessageComponent, NgTemplateOutlet, ButtonComponent, TranslatePipe],
  templateUrl: './async-state.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AsyncStateComponent {
  readonly loading = input(false);
  readonly error = input<unknown>();
  readonly retry = input<() => void>();
  readonly empty = input(false);

  protected readonly canRetry = computed(() => {
    const error = this.error();
    return (
      !!error &&
      !this.loading() &&
      !!this.retry() &&
      !(error instanceof HttpErrorResponse && error.status === 404)
    );
  });

  protected readonly loadingContent = contentChild(AsyncLoadingDirective);
  protected readonly errorContent = contentChild(AsyncErrorDirective);
  protected readonly emptyContent = contentChild(AsyncEmptyDirective);
}
