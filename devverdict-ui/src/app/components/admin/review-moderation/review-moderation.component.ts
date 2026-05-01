import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ReviewService } from '../../services/review.service';
import { Review } from '../../models/review.model';

@Component({
  selector: 'app-review-moderation',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  template: `
    <div class="review-moderation">
      <div class="header">
        <h2>Review Moderation</h2>
        <button mat-raised-button color="primary" (click)="loadReviews()">
          <mat-icon>refresh</mat-icon> Refresh
        </button>
      </div>

      <table mat-table [dataSource]="reviews()" class="mat-elevation-z2">
        <ng-container matColumnDef="id">
          <th mat-header-cell *matHeaderCellDef>ID</th>
          <td mat-cell *matCellDef="let r">{{ r.id }}</td>
        </ng-container>

        <ng-container matColumnDef="frameworkId">
          <th mat-header-cell *matHeaderCellDef>Framework</th>
          <td mat-cell *matCellDef="let r">{{ r.frameworkId }}</td>
        </ng-container>

        <ng-container matColumnDef="username">
          <th mat-header-cell *matHeaderCellDef>User</th>
          <td mat-cell *matCellDef="let r">{{ r.username || 'Anonymous' }}</td>
        </ng-container>

        <ng-container matColumnDef="rating">
          <th mat-header-cell *matHeaderCellDef>Rating</th>
          <td mat-cell *matCellDef="let r">{{ r.rating }}/5</td>
        </ng-container>

        <ng-container matColumnDef="comment">
          <th mat-header-cell *matHeaderCellDef>Comment</th>
          <td mat-cell *matCellDef="let r" class="comment-cell">{{ r.comment }}</td>
        </ng-container>

        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef>Status</th>
          <td mat-cell *matCellDef="let r">
            <mat-chip-set>
              <mat-chip [color]="r.hidden ? 'warn' : 'primary'" [highlighted]="r.hidden">
                {{ r.hidden ? 'Hidden' : 'Visible' }}
              </mat-chip>
            </mat-chip-set>
          </td>
        </ng-container>

        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef>Actions</th>
          <td mat-cell *matCellDef="let r">
            <button mat-icon-button
                    [color]="r.hidden ? 'primary' : 'warn'"
                    (click)="toggleHidden(r)"
                    [matTooltip]="r.hidden ? 'Show review' : 'Hide review'">
              <mat-icon>{{ r.hidden ? 'visibility' : 'visibility_off' }}</mat-icon>
            </button>
          </td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
      </table>
    </div>
  `,
  styles: [`
    .review-moderation {
      padding: 16px 0;
    }
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    table {
      width: 100%;
    }
    .comment-cell {
      max-width: 300px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  `]
})
export class ReviewModerationComponent {
  reviews = signal<Review[]>([]);
  displayedColumns = ['id', 'frameworkId', 'username', 'rating', 'comment', 'status', 'actions'];

  constructor(
    private reviewService: ReviewService,
    private snackBar: MatSnackBar
  ) {
    this.loadReviews();
  }

  async loadReviews(): Promise<void> {
    try {
      const data = await this.reviewService.getAllReviewsForModeration();
      this.reviews.set(data);
    } catch (err: any) {
      this.snackBar.open('Failed to load reviews', 'Close', { duration: 3000 });
    }
  }

  async toggleHidden(review: Review): Promise<void> {
    const newHidden = !review.hidden;
    try {
      await this.reviewService.moderateReview(review.id, newHidden);
      this.snackBar.open(`Review ${newHidden ? 'hidden' : 'shown'}`, 'Close', { duration: 3000 });
      this.loadReviews();
    } catch (err: any) {
      this.snackBar.open('Failed to moderate review', 'Close', { duration: 3000 });
    }
  }
}
