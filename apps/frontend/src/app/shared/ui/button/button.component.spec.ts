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
		expect(buttonEl.type).toBe('button');
		expect(buttonEl.classList).toContain('button--default');
	});

	it('should apply the requested variant and show icons in the right position', () => {
		component.variant = 'error';
		component.icon = '⚠';
		component.iconPosition = 'right';
		fixture.detectChanges();

		const buttonEl: HTMLButtonElement = fixture.nativeElement.querySelector('button');
		expect(buttonEl.classList).toContain('button--error');

		const icons = buttonEl.querySelectorAll('.button-icon');
		expect(icons.length).toBe(1);
		expect(icons[0].textContent?.trim()).toBe('⚠');
	});
});
