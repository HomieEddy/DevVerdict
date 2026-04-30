import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FrameworkService } from '../../services/framework.service';

@Component({
  selector: 'app-framework-detail',
  imports: [
    RouterLink,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatSnackBarModule
  ],
  templateUrl: './framework-detail.component.html',
  styleUrl: './framework-detail.component.scss'
})
export class FrameworkDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly frameworkService = inject(FrameworkService);
  private readonly snackBar = inject(MatSnackBar);

  readonly frameworkId = Number(this.route.snapshot.paramMap.get('id'));
  readonly frameworkResource = this.frameworkService.getFrameworkById(this.frameworkId);

  getTypeColor(type: string): string {
    switch (type.toLowerCase()) {
      case 'language': return 'primary';
      case 'framework': return 'accent';
      case 'runtime': return 'warn';
      default: return '';
    }
  }

  showError(message: string): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: 5000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom'
    });
  }
}
