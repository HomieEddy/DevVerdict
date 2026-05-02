import { Component, inject, signal, OnInit, OnDestroy, computed } from '@angular/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatSliderModule } from '@angular/material/slider';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { FrameworkService } from '../../services/framework.service';
import { FrameworkCardComponent } from '../framework-card/framework-card.component';
import { Framework } from '../../models/framework.model';

@Component({
  selector: 'app-catalog',
  imports: [
    MatProgressBarModule,
    MatSnackBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatSelectModule,
    MatSliderModule,
    MatPaginatorModule,
    FormsModule,
    FrameworkCardComponent
  ],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss'
})
export class CatalogComponent implements OnInit, OnDestroy {
  private readonly frameworkService = inject(FrameworkService);
  private readonly snackBar = inject(MatSnackBar);

  readonly searchTerm = signal('');
  readonly selectedType = signal<string>('');
  readonly minRating = signal<number | null>(null);
  readonly availableTypes = signal<string[]>([]);

  readonly frameworks = signal<Framework[]>([]);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);

  readonly pageIndex = signal(0);
  readonly pageSize = signal(25);
  readonly pageSizeOptions = [10, 25, 50];

  readonly paginatedFrameworks = computed(() => {
    const all = this.frameworks();
    const start = this.pageIndex() * this.pageSize();
    return all.slice(start, start + this.pageSize());
  });

  readonly totalCount = computed(() => this.frameworks().length);

  private readonly searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;

  constructor() {
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(term => {
      this.searchTerm.set(term);
      this.pageIndex.set(0);
      this.performSearch();
    });
  }

  async ngOnInit() {
    await this.loadFrameworkTypes();
    await this.loadAllFrameworks();
  }

  ngOnDestroy() {
    this.searchSubscription?.unsubscribe();
    this.searchSubject.complete();
  }

  private async loadFrameworkTypes() {
    try {
      const types = await this.frameworkService.fetchFrameworkTypes();
      this.availableTypes.set(types);
    } catch (err) {
      console.error('Failed to load framework types', err);
    }
  }

  private async loadAllFrameworks() {
    this.isLoading.set(true);
    this.error.set(null);
    try {
      const results = await this.frameworkService.fetchAllFrameworks();
      this.frameworks.set(results);
    } catch (err: any) {
      this.error.set(err.message || 'Failed to load frameworks');
    } finally {
      this.isLoading.set(false);
    }
  }

  onSearchChange(value: string): void {
    this.searchSubject.next(value);
  }

  onTypeChange(value: string): void {
    this.selectedType.set(value);
    this.pageIndex.set(0);
    this.performSearch();
  }

  onSliderChange(event: Event): void {
    const value = (event.target as HTMLInputElement).valueAsNumber;
    this.minRating.set(value);
    this.pageIndex.set(0);
    this.performSearch();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  clearSearch(): void {
    this.searchTerm.set('');
    this.searchSubject.next('');
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedType.set('');
    this.minRating.set(null);
    this.pageIndex.set(0);
    this.loadAllFrameworks();
  }

  hasActiveFilters(): boolean {
    return !!(this.searchTerm() || this.selectedType() || this.minRating() !== null);
  }

  private async performSearch(): Promise<void> {
    const name = this.searchTerm() || undefined;
    const type = this.selectedType() || undefined;
    const rating = this.minRating();

    this.isLoading.set(true);
    this.error.set(null);
    try {
      const results = await this.frameworkService.fetchSearchResults(name, type, rating ?? undefined);
      this.frameworks.set(results);
    } catch (err: any) {
      this.error.set(err.message || 'Search failed');
    } finally {
      this.isLoading.set(false);
    }
  }

  showError(message: string): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: 5000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom'
    });
  }
}
