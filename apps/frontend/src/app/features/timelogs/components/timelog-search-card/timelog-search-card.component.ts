/** SPDX-License-Identifier: Apache-2.0 */
import { ChangeDetectionStrategy, Component, inject, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { ID_GENERATOR } from '../../../../shared/services/id-generator.service';
import { ButtonComponent } from '../../../../shared/ui/button/button.component';
import { CardComponent } from '../../../../shared/ui/card/card.component';
import { FieldComponent } from '../../../../shared/ui/field/field.component';
import { InputComponent } from '../../../../shared/ui/input/input.component';
import { TimelogSearchRange } from '../../models/timelog-search-range';
import { timelogDateRange } from '../../utils/timelog-date-range.util';

@Component({
  selector: 'app-timelog-search-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslatePipe, ButtonComponent, CardComponent, FieldComponent, InputComponent],
  templateUrl: './timelog-search-card.component.html',
  styleUrl: './timelog-search-card.component.css',
})
export class TimelogSearchCardComponent {
  private readonly currentUser = inject(CurrentUserFacade);
  private readonly instanceId = inject(ID_GENERATOR).generate('timelog-search');
  readonly searchRequested = output<TimelogSearchRange | undefined>();
  protected readonly fromId = `${this.instanceId}-from`;
  protected readonly toId = `${this.instanceId}-to`;
  protected readonly errorId = `${this.instanceId}-error`;
  protected readonly errorKey = signal('');
  protected readonly form = new FormGroup({
    from: new FormControl('', { nonNullable: true }),
    to: new FormControl('', { nonNullable: true }),
  });

  protected submit(): void {
    const { from, to } = this.form.getRawValue();
    this.errorKey.set('');
    if (!from && !to) {
      this.searchRequested.emit(undefined);
      return;
    }
    if (!from || !to) {
      this.errorKey.set('timelog.search.errors.bothRequired');
      return;
    }
    if (from > to) {
      this.errorKey.set('timelog.search.errors.order');
      return;
    }
    try {
      const timezone = this.currentUser.preferences()?.defaultTimezone
        ?? Intl.DateTimeFormat().resolvedOptions().timeZone;
      this.searchRequested.emit(timelogDateRange(from, to, timezone));
    } catch {
      this.errorKey.set('timelog.search.errors.invalid');
    }
  }
}
