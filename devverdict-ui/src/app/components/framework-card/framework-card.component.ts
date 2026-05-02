import { Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { Framework } from '../../models/framework.model';

@Component({
  selector: 'app-framework-card',
  imports: [RouterLink, DecimalPipe, MatCardModule, MatChipsModule, MatIconModule],
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

  getTypeGradient(type: string): string {
    switch (type.toLowerCase()) {
      case 'language': return 'from-blue-500 to-cyan-400';
      case 'framework': return 'from-purple-500 to-pink-400';
      case 'runtime': return 'from-orange-500 to-amber-400';
      case 'library': return 'from-emerald-500 to-teal-400';
      case 'tool': return 'from-indigo-500 to-violet-400';
      case 'database': return 'from-rose-500 to-red-400';
      default: return 'from-gray-500 to-slate-400';
    }
  }

  getTypeBgColor(type: string): string {
    switch (type.toLowerCase()) {
      case 'language': return 'bg-blue-50 text-blue-700';
      case 'framework': return 'bg-purple-50 text-purple-700';
      case 'runtime': return 'bg-orange-50 text-orange-700';
      case 'library': return 'bg-emerald-50 text-emerald-700';
      case 'tool': return 'bg-indigo-50 text-indigo-700';
      case 'database': return 'bg-rose-50 text-rose-700';
      default: return 'bg-gray-50 text-gray-700';
    }
  }
}
