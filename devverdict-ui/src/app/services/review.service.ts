import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, lastValueFrom } from 'rxjs';
import { resource } from '@angular/core';
import { Review, CreateReviewRequest, Page } from '../models/review.model';

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/reviews';

  getReviewsByFramework(frameworkId: number, page: number = 0, size: number = 10): Observable<Page<Review>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<Page<Review>>(`${this.apiUrl}/framework/${frameworkId}`, { params });
  }

  voteReview(reviewId: number, voteType: 'UPVOTE' | 'DOWNVOTE'): Observable<Review> {
    return this.http.post<Review>(`${this.apiUrl}/${reviewId}/vote`, { voteType });
  }

  getReviewsByUser(userId: number) {
    return resource({
      loader: () => lastValueFrom(this.http.get<Review[]>(`${this.apiUrl}/user/${userId}`))
    });
  }

  async createReview(request: CreateReviewRequest): Promise<Review> {
    return new Promise((resolve, reject) => {
      this.http.post<Review>(this.apiUrl, request).subscribe({
        next: resolve,
        error: reject
      });
    });
  }

  async updateReview(id: number, request: CreateReviewRequest): Promise<Review> {
    return new Promise((resolve, reject) => {
      this.http.put<Review>(`${this.apiUrl}/${id}`, request).subscribe({
        next: resolve,
        error: reject
      });
    });
  }

  async deleteReview(id: number): Promise<void> {
    return new Promise((resolve, reject) => {
      this.http.delete<void>(`${this.apiUrl}/${id}`).subscribe({
        next: () => resolve(),
        error: reject
      });
    });
  }

  async getAllReviewsForModeration(): Promise<Review[]> {
    return new Promise((resolve, reject) => {
      this.http.get<Review[]>(`${this.apiUrl}/moderation`).subscribe({
        next: resolve,
        error: reject
      });
    });
  }

  async moderateReview(id: number, hidden: boolean): Promise<Review> {
    return new Promise((resolve, reject) => {
      this.http.patch<Review>(`${this.apiUrl}/${id}/moderate`, { hidden }).subscribe({
        next: resolve,
        error: reject
      });
    });
  }
}
