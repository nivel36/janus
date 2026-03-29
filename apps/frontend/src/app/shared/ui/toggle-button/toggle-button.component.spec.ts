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

  it('should update checked and emit checkedChange when toggled', () => {
    const emitSpy = spyOn(component.checkedChange, 'emit');

    component.checked = false;
    component.onToggle();

    expect(component.checked).toBeTrue();
    expect(emitSpy).toHaveBeenCalledWith(true);
  });

  it('should not update checked or emit checkedChange when disabled', () => {
    const emitSpy = spyOn(component.checkedChange, 'emit');

    component.checked = false;
    component.disabled = true;
    component.onToggle();

    expect(component.checked).toBeFalse();
    expect(emitSpy).not.toHaveBeenCalled();
  });
});
