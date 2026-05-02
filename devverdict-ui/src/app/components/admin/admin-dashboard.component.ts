import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { FrameworkManagementComponent } from './framework-management/framework-management.component';
import { ReviewModerationComponent } from './review-moderation/review-moderation.component';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatTabsModule,
    FrameworkManagementComponent,
    ReviewModerationComponent
  ],
  template: `
    <div class="max-w-6xl mx-auto px-4 py-8">
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900 tracking-tight">Admin Dashboard</h1>
        <p class="text-gray-500 mt-1">Manage frameworks and moderate reviews</p>
      </div>
      <mat-tab-group class="admin-tabs">
        <mat-tab label="Frameworks">
          <app-framework-management></app-framework-management>
        </mat-tab>
        <mat-tab label="Review Moderation">
          <app-review-moderation></app-review-moderation>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    ::ng-deep .admin-tabs .mat-mdc-tab-body-wrapper {
      padding-top: 24px;
    }
  `]
})
export class AdminDashboardComponent {}
