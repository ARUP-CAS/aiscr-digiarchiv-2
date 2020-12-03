
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { HomeComponent } from 'src/app/pages/home/home.component';
import { DocumentComponent } from 'src/app/pages/document/document.component';
import { ExportComponent } from 'src/app/pages/export/export.component';
import { ResultsComponent } from 'src/app/pages/results/results.component';
import { NapovedaComponent } from 'src/app/pages/napoveda/napoveda.component';
import { FavoritesComponent } from './pages/favorites/favorites.component';


const routes: Routes = [
  {path: 'home', component: HomeComponent},
  {path: 'id/:id', component: DocumentComponent},
  {path: 'print/:id', component: DocumentComponent},
  {path: 'export', component: ExportComponent},
  {path: 'results', component: ResultsComponent},
  {path: 'favorites', component: FavoritesComponent},
  {path: 'napoveda', component: NapovedaComponent},
  {path: '', redirectTo: 'home', pathMatch: 'full'},
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    initialNavigation: 'enabled'
})],
  exports: [RouterModule]
})
export class AppRoutingModule { }
