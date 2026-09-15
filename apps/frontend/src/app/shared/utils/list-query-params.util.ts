/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { effect, type EffectRef, type Signal } from '@angular/core';
import { Params } from '@angular/router';

export const DEFAULT_LIST_PAGE = 1;
export const DEFAULT_LIST_PAGE_SIZE = 5;

export function normalizeListQuery(value: string | undefined): string {
  return value?.trim() ?? '';
}

export function normalizeListPage(value: string | number | undefined): number {
  const page = typeof value === 'number' ? value : Number(value);
  return Number.isInteger(page) && page >= DEFAULT_LIST_PAGE ? page : DEFAULT_LIST_PAGE;
}

export function listQueryParams(query: string, page = DEFAULT_LIST_PAGE): Params {
  return {
    query: normalizeListQuery(query) || null,
    page: page === DEFAULT_LIST_PAGE ? null : page,
  };
}

export function emptyListPage<T>(pageSize = DEFAULT_LIST_PAGE_SIZE): {
  items: T[];
  totalItems: number;
  page: number;
  pageSize: number;
  totalPages: number;
} {
  return { items: [], totalItems: 0, page: 0, pageSize, totalPages: 0 };
}

export function synchronizeListPage(
  page: Signal<number>,
  totalItems: Signal<number>,
  isLoading: Signal<boolean>,
  onPageChange: (page: number) => void,
  pageSize = DEFAULT_LIST_PAGE_SIZE,
): EffectRef {
  return effect(() => {
    if (isLoading()) {
      return;
    }

    const maxPage = Math.max(DEFAULT_LIST_PAGE, Math.ceil(totalItems() / pageSize));
    if (page() > maxPage) {
      onPageChange(maxPage);
    }
  });
}
