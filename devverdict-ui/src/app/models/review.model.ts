export interface Review {
  id: number;
  frameworkId: number;
  comment: string;
  rating: number;
  createdAt: string;
}

export interface CreateReviewRequest {
  frameworkId: number;
  comment: string;
  rating: number;
}
