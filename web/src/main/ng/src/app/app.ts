import { Component, DOCUMENT, Inject } from '@angular/core';
import { ActivatedRoute, IsActiveMatchOptions, NavigationEnd, Router, RouterModule, RouterOutlet } from '@angular/router';

import {MatSidenavModule} from '@angular/material/sidenav';
import {MatListModule} from '@angular/material/list';
import { AppConfiguration } from './app-configuration';
import { AppService } from './app.service';
import { AppState } from './app.state';
import { FooterComponent } from "./components/footer/footer.component";
import { SidenavListComponent } from "./components/sidenav-list/sidenav-list.component";
import { NavbarComponent } from "./components/navbar/navbar.component";
import { SearchbarComponent } from "./components/searchbar/searchbar.component";

@Component({
  selector: 'app-root',
  imports: [MatSidenavModule, MatListModule, RouterOutlet, FooterComponent, SidenavListComponent, NavbarComponent, SearchbarComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {

  
  isExport: boolean;
  isStats: boolean;
  isResults: boolean;

  constructor(
    private config: AppConfiguration,
    public state: AppState,
    private service: AppService,
    private route: ActivatedRoute,
    private router: Router,
    @Inject(DOCUMENT) private document: Document
  ) {}

  ngOnInit() {

    this.state.setConfig(this.config);
    this.service.changeLang(this.state.currentLang);
    
    const p: IsActiveMatchOptions = {
      paths: 'subset',
      fragment: 'ignored',
      matrixParams: 'ignored',
      queryParams: 'subset',
    };
    this.router.events.subscribe(val => {
      if (val instanceof NavigationEnd) {
        this.isExport = this.router.isActive('export', p) || this.router.isActive('export-mapa', p);
        this.isResults = this.router.isActive('results', p);
        this.isStats = this.router.isActive('stats', p);
        const params = this.route.snapshot.queryParamMap;
        this.state.isMapaCollapsed = (this.router.isActive('map', p) || this.router.isActive('export-mapa', p) || params.has('mapa')) ? params.get('mapa') === 'false' : true;
        if (params.has('lang')) {
          this.service.changeLang(params.get('lang'));
        }
        this.service.setCrumbs(params);
        this.state.processParams(params);

        if (!this.state.isMapaCollapsed) {
          this.document.body.classList.add('app-view-map');
        } else {
          this.document.body.classList.remove('app-view-map');
        }

        this.state.setRouteChanged(val.urlAfterRedirects);

      }
    });
  }
  
}
