
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { HomeComponent } from 'src/app/pages/home/home.component';
import { DocumentComponent } from 'src/app/pages/document/document.component';
import { ExportComponent } from 'src/app/pages/export/export.component';
import { ResultsComponent } from 'src/app/pages/results/results.component';
import { NapovedaComponent } from 'src/app/pages/napoveda/napoveda.component';
import { ExportMapaComponent } from './pages/export-mapa/export-mapa.component';
import { StatsComponent } from './pages/stats/stats.component';
import { MapViewContainerComponent } from './pages/map-view/map-view-container.component';


const routes: Routes = [
  {path: 'home', component: HomeComponent},
  {path: 'id/:id', component: DocumentComponent},
  {path: 'print/:id', component: DocumentComponent},
  {path: 'export', component: ExportComponent},
  {path: 'export-mapa', component: ExportMapaComponent},
  {path: 'results', component: ResultsComponent},
  {path: 'map', component: MapViewContainerComponent},
  {path: 'napoveda', component: NapovedaComponent},
  {path: 'stats', component: StatsComponent},
  {path: '', redirectTo: 'home', pathMatch: 'full'},
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    initialNavigation: 'enabledBlocking'
})],
  exports: [RouterModule]
})
export class AppRoutingModule { }
