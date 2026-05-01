export interface Review {
  id: number;
  frameworkId: number;
  comment: string;
  rating: number;
  userId?: number;
  username?: string;
  hidden?: boolean;
  createdAt: string;
}

export interface CreateReviewRequest {
  frameworkId: number;
  comment: string;
  rating: number;
  userId?: number;
  username?: string;
}
