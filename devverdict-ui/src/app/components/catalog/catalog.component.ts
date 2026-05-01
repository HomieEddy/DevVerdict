import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatSliderModule } from '@angular/material/slider';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { FrameworkService } from '../../services/framework.service';
import { FrameworkCardComponent } from '../framework-card/framework-card.component';

@Component({
  selector: 'app-catalog',
  imports: [
    MatProgressBarModule,
    MatSnackBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatSliderModule,
    FormsModule,
    FrameworkCardComponent
  ],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss'
})
export class CatalogComponent implements OnInit, OnDestroy {
  private readonly frameworkService = inject(FrameworkService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly searchTerm = signal('');
  readonly selectedType = signal<string>('');
  readonly minRating = signal<number | null>(null);
  readonly availableTypes = ['Language', 'Framework', 'Runtime', 'Library', 'Tool'];

  private readonly searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;

  frameworksResource = this.frameworkService.getAllFrameworks();

  constructor() {
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(term => {
      this.searchTerm.set(term);
      this.performSearch();
    });
  }

  ngOnInit() {
    const params = this.route.snapshot.queryParams;
    const name = params['search'] || '';
    const type = params['type'] || '';
    const rating = params['minRating'] ? parseFloat(params['minRating']) : null;

    this.searchTerm.set(name);
    this.selectedType.set(type);
    this.minRating.set(rating);

    if (name || type || rating !== null) {
      this.frameworksResource = this.frameworkService.searchFrameworks(
        name || undefined, type || undefined, rating
      );
    }
  }

  ngOnDestroy() {
    this.searchSubscription?.unsubscribe();
    this.searchSubject.complete();
  }

  onSearchChange(value: string): void {
    this.searchSubject.next(value);
  }

  onTypeChange(value: string): void {
    this.selectedType.set(value);
    this.performSearch();
  }

  onRatingChange(value: number): void {
    this.minRating.set(value);
    this.performSearch();
  }

  clearSearch(): void {
    this.searchTerm.set('');
    this.searchSubject.next('');
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedType.set('');
    this.minRating.set(null);
    this.frameworksResource = this.frameworkService.getAllFrameworks();
    this.router.navigate([], { queryParams: {} });
  }

  hasActiveFilters(): boolean {
    return !!(this.searchTerm() || this.selectedType() || this.minRating() !== null);
  }

  private performSearch(): void {
    const name = this.searchTerm() || undefined;
    const type = this.selectedType() || undefined;
    const rating = this.minRating();

    this.frameworksResource = this.frameworkService.searchFrameworks(name, type, rating ?? undefined);

    const queryParams: any = {};
    if (name) queryParams.search = name;
    if (type) queryParams.type = type;
    if (rating !== null && rating !== undefined) queryParams.minRating = rating;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      replaceUrl: true
    });
  }

  showError(message: string): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: 5000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom'
    });
  }
}
