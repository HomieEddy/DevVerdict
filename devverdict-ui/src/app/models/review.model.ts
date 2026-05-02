export interface Review {
  id: number;
  frameworkId: number;
  comment: string;
  rating: number;
  userId?: number;
  username?: string;
  hidden?: boolean;
  createdAt: string;
  pros?: string;
  cons?: string;
  helpfulVotes: number;
  notHelpfulVotes: number;
}

export interface CreateReviewRequest {
  frameworkId: number;
  comment: string;
  rating: number;
  userId?: number;
  username?: string;
  pros?: string;
  cons?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  numberOfElements: number;
}
