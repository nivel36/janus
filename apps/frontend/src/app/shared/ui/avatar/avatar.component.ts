/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, input } from '@angular/core';

/**
 * Size variants supported by the avatar component.
 */
export type AvatarSize = 'large' | 'medium' | 'small';

@Component({
  selector: 'app-avatar',
  standalone: true,
  imports: [],
  templateUrl: './avatar.component.html',
  styleUrl: './avatar.component.css',
})
export class AvatarComponent {
  /**
   * Image source rendered inside the avatar.
   */
  readonly src = input<string>('assets/images/user.png');

  /**
   * Accessible alternative text for informative avatars.
   */
  readonly alt = input<string>('');

  /**
   * Visual size applied to the avatar.
   */
  readonly size = input<AvatarSize>('medium');

  /**
   * Extra CSS classes appended to the root element.
   */
  readonly styleClass = input<string>('');

  /**
   * Builds the complete CSS class list for the root avatar element.
   */
  get avatarClass(): string {
    return ['app-avatar', `app-avatar--${this.size()}`, this.styleClass().trim()]
      .filter(Boolean)
      .join(' ');
  }
}
