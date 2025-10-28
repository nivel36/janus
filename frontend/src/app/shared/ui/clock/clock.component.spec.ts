import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ClockComponent } from './clock.component';

describe('ClockComponent', () => {
	let fixture: ComponentFixture<ClockComponent>;
	let component: ClockComponent;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [ClockComponent], // standalone
		}).compileComponents();

		fixture = TestBed.createComponent(ClockComponent);
		component = fixture.componentInstance;
	});

	afterEach(() => {
		jasmine.clock().uninstall?.();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should render a time string after init', fakeAsync(() => {
		spyOn(Date.prototype, 'toLocaleTimeString').and.returnValue('10:15:30');
		fixture.detectChanges(); // triggers ngOnInit
		tick(); // allow first update
		fixture.detectChanges();
		const el: HTMLElement = fixture.nativeElement;
		expect(el.querySelector('.clock-time')?.textContent?.trim()).toBe('10:15:30');
	}));

	it('should prefer @Input locale over navigator.language', fakeAsync(() => {
		const spy = spyOn(Date.prototype, 'toLocaleTimeString').and.returnValue('x');
		component.locale = 'fr-FR';
		fixture.detectChanges();
		tick(0);
		expect(spy).toHaveBeenCalledWith('fr-FR', jasmine.objectContaining({ hour12: false }));
	}));

	it('should fall back to navigator.language when locale is not provided', fakeAsync(() => {
		// Mock navigator.language
		const langSpy = spyOnProperty(navigator, 'language', 'get').and.returnValue('es-ES');
		const fmtSpy = spyOn(Date.prototype, 'toLocaleTimeString').and.returnValue('x');

		fixture.detectChanges();
		tick(0);

		expect(langSpy).toHaveBeenCalled();
		expect(fmtSpy).toHaveBeenCalledWith('es-ES', jasmine.any(Object));
	}));

	it('should respect 12-hour format when use12Hour = true', fakeAsync(() => {
		const spy = spyOn(Date.prototype, 'toLocaleTimeString').and.returnValue('x');
		component.use12Hour = true;
		fixture.detectChanges();
		tick(0);
		expect(spy).toHaveBeenCalledWith(jasmine.any(String), jasmine.objectContaining({ hour12: true }));
	}));

	it('should update the time every second', fakeAsync(() => {
		const spy = spyOn(Date.prototype, 'toLocaleTimeString').and.returnValues('t0', 't1', 't2', 't3');
		fixture.detectChanges(); // ngOnInit + setInterval
		tick(0);   // first call
		tick(1000); // second
		tick(1000); // third
		expect(spy.calls.count()).toBe(3); // initial + 2 ticks = 3 calls
	}));

	it('should clear the interval on destroy', fakeAsync(() => {
		const clearSpy = spyOn(window, 'clearInterval').and.callThrough();
		fixture.detectChanges();
		tick(0);
		component.ngOnDestroy();
		expect(clearSpy).toHaveBeenCalled();
	}));
});
