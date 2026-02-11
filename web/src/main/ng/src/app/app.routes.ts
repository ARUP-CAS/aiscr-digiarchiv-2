import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { ResultsComponent } from './pages/results/results.component';
import { StatsComponent } from './pages/stats/stats.component';
import { MapViewContainerComponent } from './pages/map-view/map-view-container.component';
import { DocumentComponent } from './pages/document/document.component';
import { ExportMapaComponent } from './pages/export-mapa/export-mapa.component';
import { ExportComponent } from './pages/export/export.component';

export const routes: Routes = [
    { path: '', redirectTo: 'home', pathMatch: 'full' },
    { path: 'home', component: Home },
    { path: 'results', component: ResultsComponent },
    { path: 'stats', component: StatsComponent },
    {
        path: 'map',
        loadComponent: () => import('./pages/map-view/map-view-container.component').then(m => m.MapViewContainerComponent)
        //    component: MapViewContainerComponent,  
    },
    {
        path: 'map/:id',
        loadComponent: () => import('./pages/map-view/map-view-container.component').then(m => m.MapViewContainerComponent)
    },
    {
        path: 'id/:id',
        loadComponent: () => import('./pages/document/document.component').then(m => m.DocumentComponent)
    },
    {
        path: 'print/:id',
        loadComponent: () => import('./pages/document/document.component').then(m => m.DocumentComponent)
    },
    // { path: 'id/:id', component: DocumentComponent },
    // { path: 'print/:id', component: DocumentComponent },
    { path: 'export', component: ExportComponent },
    { path: 'export-mapa', component: ExportMapaComponent },

];
