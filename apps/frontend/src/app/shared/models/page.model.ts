/**
 * SPDX-License-Identifier: Apache-2.0
 */
export interface Page<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
