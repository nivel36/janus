/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, input } from '@angular/core';

/**
 * Size variants supported by the avatar component.
 */
export type AvatarSize = 'large' | 'medium' | 'small';

const DEFAULT_AVATAR_SRC = 'assets/images/user.png';

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
  readonly src = input<string>(DEFAULT_AVATAR_SRC);

  /**
   * Image displayed when the requested avatar cannot be loaded.
   */
  readonly fallbackSrc = DEFAULT_AVATAR_SRC;

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
   * Replaces a failed avatar without retrying when the fallback also fails.
   */
  onImageError(event: Event): void {
    const image = event.target as HTMLImageElement | null;

    if (!image || image.getAttribute('src') === this.fallbackSrc) {
      return;
    }

    image.src = this.fallbackSrc;
  }

  /**
   * Builds the complete CSS class list for the root avatar element.
   */
  get avatarClass(): string {
    return ['app-avatar', `app-avatar--${this.size()}`, this.styleClass().trim()]
      .filter(Boolean)
      .join(' ');
  }
}
