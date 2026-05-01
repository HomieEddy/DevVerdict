import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { Framework } from '../../../models/framework.model';
import { CreateFrameworkRequest } from '../../../services/framework.service';

@Component({
  selector: 'app-framework-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule
  ],
  template: `
    <h2 mat-dialog-title>{{ data ? 'Edit' : 'Add' }} Framework</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Name</mat-label>
        <input mat-input [(ngModel)]="model.name" name="name" required />
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Type</mat-label>
        <mat-select [(ngModel)]="model.type" name="type" required>
          <mat-option value="Framework">Framework</mat-option>
          <mat-option value="Library">Library</mat-option>
          <mat-option value="Language">Language</mat-option>
          <mat-option value="Tool">Tool</mat-option>
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Description</mat-label>
        <textarea matInput [(ngModel)]="model.description" name="description" rows="4" required></textarea>
      </mat-form-field>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Average Rating</mat-label>
        <input matInput type="number" [(ngModel)]="model.averageRating" name="averageRating" min="0" max="5" step="0.1" required />
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" (click)="onSave()" [disabled]="!isValid()">Save</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width {
      width: 100%;
      margin-bottom: 12px;
    }
    mat-dialog-content {
      display: flex;
      flex-direction: column;
      min-width: 400px;
    }
  `]
})
export class FrameworkDialogComponent {
  model: CreateFrameworkRequest = {
    name: '',
    type: 'Framework',
    description: '',
    averageRating: 0
  };

  constructor(
    public dialogRef: MatDialogRef<FrameworkDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: Framework | null
  ) {
    if (data) {
      this.model = {
        name: data.name,
        type: data.type,
        description: data.description,
        averageRating: data.averageRating
      };
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    this.dialogRef.close(this.model);
  }

  isValid(): boolean {
    return !!this.model.name && !!this.model.type && !!this.model.description && this.model.averageRating >= 0;
  }
}
