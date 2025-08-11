import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { ResultsComponent } from './pages/results/results.component';
import { StatsComponent } from './pages/stats/stats.component';
import { MapViewContainerComponent } from './pages/map-view/map-view-container.component';

export const routes: Routes = [
    { path: '', redirectTo: 'home', pathMatch: 'full' },
    { path: 'home', component: Home },
    { path: 'results', component: ResultsComponent },
    { path: 'stats', component: StatsComponent },
    { path: 'map', 
        loadComponent: () => import('./pages/map-view/map-view-container.component').then(m => m.MapViewContainerComponent)
    //    component: MapViewContainerComponent,  
    }

];
