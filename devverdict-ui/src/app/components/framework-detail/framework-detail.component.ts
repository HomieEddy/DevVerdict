import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
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
export class FrameworkDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly frameworkService = inject(FrameworkService);
  private readonly reviewService = inject(ReviewService);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly frameworkId = Number(this.route.snapshot.paramMap.get('id'));
  readonly frameworkResource = this.frameworkService.getFrameworkById(this.frameworkId);
  readonly reviewsResource = this.reviewService.getReviewsByFramework(this.frameworkId);

  readonly comment = signal('');
  readonly rating = signal(0);
  readonly hoveredRating = signal(0);
  readonly isSubmitting = signal(false);
  readonly isLoggedIn = computed(() => this.authService.isAuthenticated());
  readonly currentUserId = computed(() => this.authService.currentUser()?.id);

  canEditReview(review: any): boolean {
    return review.userId === this.currentUserId();
  }

  getTypeColor(type: string): string {
    switch (type.toLowerCase()) {
      case 'language': return 'primary';
      case 'framework': return 'accent';
      case 'runtime': return 'warn';
      default: return '';
    }
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
        username: this.authService.currentUser()?.username
      });

      this.comment.set('');
      this.rating.set(0);
      this.showSuccess('Review submitted successfully!');

      this.reviewsResource.reload();
      this.frameworkResource.reload();
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
      this.reviewsResource.reload();
      this.frameworkResource.reload();
    } catch (err: any) {
      this.showError(err.error?.message || 'Failed to delete review.');
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
