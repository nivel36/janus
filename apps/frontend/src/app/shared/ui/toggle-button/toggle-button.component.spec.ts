/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToggleButtonComponent } from './toggle-button.component';

describe('ToggleButtonComponent', () => {
  let fixture: ComponentFixture<ToggleButtonComponent>;
  let component: ToggleButtonComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToggleButtonComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ToggleButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit checkedChange with true when unchecked is toggled', () => {
    const emitSpy = spyOn(component.checkedChange, 'emit');

    component.checked = false;
    component.onToggle();

    expect(emitSpy).toHaveBeenCalledWith(true);
  });

  it('should not emit checkedChange when disabled', () => {
    const emitSpy = spyOn(component.checkedChange, 'emit');

    component.disabled = true;
    component.onToggle();

    expect(emitSpy).not.toHaveBeenCalled();
  });
});
