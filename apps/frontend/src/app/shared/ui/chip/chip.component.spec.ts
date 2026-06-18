/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { IconDefinition } from '@fortawesome/fontawesome-svg-core';
import { faCheck } from '@fortawesome/free-solid-svg-icons';

import { ChipComponent } from './chip.component';

@Component({
  standalone: true,
  imports: [ChipComponent],
  template: `
    <app-chip
      [label]="label"
      [icon]="icon"
      [type]="type"
      [size]="size"
      [styleClass]="styleClass"
    ></app-chip>
  `,
})
class TestHostComponent {
  label = 'Activo';
  icon: IconDefinition | undefined = undefined;
  type: 'default' | 'primary' | 'secondary' | 'tertiary' | 'green' = 'default';
  size: 'normal' | 'big' | 'small' = 'normal';
  styleClass = '';
}

describe('ChipComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
  });

  it('should render the label without an icon by default', () => {
    const chipEl: HTMLElement = fixture.nativeElement.querySelector('.app-chip');

    expect(chipEl.textContent?.trim()).toBe('Activo');
    expect(chipEl.querySelector('.app-chip__icon')).toBeNull();
  });

  it('should render the optional icon before the label', () => {
    fixture.componentInstance.icon = faCheck;
    fixture.detectChanges();

    const chipEl: HTMLElement = fixture.nativeElement.querySelector('.app-chip');
    const children = Array.from(chipEl.children);

    expect(chipEl.querySelector('.app-chip__icon fa-icon')).not.toBeNull();
    expect(children[0].classList).toContain('app-chip__icon');
    expect(children[1].classList).toContain('app-chip__label');
  });

  it('should apply type, size, and extra classes', () => {
    fixture.componentInstance.type = 'green';
    fixture.componentInstance.size = 'big';
    fixture.componentInstance.styleClass = 'custom-chip';
    fixture.detectChanges();

    const chipEl: HTMLElement = fixture.nativeElement.querySelector('.app-chip');

    expect(chipEl.classList).toContain('app-chip--green');
    expect(chipEl.classList).toContain('app-chip--size-big');
    expect(chipEl.classList).toContain('custom-chip');
  });
});
