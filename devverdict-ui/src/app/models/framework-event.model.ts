export interface FrameworkEvent {
  type: 'REVIEW_CREATED';
  frameworkId: number;
  newAverage: number;
  newCount: number;
}
