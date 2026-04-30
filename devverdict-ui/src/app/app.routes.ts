import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./components/catalog/catalog.component').then(m => m.CatalogComponent)
  },
  {
    path: 'framework/:id',
    loadComponent: () => import('./components/framework-detail/framework-detail.component').then(m => m.FrameworkDetailComponent)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
