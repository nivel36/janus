/**
 * SPDX-License-Identifier: Apache-2.0
 */
interface Page<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
