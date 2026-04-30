import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { resource } from '@angular/core';
import { lastValueFrom } from 'rxjs';
import { Review, CreateReviewRequest } from '../models/review.model';

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/reviews';

  getReviewsByFramework(frameworkId: number) {
    return resource({
      loader: () => lastValueFrom(this.http.get<Review[]>(`${this.apiUrl}/framework/${frameworkId}`))
    });
  }

  async createReview(request: CreateReviewRequest): Promise<Review> {
    return lastValueFrom(this.http.post<Review>(this.apiUrl, request));
  }
}
