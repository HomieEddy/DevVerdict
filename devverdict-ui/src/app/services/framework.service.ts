import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { resource } from '@angular/core';
import { Framework } from '../models/framework.model';

@Injectable({
  providedIn: 'root'
})
export class FrameworkService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/catalog/frameworks';

  getAllFrameworks() {
    return resource({
      loader: () => this.http.get<Framework[]>(this.apiUrl)
    });
  }

  getFrameworkById(id: number) {
    return resource({
      loader: () => this.http.get<Framework>(`${this.apiUrl}/${id}`)
    });
  }
}
