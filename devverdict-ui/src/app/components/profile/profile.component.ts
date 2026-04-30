import { Component, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../services/auth.service';
import { ReviewService } from '../../services/review.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule
  ],
  templateUrl: './profile.component.html'
})
export class ProfileComponent {
  user = computed(() => this.authService.currentUser());
  reviewsResource = computed(() => {
    const userId = this.authService.currentUser()?.id;
    return userId ? this.reviewService.getReviewsByUser(userId) : null;
  });

  constructor(
    private authService: AuthService,
    private reviewService: ReviewService
  ) {}
}
