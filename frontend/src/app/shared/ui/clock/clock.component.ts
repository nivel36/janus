import { Component, OnInit, OnDestroy, Input } from '@angular/core';

@Component({
	selector: 'app-clock',
	standalone: true,
	templateUrl: './clock.component.html',
	styleUrls: ['./clock.component.css']
})
export class ClockComponent implements OnInit, OnDestroy {
	@Input() locale?: string;
	@Input() use12Hour: boolean = false;

	time: string = '';
	private timerId?: number;

	ngOnInit(): void {
		this.updateTime();
		this.timerId = window.setInterval(() => this.updateTime(), 1000);
	}

	ngOnDestroy(): void {
		if (this.timerId) clearInterval(this.timerId);
	}

	private updateTime(): void {
		const currentLocale = this.locale ?? navigator.language;
		const now = new Date();
		this.time = now.toLocaleTimeString(currentLocale, { hour12: this.use12Hour });
	}
}
