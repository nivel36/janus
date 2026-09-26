/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { CommonModule, DOCUMENT } from '@angular/common';
import { ActiveDescendantKeyManager, LiveAnnouncer } from '@angular/cdk/a11y';
import { ConnectedPosition, OverlayModule } from '@angular/cdk/overlay';
import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  ViewChild,
  computed,
  inject,
  input,
  isDevMode,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ButtonComponent } from '../button/button.component';
import { InputGroupComponent } from '../input-group/input-group.component';
import { InputComponent } from '../input/input.component';
import {
  AUTOCOMPLETE_OPTION_CONTROLLER,
  AutocompleteOptionController,
  AutocompleteOptionDirective,
} from './autocomplete-option.directive';

/**
 * Presentation-only autocomplete control.
 *
 * Data fetching deliberately lives outside this class. Consumers provide the current
 * items/loading/error state and react to queryChange. Form serialization is supplied by
 * AutocompleteValueAccessorDirective, keeping this control useful without Angular forms.
 */
@Component({
  selector: 'app-autocomplete-textbox',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    OverlayModule,
    TranslatePipe,
    InputComponent,
    InputGroupComponent,
    ButtonComponent,
    AutocompleteOptionDirective,
  ],
  templateUrl: './autocomplete-textbox.component.html',
  styleUrl: './autocomplete-textbox.component.css',
  providers: [
    {
      provide: AUTOCOMPLETE_OPTION_CONTROLLER,
      useExisting: AutocompleteTextboxComponent,
    },
  ],
})
export class AutocompleteTextboxComponent<T = unknown>
  implements AfterViewInit, AutocompleteOptionController
{
  @ViewChild('inputWrapper', { static: true }) private inputWrapper!: ElementRef<HTMLElement>;

  readonly items = input<readonly T[]>([]);
  readonly loading = input(false);
  readonly error = input<unknown | null>(null);
  /** Whether the data owner has reached the point at which feedback may be displayed. */
  readonly panelOpen = input(true);
  readonly displayWith = input<(option: T) => string>((option) => String(option));
  readonly trackByValueInput = input<(option: T) => string | number>();
  readonly trackByValue = computed<(option: T) => string | number>(
    () => this.trackByValueInput() ?? ((option) => this.displayWith()(option)),
  );
  readonly placeholder = input<string>();
  readonly emptyHint = input<string>();
  readonly inputId = input<string>();
  readonly ariaLabel = input<string>();
  readonly ariaLabelledBy = input<string>();
  readonly ariaDescribedBy = input<string | null>();
  readonly ariaInvalid = input(false);
  readonly clearButtonAriaLabel = input<string>();

  readonly queryChange = output<string>();
  readonly selectedChange = output<T | null>();
  readonly touched = output<void>();

  readonly textControl = new FormControl('', { nonNullable: true });
  private readonly selectionState = signal<T | null>(null);
  private readonly disabledState = signal(false);
  private readonly panelDismissed = signal(true);
  private readonly overlayWidthState = signal(0);
  private readonly resultOptions: AutocompleteOptionDirective[] = [];
  private keyManager?: ActiveDescendantKeyManager<AutocompleteOptionDirective>;

  private static nextInstanceId = 0;
  private readonly instanceId = AutocompleteTextboxComponent.nextInstanceId++;
  protected readonly optionIdPrefix = `autocomplete-option-${this.instanceId}`;
  private readonly generatedInputId = `autocomplete-input-${this.instanceId}`;
  readonly controlId = computed(() => this.inputId() ?? this.generatedInputId);
  protected readonly statusMessageId = `autocomplete-status-${this.instanceId}`;
  protected readonly resultsListId = `autocomplete-results-${this.instanceId}`;
  protected readonly overlayPositions: ConnectedPosition[] = [
    { originX: 'start', originY: 'bottom', overlayX: 'start', overlayY: 'top', offsetY: 4 },
    { originX: 'start', originY: 'top', overlayX: 'start', overlayY: 'bottom', offsetY: -4 },
  ];

  private readonly translateService = inject(TranslateService);
  private readonly liveAnnouncer = inject(LiveAnnouncer);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);

  constructor() {
    this.textControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((value) => {
      if (this.hasSelection && value.trim() !== this.displayWith()(this.selectedValue!).trim()) {
        this.selectionState.set(null);
        this.selectedChange.emit(null);
      }
      this.panelDismissed.set(!value.trim());
      this.clearActiveOption();
      this.queryChange.emit(value.trim());
    });
    this.destroyRef.onDestroy(() => {
      this.keyManager?.destroy();
      this.liveAnnouncer.clear();
    });
    if (isDevMode() && !this.ariaLabel()?.trim() && !this.ariaLabelledBy()?.trim()) {
      queueMicrotask(() => {
        if (!this.ariaLabel()?.trim() && !this.ariaLabelledBy()?.trim()) {
          console.warn(this.translateService.instant('autocomplete.missingAccessibleNameWarning'));
        }
      });
    }
  }

  get selectedValue(): T | null {
    return this.selectionState();
  }
  get disabled(): boolean {
    return this.disabledState();
  }
  get overlayWidth(): number {
    return this.overlayWidthState();
  }
  get hasSelection(): boolean {
    return this.selectedValue !== null;
  }
  get isLoading(): boolean {
    return this.loading();
  }
  get isOverlayOpen(): boolean {
    return this.panelOpen() && !this.panelDismissed() && !this.disabled && !this.hasSelection;
  }
  get hasResults(): boolean {
    return this.items().length > 0;
  }
  get results(): readonly T[] {
    return this.items();
  }
  get panelKind(): 'loading' | 'results' | 'empty' | 'error' {
    if (this.loading()) return 'loading';
    if (this.error() !== null) return 'error';
    return this.hasResults ? 'results' : 'empty';
  }

  ngAfterViewInit(): void {
    this.updateOverlayWidth();
    const ResizeObserverConstructor = this.document.defaultView?.ResizeObserver;
    if (!ResizeObserverConstructor) return;
    const observer = new ResizeObserverConstructor((entries) => {
      const width = entries[0]?.contentRect.width;
      if (width !== undefined && width !== this.overlayWidth) {
        this.overlayWidthState.set(width);
      }
    });
    observer.observe(this.inputWrapper.nativeElement);
    this.destroyRef.onDestroy(() => observer.disconnect());
  }

  private updateOverlayWidth(): void {
    this.overlayWidthState.set(this.inputWrapper?.nativeElement.getBoundingClientRect().width ?? 0);
  }
  registerOption(option: AutocompleteOptionDirective): void {
    this.resultOptions.push(option);
    this.recreateKeyManager();
  }
  unregisterOption(option: AutocompleteOptionDirective): void {
    const index = this.resultOptions.indexOf(option);
    if (index >= 0) this.resultOptions.splice(index, 1);
    this.recreateKeyManager();
  }
  private recreateKeyManager(): void {
    this.keyManager?.destroy();
    this.keyManager = new ActiveDescendantKeyManager(this.resultOptions)
      .withWrap()
      .withVerticalOrientation();
  }
  onInputKeydown(event: KeyboardEvent): void {
    if (this.disabled) return;
    if (event.key === 'Enter' && this.keyManager?.activeItemIndex != null && this.hasResults) {
      event.preventDefault();
      this.onSelect(this.results[this.keyManager.activeItemIndex]);
    } else if (event.key === 'Escape' && this.isOverlayOpen) {
      event.preventDefault();
      this.closeOverlay();
    } else if (this.hasResults) {
      this.keyManager?.onKeydown(event);
    }
  }
  onOutsideClick(): void {
    if (this.isOverlayOpen) this.closeOverlay();
  }
  private closeOverlay(): void {
    this.panelDismissed.set(true);
    this.clearActiveOption();
    void this.liveAnnouncer.announce(
      this.translateService.instant('autocomplete.resultsClosed'),
      'polite',
    );
  }
  onOptionPointerDown(event: PointerEvent): void {
    event.preventDefault();
  }
  onClearButtonPointerDown(event: PointerEvent): void {
    event.preventDefault();
  }
  onSelect(option: T): void {
    if (this.disabled) return;
    this.setSelection(option);
    this.selectedChange.emit(option);
    this.touched.emit();
  }
  clearSelection(): void {
    if (this.disabled) return;
    this.setSelection(null);
    this.selectedChange.emit(null);
    this.touched.emit();
    this.queryChange.emit('');
  }
  setSelection(option: T | null, fallbackText = ''): void {
    this.selectionState.set(option);
    this.panelDismissed.set(true);
    this.clearActiveOption();
    this.textControl.setValue(option !== null ? this.displayWith()(option) : fallbackText, {
      emitEvent: false,
    });
  }
  setDisabledState(disabled: boolean): void {
    this.disabledState.set(disabled);
    if (disabled) {
      this.textControl.disable({ emitEvent: false });
    } else {
      this.textControl.enable({ emitEvent: false });
    }
    if (disabled) this.panelDismissed.set(true);
  }
  handleBlur(): void {
    this.touched.emit();
    if (this.isOverlayOpen) this.closeOverlay();
  }
  isActive(index: number): boolean {
    return this.keyManager?.activeItemIndex === index;
  }
  get activeDescendantId(): string | null {
    return this.keyManager?.activeItem?.id ?? null;
  }
  private clearActiveOption(): void {
    this.keyManager?.setActiveItem(-1);
  }
  getOptionId(index: number): string {
    return `${this.optionIdPrefix}-${index}`;
  }
  get liveRegionMessage(): string {
    if (!this.isOverlayOpen) return '';
    if (this.loading()) return this.translateService.instant('autocomplete.loadingResults');
    if (this.error() !== null || !this.hasResults) {
      return this.translateService.instant('autocomplete.noResultsFound');
    }
    return this.translateService.instant(
      this.items().length === 1
        ? 'autocomplete.oneResultAvailable'
        : 'autocomplete.manyResultsAvailable',
      { count: this.items().length },
    );
  }
}
