import { Component, computed, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { interval, Subscription, lastValueFrom } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDividerModule } from '@angular/material/divider';
import { FrameworkService } from '../../services/framework.service';
import { ReviewService } from '../../services/review.service';
import { AuthService } from '../../services/auth.service';
import { Review } from '../../models/review.model';

@Component({
  selector: 'app-framework-detail',
  imports: [
    RouterLink,
    FormsModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatDividerModule
  ],
  templateUrl: './framework-detail.component.html',
  styleUrl: './framework-detail.component.scss'
})
export class FrameworkDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly frameworkService = inject(FrameworkService);
  private readonly reviewService = inject(ReviewService);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly frameworkId = Number(this.route.snapshot.paramMap.get('id'));
  readonly frameworkResource = this.frameworkService.getFrameworkById(this.frameworkId);

  readonly reviews = signal<Review[]>([]);
  readonly reviewsLoading = signal(false);
  readonly reviewsError = signal<string | null>(null);
  readonly currentPage = signal(0);
  readonly pageSize = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  readonly comment = signal('');
  readonly pros = signal('');
  readonly cons = signal('');
  readonly rating = signal(0);
  readonly hoveredRating = signal(0);
  readonly isSubmitting = signal(false);
  readonly isLoggedIn = computed(() => this.authService.isAuthenticated());
  readonly currentUserId = computed(() => this.authService.currentUser()?.id);
  readonly lastUpdated = signal<Date | null>(null);

  private readonly pollInterval = 30000;
  private pollSubscription?: Subscription;

  ngOnInit(): void {
    this.lastUpdated.set(new Date());
    this.loadReviews();
    this.pollSubscription = interval(this.pollInterval).subscribe(() => {
      if (!this.isSubmitting()) {
        this.frameworkResource.reload();
        this.loadReviews();
        this.lastUpdated.set(new Date());
      }
    });
  }

  ngOnDestroy(): void {
    this.pollSubscription?.unsubscribe();
  }

  private loadReviews(): void {
    this.reviewsLoading.set(true);
    this.reviewsError.set(null);
    this.reviewService.getReviewsByFramework(this.frameworkId, this.currentPage(), this.pageSize())
      .subscribe({
        next: (page) => {
          this.reviews.set(page.content);
          this.totalElements.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.reviewsLoading.set(false);
        },
        error: (err) => {
          this.reviewsError.set(err.error?.message || 'Failed to load reviews');
          this.reviewsLoading.set(false);
        }
      });
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadReviews();
    }
  }

  canEditReview(review: Review): boolean {
    return review.userId === this.currentUserId();
  }

  formatTimeAgo(date: Date): string {
    const seconds = Math.floor((new Date().getTime() - date.getTime()) / 1000);
    if (seconds < 60) return 'just now';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    return `${hours}h ago`;
  }

  getTypeColor(type: string): string {
    switch (type.toLowerCase()) {
      case 'language': return 'primary';
      case 'framework': return 'accent';
      case 'runtime': return 'warn';
      default: return '';
    }
  }

  getInitials(username: string | undefined): string {
    return username ? username.charAt(0).toUpperCase() : '?';
  }

  setRating(value: number): void {
    this.rating.set(value);
  }

  setHoveredRating(value: number): void {
    this.hoveredRating.set(value);
  }

  async submitReview(): Promise<void> {
    if (!this.isLoggedIn()) {
      this.showError('Please log in to submit a review.');
      this.router.navigate(['/login']);
      return;
    }

    const currentRating = this.rating();
    const currentComment = this.comment();

    if (currentRating < 1 || currentRating > 5) {
      this.showError('Please select a rating between 1 and 5 stars.');
      return;
    }

    if (!currentComment || currentComment.trim().length === 0) {
      this.showError('Please enter a comment.');
      return;
    }

    this.isSubmitting.set(true);

    try {
      await this.reviewService.createReview({
        frameworkId: this.frameworkId,
        comment: currentComment.trim(),
        rating: currentRating,
        userId: this.currentUserId(),
        username: this.authService.currentUser()?.username,
        pros: this.pros().trim() || undefined,
        cons: this.cons().trim() || undefined
      });

      this.comment.set('');
      this.pros.set('');
      this.cons.set('');
      this.rating.set(0);
      this.showSuccess('Review submitted successfully!');

      this.loadReviews();
      this.frameworkResource.reload();
      this.lastUpdated.set(new Date());
    } catch (err: any) {
      this.showError(err.error?.message || 'Failed to submit review. Please try again.');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  async deleteReview(reviewId: number): Promise<void> {
    try {
      await this.reviewService.deleteReview(reviewId);
      this.showSuccess('Review deleted successfully!');
      this.loadReviews();
      this.frameworkResource.reload();
    } catch (err: any) {
      this.showError(err.error?.message || 'Failed to delete review.');
    }
  }

  async voteReview(reviewId: number, voteType: 'UPVOTE' | 'DOWNVOTE'): Promise<void> {
    if (!this.isLoggedIn()) {
      this.showError('Please log in to vote on reviews.');
      this.router.navigate(['/login']);
      return;
    }
    try {
      await lastValueFrom(this.reviewService.voteReview(reviewId, voteType));
      this.loadReviews();
      this.showSuccess('Vote recorded!');
    } catch (err: any) {
      this.showError(err.error?.message || 'Failed to vote. Please try again.');
    }
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom'
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: 5000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom'
    });
  }
}
