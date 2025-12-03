import { NgTemplateOutlet } from '@angular/common';
import { AfterContentInit, Component, ContentChild, ElementRef, Input, TemplateRef, ViewChild } from '@angular/core';

@Component({
	selector: 'app-card',
	standalone: true,
	imports: [NgTemplateOutlet],
	templateUrl: './card.component.html',
	styleUrls: ['./card.component.css']
})
export class CardComponent implements AfterContentInit {
	@Input() title: string | null = null;
	@Input() styleClass = '';
	@Input() titleClass = '';
	@Input() headerClass = '';
	@Input() footerClass = '';

	@ContentChild('cardHeader', { read: TemplateRef }) headerTpl?: TemplateRef<unknown>;
	@ContentChild('cardFooter', { read: TemplateRef }) footerTpl?: TemplateRef<unknown>;
	@ViewChild('content', { static: false, read: ElementRef }) ngContent?: ElementRef;

	hasHeader = false;
	hasFooter = false;

	ngAfterContentInit(): void {
		this.hasHeader = !!this.headerTpl || !!this.title;
		this.hasFooter = !!this.footerTpl;
	}
}
