/**
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  ChangeDetectionStrategy,
  Component,
  Directive,
  TemplateRef,
  contentChild,
  inject,
  input,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';

import { MessageComponent } from '../message/message.component';

@Directive({ selector: 'ng-template[appAsyncLoading]', standalone: true })
export class AsyncLoadingDirective {
  readonly template = inject(TemplateRef<unknown>);
}

@Directive({ selector: 'ng-template[appAsyncError]', standalone: true })
export class AsyncErrorDirective {
  readonly template = inject(TemplateRef<unknown>);
}

@Directive({ selector: 'ng-template[appAsyncEmpty]', standalone: true })
export class AsyncEmptyDirective {
  readonly template = inject(TemplateRef<unknown>);
}

@Component({
  selector: 'app-async-state',
  standalone: true,
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
