/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ButtonComponent } from './button.component';

describe('ButtonComponent', () => {
  let fixture: ComponentFixture<ButtonComponent>;
  let component: ButtonComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ButtonComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the default variant with the default type', () => {
    const buttonEl: HTMLButtonElement = fixture.nativeElement.querySelector('button');

    expect(buttonEl).toBeTruthy();
    expect(buttonEl.type).toBe('button');
    expect(buttonEl.classList).toContain('button--default');
  });

  it('should apply the requested variant and show the icon on the right', () => {
    component.variant = 'secondary';
    component.icon = '⚠';
    component.iconPosition = 'right';
    fixture.detectChanges();

    const buttonEl: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(buttonEl.classList).toContain('button--secondary');

    const icons = buttonEl.querySelectorAll('.button-icon');
    expect(icons.length).toBe(1);
    expect(icons[0].textContent?.trim()).toBe('⚠');

    const children = Array.from(buttonEl.children);
    expect(children[0].classList).toContain('button-label');
    expect(children[1].classList).toContain('button-icon');
  });

  it('should show the icon on the left when requested', () => {
    component.icon = '✓';
    component.iconPosition = 'left';
    fixture.detectChanges();

    const buttonEl: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    const children = Array.from(buttonEl.children);

    expect(children[0].classList).toContain('button-icon');
    expect(children[1].classList).toContain('button-label');
  });

  it('should disable the button when disabled is true', () => {
    component.disabled = true;
    fixture.detectChanges();

    const buttonEl: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(buttonEl.disabled).toBeTrue();
  });

  it('should expose aria-label when provided', () => {
    component.ariaLabel = 'Open menu';
    fixture.detectChanges();

    const buttonEl: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(buttonEl.getAttribute('aria-label')).toBe('Open menu');
  });

  it('should not render aria-label when not provided', () => {
    component.ariaLabel = null;
    fixture.detectChanges();

    const buttonEl: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(buttonEl.hasAttribute('aria-label')).toBeFalse();
  });

  it('should emit clicked when the button is pressed', () => {
    spyOn(component.clicked, 'emit');

    const buttonEl: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    buttonEl.click();

    expect(component.clicked.emit).toHaveBeenCalled();
  });
});
