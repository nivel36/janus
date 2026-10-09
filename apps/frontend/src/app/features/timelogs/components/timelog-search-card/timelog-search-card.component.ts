/** SPDX-License-Identifier: Apache-2.0 */
import { ChangeDetectionStrategy, Component, DestroyRef, inject, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { map } from 'rxjs';
import { EmployeesService } from '../../../../client/api/employees.service';
import { EmployeeResponse } from '../../../../client/model/employeeResponse';
import { AutocompleteSearchState } from '../../../../shared/state/autocomplete-search.state';
import { AutocompleteTextboxComponent } from '../../../../shared/ui/autocomplete-textbox/autocomplete-textbox.component';
import { CurrentUserFacade } from '../../../../core/user/services/current-user.facade';
import { ID_GENERATOR } from '../../../../shared/services/id-generator.service';
import { ButtonComponent } from '../../../../shared/ui/button/button.component';
import { CardComponent } from '../../../../shared/ui/card/card.component';
import { FieldComponent } from '../../../../shared/ui/field/field.component';
import { InputComponent } from '../../../../shared/ui/input/input.component';
import { TimelogSearchFilters } from '../../models/timelog-search-range';
import { timelogDateRange } from '../../utils/timelog-date-range.util';

@Component({
  selector: 'app-timelog-search-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AutocompleteTextboxComponent, ReactiveFormsModule, TranslatePipe, ButtonComponent, CardComponent, FieldComponent, InputComponent],
  templateUrl: './timelog-search-card.component.html',
  styleUrl: './timelog-search-card.component.css',
})
export class TimelogSearchCardComponent {
  private readonly currentUser = inject(CurrentUserFacade);
  private readonly instanceId = inject(ID_GENERATOR).generate('timelog-search');
  private readonly employees = inject(EmployeesService);
  protected readonly employeeSearch = new AutocompleteSearchState(
    (query: string) => this.employees.searchEmployees(query, undefined, undefined, 0, 20)
      .pipe(map((page) => page.content)),
    inject(DestroyRef),
  );
  protected readonly selectedEmployee = signal<EmployeeResponse | null>(null);
  protected readonly displayEmployee = (employee: EmployeeResponse): string =>
    `${employee.name} ${employee.surname} (${employee.employeeNumber})`;
  protected readonly employeeId = `${this.instanceId}-employee`;
  readonly searchRequested = output<TimelogSearchFilters | undefined>();
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
    const employeeNumber = this.selectedEmployee()?.employeeNumber;
    const employeeFilter = employeeNumber ? { employeeNumber } : {};
    this.errorKey.set('');
    if (!from && !to) {
      this.searchRequested.emit(employeeNumber ? employeeFilter : undefined);
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
      this.searchRequested.emit({ ...timelogDateRange(from, to, timezone), ...employeeFilter });
    } catch {
      this.errorKey.set('timelog.search.errors.invalid');
    }
  }
}
