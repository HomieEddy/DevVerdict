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

  async updateReview(id: number, request: CreateReviewRequest): Promise<Review> {
    return lastValueFrom(this.http.put<Review>(`${this.apiUrl}/${id}`, request));
  }

  async deleteReview(id: number): Promise<void> {
    return lastValueFrom(this.http.delete<void>(`${this.apiUrl}/${id}`));
  }

  getReviewsByUser(userId: number) {
    return resource({
      loader: () => lastValueFrom(this.http.get<Review[]>(`${this.apiUrl}/user/${userId}`))
    });
  }

  async getAllReviewsForModeration(): Promise<Review[]> {
    return lastValueFrom(this.http.get<Review[]>(`${this.apiUrl}/moderation`));
  }

  async moderateReview(id: number, hidden: boolean): Promise<Review> {
    return lastValueFrom(this.http.patch<Review>(`${this.apiUrl}/${id}/moderate`, { hidden }));
  }
}
