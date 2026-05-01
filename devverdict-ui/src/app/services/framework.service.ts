import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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

  fetchAllFrameworks(): Promise<Framework[]> {
    return lastValueFrom(this.http.get<Framework[]>(this.apiUrl));
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

  searchFrameworks(name?: string, type?: string, minRating?: number | null) {
    let params = new HttpParams();
    if (name) params = params.set('name', name);
    if (type) params = params.set('type', type);
    if (minRating !== undefined && minRating !== null) params = params.set('minRating', minRating.toString());

    return resource({
      loader: () => lastValueFrom(this.http.get<Framework[]>(`${this.apiUrl}/search`, { params }))
    });
  }

  fetchSearchResults(name?: string, type?: string, minRating?: number | null): Promise<Framework[]> {
    let params = new HttpParams();
    if (name) params = params.set('name', name);
    if (type) params = params.set('type', type);
    if (minRating !== undefined && minRating !== null) params = params.set('minRating', minRating.toString());

    return lastValueFrom(this.http.get<Framework[]>(`${this.apiUrl}/search`, { params }));
  }
}
