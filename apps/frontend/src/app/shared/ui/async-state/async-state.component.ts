/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  Directive,
  TemplateRef,
  contentChild,
  inject,
  input,
} from '@angular/core';

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
  imports: [MessageComponent, NgTemplateOutlet],
  templateUrl: './async-state.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AsyncStateComponent {
  readonly loading = input(false);
  readonly error = input(false);
  readonly empty = input(false);

  protected readonly loadingContent = contentChild(AsyncLoadingDirective);
  protected readonly errorContent = contentChild(AsyncErrorDirective);
  protected readonly emptyContent = contentChild(AsyncEmptyDirective);
}
