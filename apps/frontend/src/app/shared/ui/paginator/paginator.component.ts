/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

/**
 * Compact, generic paginator for tabular data.
 *
 * <p>
 * The paginator displays a compact summary in the form:
 * {@code < 1-10 de 123 >}
 * and emits page changes so parent components can decide how to load/slice data.
 * </p>
 */
@Component({
  selector: 'app-paginator',
  standalone: true,
  templateUrl: './paginator.component.html',
  styleUrl: './paginator.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginatorComponent {
  /**
   * Total amount of available items.
   */
  readonly totalItems = input.required<number>();

  /**
   * Number of items shown per page.
   */
  readonly pageSize = input.required<number>();

  /**
   * Current page (1-based).
   */
  readonly currentPage = input.required<number>();

  /**
   * Text displayed between range and total count.
   */
  readonly betweenLabel = input('de');

  /**
   * Accessible label for the previous-page button.
   */
  readonly previousLabel = input('Página anterior');

  /**
   * Accessible label for the next-page button.
   */
  readonly nextLabel = input('Página siguiente');

  /**
   * Emits the next page (1-based) when navigation is requested.
   */
  readonly pageChange = output<number>();

  /**
   * Total number of available pages.
   */
  protected readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.totalItems() / this.pageSize())),
  );

  /**
   * First visible item index in the current page (1-based).
   */
  protected readonly startItem = computed(() => {
    if (this.totalItems() === 0) {
      return 0;
    }

    return (this.currentPage() - 1) * this.pageSize() + 1;
  });

  /**
   * Last visible item index in the current page (1-based).
   */
  protected readonly endItem = computed(() =>
    Math.min(this.currentPage() * this.pageSize(), this.totalItems()),
  );

  /**
   * Whether there is a previous page available.
   */
  protected readonly hasPreviousPage = computed(() => this.currentPage() > 1);

  /**
   * Whether there is a next page available.
   */
  protected readonly hasNextPage = computed(() => this.currentPage() < this.totalPages());

  /**
   * Requests navigation to the previous page if available.
   */
  protected goToPreviousPage(): void {
    if (!this.hasPreviousPage()) {
      return;
    }

    this.pageChange.emit(this.currentPage() - 1);
  }

  /**
   * Requests navigation to the next page if available.
   */
  protected goToNextPage(): void {
    if (!this.hasNextPage()) {
      return;
    }

    this.pageChange.emit(this.currentPage() + 1);
  }
}
