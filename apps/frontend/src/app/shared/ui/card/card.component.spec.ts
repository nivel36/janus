/**
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { CardComponent } from './card.component';

@Component({
  standalone: true,
  imports: [CardComponent],
  template: `
    <app-card
      [title]="title"
      [styleClass]="styleClass"
      [titleClass]="titleClass"
      [headerClass]="headerClass"
      [footerClass]="footerClass"
    >
      <ng-template #cardHeader>
        <div class="projected-header">Projected header</div>
      </ng-template>

      <p class="projected-body">Projected body</p>

      <ng-template #cardFooter>
        <div class="projected-footer">Projected footer</div>
      </ng-template>
    </app-card>
  `,
})
class TestHostComponent {
  title: string | null = null;
  styleClass = '';
  titleClass = '';
  headerClass = '';
  footerClass = '';
}

@Component({
  standalone: true,
  imports: [CardComponent],
  template: `
    <app-card
      [title]="title"
      [styleClass]="styleClass"
      [titleClass]="titleClass"
      [headerClass]="headerClass"
      [footerClass]="footerClass"
    >
      <p class="projected-body">Projected body only</p>
    </app-card>
  `,
})
class TestHostWithoutTemplatesComponent {
  title: string | null = null;
  styleClass = '';
  titleClass = '';
  headerClass = '';
  footerClass = '';
}

describe('CardComponent', () => {
  describe('with projected header and footer templates', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let hostComponent: TestHostComponent;

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [TestHostComponent],
      }).compileComponents();

      fixture = TestBed.createComponent(TestHostComponent);
      hostComponent = fixture.componentInstance;
    });

    it('should create', () => {
      fixture.detectChanges();

      const cardDebugElement = fixture.debugElement.query(By.directive(CardComponent));
      const cardComponent = cardDebugElement.componentInstance as CardComponent;

      expect(cardComponent).toBeTruthy();
    });

    it('should render the projected body content', () => {
      fixture.detectChanges();

      const bodyContent = fixture.nativeElement.querySelector('.projected-body');

      expect(bodyContent).toBeTruthy();
      expect(bodyContent.textContent.trim()).toBe('Projected body');
    });

    it('should render the title when provided', () => {
      hostComponent.title = 'My card title';
      fixture.detectChanges();

      const titleElement = fixture.nativeElement.querySelector('.card-title');

      expect(titleElement).toBeTruthy();
      expect(titleElement.textContent.trim()).toBe('My card title');
    });

    it('should render the projected header template', () => {
      fixture.detectChanges();

      const projectedHeader = fixture.nativeElement.querySelector('.projected-header');

      expect(projectedHeader).toBeTruthy();
      expect(projectedHeader.textContent.trim()).toBe('Projected header');
    });

    it('should render the projected footer template', () => {
      fixture.detectChanges();

      const projectedFooter = fixture.nativeElement.querySelector('.projected-footer');

      expect(projectedFooter).toBeTruthy();
      expect(projectedFooter.textContent.trim()).toBe('Projected footer');
    });

    it('should apply custom CSS classes', () => {
      hostComponent.title = 'Styled card';
      hostComponent.styleClass = 'custom-card';
      hostComponent.titleClass = 'custom-title';
      hostComponent.headerClass = 'custom-header';
      hostComponent.footerClass = 'custom-footer';

      fixture.detectChanges();

      const cardElement = fixture.nativeElement.querySelector('.card');
      const titleElement = fixture.nativeElement.querySelector('.card-title');
      const headerElement = fixture.nativeElement.querySelector('.card-header');
      const footerElement = fixture.nativeElement.querySelector('.card-footer');

      expect(cardElement.classList.contains('custom-card')).toBeTrue();
      expect(titleElement.classList.contains('custom-title')).toBeTrue();
      expect(headerElement.classList.contains('custom-header')).toBeTrue();
      expect(footerElement.classList.contains('custom-footer')).toBeTrue();
    });

    it('should set hasHeader to true when a header template exists', () => {
      fixture.detectChanges();

      const cardDebugElement = fixture.debugElement.query(By.directive(CardComponent));
      const cardComponent = cardDebugElement.componentInstance as CardComponent;

      expect(cardComponent.hasHeader).toBeTrue();
    });

    it('should set hasFooter to true when a footer template exists', () => {
      fixture.detectChanges();

      const cardDebugElement = fixture.debugElement.query(By.directive(CardComponent));
      const cardComponent = cardDebugElement.componentInstance as CardComponent;

      expect(cardComponent.hasFooter).toBeTrue();
    });

    it('should render the header element when a header template exists even if title is null', () => {
      fixture.detectChanges();

      const headerElement = fixture.nativeElement.querySelector('.card-header');

      expect(headerElement).toBeTruthy();
    });

    it('should render the footer element when a footer template exists', () => {
      fixture.detectChanges();

      const footerElement = fixture.nativeElement.querySelector('.card-footer');

      expect(footerElement).toBeTruthy();
    });
  });

  describe('without projected header and footer templates', () => {
    let fixture: ComponentFixture<TestHostWithoutTemplatesComponent>;
    let hostComponent: TestHostWithoutTemplatesComponent;

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [TestHostWithoutTemplatesComponent],
      }).compileComponents();

      fixture = TestBed.createComponent(TestHostWithoutTemplatesComponent);
      hostComponent = fixture.componentInstance;
    });

    it('should not render the header when there is no title and no header template', () => {
      fixture.detectChanges();

      const headerElement = fixture.nativeElement.querySelector('.card-header');
      const cardDebugElement = fixture.debugElement.query(By.directive(CardComponent));
      const cardComponent = cardDebugElement.componentInstance as CardComponent;

      expect(headerElement).toBeNull();
      expect(cardComponent.hasHeader).toBeFalse();
    });

    it('should render the header when title exists even if there is no header template', () => {
      hostComponent.title = 'Title only';
      fixture.detectChanges();

      const headerElement = fixture.nativeElement.querySelector('.card-header');
      const titleElement = fixture.nativeElement.querySelector('.card-title');
      const cardDebugElement = fixture.debugElement.query(By.directive(CardComponent));
      const cardComponent = cardDebugElement.componentInstance as CardComponent;

      expect(headerElement).toBeTruthy();
      expect(titleElement).toBeTruthy();
      expect(titleElement.textContent.trim()).toBe('Title only');
      expect(cardComponent.hasHeader).toBeTrue();
    });

    it('should not render the footer when there is no footer template', () => {
      fixture.detectChanges();

      const footerElement = fixture.nativeElement.querySelector('.card-footer');
      const cardDebugElement = fixture.debugElement.query(By.directive(CardComponent));
      const cardComponent = cardDebugElement.componentInstance as CardComponent;

      expect(footerElement).toBeNull();
      expect(cardComponent.hasFooter).toBeFalse();
    });

    it('should keep the body content rendered when there is no header or footer', () => {
      fixture.detectChanges();

      const bodyContent = fixture.nativeElement.querySelector('.projected-body');

      expect(bodyContent).toBeTruthy();
      expect(bodyContent.textContent.trim()).toBe('Projected body only');
    });
  });
});
