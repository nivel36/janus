/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { KeycloakEventType } from 'keycloak-angular';
import { vi } from 'vitest';

import type { AuthTokenClaims } from '../app/core/auth/auth.models';

export function createKeycloakMock(
  overrides: Partial<Keycloak> & { tokenParsed?: AuthTokenClaims } = {},
) {
  return {
    authenticated: false,
    tokenParsed: undefined as AuthTokenClaims | undefined,
    login: vi.fn().mockResolvedValue(undefined),
    logout: vi.fn().mockResolvedValue(undefined),
    clearToken: vi.fn(),
    hasRealmRole: vi.fn(),
    hasResourceRole: vi.fn(),
    ...overrides,
  };
}

export function createKeycloakEventSignal() {
  return signal({ type: KeycloakEventType.KeycloakAngularNotInitialized });
}

export function createDocumentMock(url = 'https://janus.example/current') {
  return { defaultView: { location: new URL(url) } };
}
