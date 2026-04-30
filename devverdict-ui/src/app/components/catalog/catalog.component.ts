import { Component, inject } from '@angular/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FrameworkService } from '../../services/framework.service';
import { FrameworkCardComponent } from '../framework-card/framework-card.component';

@Component({
  selector: 'app-catalog',
  imports: [MatProgressBarModule, MatSnackBarModule, FrameworkCardComponent],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss'
})
export class CatalogComponent {
  private readonly frameworkService = inject(FrameworkService);
  private readonly snackBar = inject(MatSnackBar);

  readonly frameworksResource = this.frameworkService.getAllFrameworks();

  showError(message: string): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: 5000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom'
    });
  }
}
