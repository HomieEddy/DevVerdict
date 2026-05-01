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
    <div class="admin-container">
      <h1>Admin Dashboard</h1>
      <mat-tab-group>
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
    .admin-container {
      padding: 24px;
      max-width: 1200px;
      margin: 0 auto;
    }
    h1 {
      margin-bottom: 24px;
    }
  `]
})
export class AdminDashboardComponent {}
