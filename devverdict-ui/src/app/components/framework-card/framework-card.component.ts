import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { Framework } from '../../models/framework.model';

@Component({
  selector: 'app-framework-card',
  imports: [RouterLink, MatCardModule, MatChipsModule, MatIconModule],
  templateUrl: './framework-card.component.html',
  styleUrl: './framework-card.component.scss'
})
export class FrameworkCardComponent {
  readonly framework = input.required<Framework>();

  getTypeColor(type: string): string {
    switch (type.toLowerCase()) {
      case 'language': return 'primary';
      case 'framework': return 'accent';
      case 'runtime': return 'warn';
      default: return '';
    }
  }
}
