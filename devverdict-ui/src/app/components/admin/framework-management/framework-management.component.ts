import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FrameworkService, CreateFrameworkRequest } from '../../../services/framework.service';
import { Framework } from '../../../models/framework.model';
import { FrameworkDialogComponent } from './framework-dialog.component';

@Component({
  selector: 'app-framework-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  template: `
    <div class="framework-management">
      <div class="header">
        <h2>Frameworks</h2>
        <button mat-raised-button color="primary" (click)="openAddDialog()">
          <mat-icon>add</mat-icon> Add Framework
        </button>
      </div>

      <table mat-table [dataSource]="frameworks()" class="mat-elevation-z2">
        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef>Name</th>
          <td mat-cell *matCellDef="let f">{{ f.name }}</td>
        </ng-container>

        <ng-container matColumnDef="type">
          <th mat-header-cell *matHeaderCellDef>Type</th>
          <td mat-cell *matCellDef="let f">{{ f.type }}</td>
        </ng-container>

        <ng-container matColumnDef="rating">
          <th mat-header-cell *matHeaderCellDef>Rating</th>
          <td mat-cell *matCellDef="let f">{{ f.averageRating | number:'1.1-1' }}</td>
        </ng-container>

        <ng-container matColumnDef="reviews">
          <th mat-header-cell *matHeaderCellDef>Reviews</th>
          <td mat-cell *matCellDef="let f">{{ f.reviewCount }}</td>
        </ng-container>

        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef>Actions</th>
          <td mat-cell *matCellDef="let f">
            <button mat-icon-button color="primary" (click)="openEditDialog(f)" matTooltip="Edit">
              <mat-icon>edit</mat-icon>
            </button>
            <button mat-icon-button color="warn" (click)="deleteFramework(f)" [disabled]="f.reviewCount > 0" matTooltip="Delete">
              <mat-icon>delete</mat-icon>
            </button>
          </td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
      </table>
    </div>
  `,
  styles: [`
    .framework-management {
      padding: 16px 0;
    }
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    table {
      width: 100%;
    }
  `]
})
export class FrameworkManagementComponent {
  frameworks = signal<Framework[]>([]);
  displayedColumns = ['name', 'type', 'rating', 'reviews', 'actions'];

  constructor(
    private frameworkService: FrameworkService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {
    this.loadFrameworks();
  }

  async loadFrameworks(): Promise<void> {
    try {
      const data = await this.frameworkService.fetchAllFrameworks();
      this.frameworks.set(data);
    } catch {
      this.snackBar.open('Failed to load frameworks', 'Close', { duration: 3000 });
    }
  }

  openAddDialog(): void {
    const dialogRef = this.dialog.open(FrameworkDialogComponent, {
      width: '500px',
      data: null
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result) this.createFramework(result);
    });
  }

  openEditDialog(framework: Framework): void {
    const dialogRef = this.dialog.open(FrameworkDialogComponent, {
      width: '500px',
      data: framework
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result) this.updateFramework(framework.id, result);
    });
  }

  async createFramework(request: CreateFrameworkRequest): Promise<void> {
    try {
      await this.frameworkService.createFramework(request);
      this.snackBar.open('Framework created', 'Close', { duration: 3000 });
      this.loadFrameworks();
    } catch (err: any) {
      this.snackBar.open(err.error || 'Failed to create framework', 'Close', { duration: 3000 });
    }
  }

  async updateFramework(id: number, request: CreateFrameworkRequest): Promise<void> {
    try {
      await this.frameworkService.updateFramework(id, request);
      this.snackBar.open('Framework updated', 'Close', { duration: 3000 });
      this.loadFrameworks();
    } catch (err: any) {
      this.snackBar.open(err.error || 'Failed to update framework', 'Close', { duration: 3000 });
    }
  }

  async deleteFramework(framework: Framework): Promise<void> {
    if (framework.reviewCount > 0) {
      this.snackBar.open('Cannot delete framework with reviews', 'Close', { duration: 3000 });
      return;
    }
    if (!confirm(`Delete "${framework.name}"?`)) return;

    try {
      await this.frameworkService.deleteFramework(framework.id);
      this.snackBar.open('Framework deleted', 'Close', { duration: 3000 });
      this.loadFrameworks();
    } catch (err: any) {
      this.snackBar.open(err.error || 'Failed to delete framework', 'Close', { duration: 3000 });
    }
  }
}
