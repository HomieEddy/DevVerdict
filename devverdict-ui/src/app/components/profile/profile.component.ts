import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../services/auth.service';
import { ReviewService } from '../../services/review.service';
import { FrameworkService } from '../../services/framework.service';
import { Review } from '../../models/review.model';
import { Framework } from '../../models/framework.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatProgressBarModule
  ],
  templateUrl: './profile.component.html'
})
export class ProfileComponent {
  private readonly authService = inject(AuthService);
  private readonly reviewService = inject(ReviewService);
  private readonly frameworkService = inject(FrameworkService);

  readonly user = computed(() => this.authService.currentUser());
  readonly reviews = signal<Review[]>([]);
  readonly reviewsLoading = signal(false);
  readonly reviewsError = signal<string | null>(null);
  readonly frameworksMap = signal<Map<number, string>>(new Map());

  constructor() {
    this.loadFrameworks();
    effect(() => {
      const userId = this.authService.currentUser()?.id;
      if (userId) {
        this.loadReviews(userId);
      } else {
        this.reviews.set([]);
      }
    });
  }

  private loadFrameworks(): void {
    this.frameworkService.fetchAllFrameworks()
      .then((frameworks: Framework[]) => {
        const map = new Map<number, string>();
        frameworks.forEach((f: Framework) => map.set(f.id, f.name));
        this.frameworksMap.set(map);
      })
      .catch(() => {
        // silently fail — framework names are optional UX enhancement
      });
  }

  getFrameworkName(frameworkId: number): string {
    return this.frameworksMap().get(frameworkId) || `Framework #${frameworkId}`;
  }

  private loadReviews(userId: number): void {
    this.reviewsLoading.set(true);
    this.reviewsError.set(null);
    this.reviewService.getReviewsByUser(userId).subscribe({
      next: (data) => {
        this.reviews.set(data);
        this.reviewsLoading.set(false);
      },
      error: (err) => {
        this.reviewsError.set(err.error?.message || 'Failed to load reviews');
        this.reviewsLoading.set(false);
      }
    });
  }
}
