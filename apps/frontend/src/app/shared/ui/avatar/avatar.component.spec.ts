/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AvatarComponent, AvatarSize } from './avatar.component';

@Component({
  standalone: true,
  imports: [AvatarComponent],
  template: `
    <app-avatar [src]="src" [alt]="alt" [size]="size" [styleClass]="styleClass"></app-avatar>
  `,
})
class TestHostComponent {
  src = 'assets/images/user.png';
  alt = 'User avatar';
  size: AvatarSize = 'medium';
  styleClass = '';
}

describe('AvatarComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let hostComponent: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    hostComponent = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render the image with the default medium size', () => {
    const avatarEl: HTMLImageElement = fixture.nativeElement.querySelector('.app-avatar');

    expect(avatarEl.getAttribute('src')).toBe('assets/images/user.png');
    expect(avatarEl.getAttribute('alt')).toBe('User avatar');
    expect(avatarEl.classList).toContain('app-avatar--medium');
  });

  it('should apply the selected size and extra classes', () => {
    hostComponent.size = 'large';
    hostComponent.styleClass = 'employee-card__avatar';
    fixture.detectChanges();

    const avatarEl: HTMLImageElement = fixture.nativeElement.querySelector('.app-avatar');

    expect(avatarEl.classList).toContain('app-avatar--large');
    expect(avatarEl.classList).toContain('employee-card__avatar');
  });

  it('should use the fallback image when the requested image fails', () => {
    hostComponent.src = 'assets/images/missing-user.png';
    fixture.detectChanges();

    const avatarEl: HTMLImageElement = fixture.nativeElement.querySelector('.app-avatar');
    avatarEl.dispatchEvent(new Event('error'));

    expect(avatarEl.getAttribute('src')).toBe('assets/images/user.png');
  });

  it('should not retry the fallback image when it also fails', () => {
    hostComponent.src = 'assets/images/missing-user.png';
    fixture.detectChanges();

    const avatarEl: HTMLImageElement = fixture.nativeElement.querySelector('.app-avatar');
    const srcSetter = vi.spyOn(avatarEl, 'src', 'set');

    avatarEl.dispatchEvent(new Event('error'));
    avatarEl.dispatchEvent(new Event('error'));

    expect(srcSetter).toHaveBeenCalledOnce();
    expect(avatarEl.getAttribute('src')).toBe('assets/images/user.png');
  });
});
