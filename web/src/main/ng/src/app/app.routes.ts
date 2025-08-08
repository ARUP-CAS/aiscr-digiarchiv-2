import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { ResultsComponent } from './pages/results/results.component';
import { StatsComponent } from './pages/stats/stats.component';

export const routes: Routes = [
    { path: '', redirectTo: 'home', pathMatch: 'full' },
    { path: 'home', component: Home },
    { path: 'results', component: ResultsComponent },
    { path: 'stats', component: StatsComponent }
];
