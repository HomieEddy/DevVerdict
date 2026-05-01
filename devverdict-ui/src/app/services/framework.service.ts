import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { resource } from '@angular/core';
import { lastValueFrom } from 'rxjs';
import { Framework } from '../models/framework.model';

export interface CreateFrameworkRequest {
  name: string;
  type: string;
  description: string;
  averageRating: number;
}

@Injectable({
  providedIn: 'root'
})
export class FrameworkService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/catalog/frameworks';

  getAllFrameworks() {
    return resource({
      loader: () => lastValueFrom(this.http.get<Framework[]>(this.apiUrl))
    });
  }

  getFrameworkById(id: number) {
    return resource({
      loader: () => lastValueFrom(this.http.get<Framework>(`${this.apiUrl}/${id}`))
    });
  }

  async createFramework(request: CreateFrameworkRequest): Promise<Framework> {
    return lastValueFrom(this.http.post<Framework>(this.apiUrl, request));
  }

  async updateFramework(id: number, request: CreateFrameworkRequest): Promise<Framework> {
    return lastValueFrom(this.http.put<Framework>(`${this.apiUrl}/${id}`, request));
  }

  async deleteFramework(id: number): Promise<void> {
    return lastValueFrom(this.http.delete<void>(`${this.apiUrl}/${id}`));
  }
}
