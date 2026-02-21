import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CardComponent } from './card.component';

/** Host 1: only title, no body, no footer */
@Component({
	template: `<app-card title="Clock in"></app-card>`
})
class HostOnlyTitleComponent { }

/** Host 2: title + extra in header, no body, no footer */
@Component({
	template: `
    <app-card title="Users">
      <div card-header><button>Refresh</button></div>
    </app-card>
  `
})
class HostTitleAndHeaderExtraComponent { }

/** Host 3: body only */
@Component({
	template: `
    <app-card>
      <p>Body content</p>
    </app-card>
  `
})
class HostOnlyBodyComponent { }

/** Host 4: header + body + footer */
@Component({
	template: `
    <app-card title="Report">
      <div card-header>Active filters: Q3</div>
      <p>Content…</p>
      <div card-footer>Page 1/3</div>
    </app-card>
  `
})
class HostAllSectionsComponent { }

/** Host 5: nothing projected and no title */
@Component({
	template: `<app-card></app-card>`
})
class HostEmptyComponent { }

describe('CardComponent content projection', () => {
	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				CardComponent,
				HostOnlyTitleComponent,
				HostTitleAndHeaderExtraComponent,
				HostOnlyBodyComponent,
				HostAllSectionsComponent,
				HostEmptyComponent
			]
		}).compileComponents();
	});

	function q(fixture: ComponentFixture<any>, selector: string): HTMLElement | null {
		return fixture.debugElement.nativeElement.querySelector(selector);
	}

	it('renders header when title is present, without extra header content', () => {
		const fixture = TestBed.createComponent(HostOnlyTitleComponent);
		fixture.detectChanges();

		const header = q(fixture, '.card-header');
		const titleEl = q(fixture, '.card-title');
		const footer = q(fixture, '.card-footer');

		expect(header).withContext('header should exist').not.toBeNull();
		expect(titleEl?.textContent?.trim()).toBe('Clock in');
		expect(footer).withContext('footer should not exist').toBeNull();
	});

	it('renders header with title and [card-header] content', () => {
		const fixture = TestBed.createComponent(HostTitleAndHeaderExtraComponent);
		fixture.detectChanges();

		const header = q(fixture, '.card-header');
		const titleEl = q(fixture, '.card-title');

		expect(header).not.toBeNull();
		expect(titleEl?.textContent?.trim()).toBe('Users');
		expect(header!.textContent).toContain('Refresh');
	});

	it('does not render header when there is no title and no [card-header]', () => {
		const fixture = TestBed.createComponent(HostEmptyComponent);
		fixture.detectChanges();

		const header = q(fixture, '.card-header');
		expect(header).toBeNull();
	});

	it('renders footer when [card-footer] is present', () => {
		const fixture = TestBed.createComponent(HostAllSectionsComponent);
		fixture.detectChanges();

		const footer = q(fixture, '.card-footer');
		expect(footer).not.toBeNull();
		expect(footer!.textContent).toContain('Page 1/3');
	});

	it('does not render footer when [card-footer] is absent', () => {
		const fixture = TestBed.createComponent(HostOnlyBodyComponent);
		fixture.detectChanges();

		const footer = q(fixture, '.card-footer');
		expect(footer).toBeNull();
	});

	it('keeps body empty when no projected content is provided', () => {
		const fixture = TestBed.createComponent(HostOnlyTitleComponent);
		fixture.detectChanges();

		const body = q(fixture, '.card-body');
		expect(body).not.toBeNull();
		// CSS :empty is not evaluated in tests; check logical emptiness.
		expect(body!.textContent?.trim()).toBe('');
	});

	it('shows body content when provided', () => {
		const fixture = TestBed.createComponent(HostOnlyBodyComponent);
		fixture.detectChanges();

		const body = q(fixture, '.card-body');
		expect(body).not.toBeNull();
		expect(body!.textContent).toContain('Body content');
	});

	it('renders header, body, and footer when all are present', () => {
		const fixture = TestBed.createComponent(HostAllSectionsComponent);
		fixture.detectChanges();

		expect(q(fixture, '.card-header')).not.toBeNull();
		expect(q(fixture, '.card-title')!.textContent?.trim()).toBe('Report');
		expect(q(fixture, '.card-body')!.textContent).toContain('Content…');
		expect(q(fixture, '.card-footer')).not.toBeNull();
	});
});
