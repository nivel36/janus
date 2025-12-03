import { Component, Input } from '@angular/core';

type ButtonVariant = 'default' | 'error' | 'info' | 'success' | 'neutral';

type ButtonType = 'button' | 'submit' | 'reset';

type IconPosition = 'left' | 'right';

@Component({
	selector: 'app-button',
	standalone: true,
	templateUrl: './button.component.html',
	styleUrl: './button.component.css',
})
export class ButtonComponent {
	@Input() variant: ButtonVariant = 'default';
	@Input() type: ButtonType = 'button';
	@Input() icon: string | null = null;
	@Input() iconPosition: IconPosition = 'left';
	@Input() disabled = false;
	@Input() styleClass = '';

	get variantClass(): string {
		return `button--${this.variant}`;
	}

	get hasIconOnLeft(): boolean {
		return !!this.icon && this.iconPosition === 'left';
	}

	get hasIconOnRight(): boolean {
		return !!this.icon && this.iconPosition === 'right';
	}
}
