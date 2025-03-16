import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';

import { Component, OnInit, Inject } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { ActivatedRoute, Router, NavigationEnd, ParamMap } from '@angular/router';
import { Subject } from 'rxjs/internal/Subject';
import { DOCUMENT } from '@angular/common';


@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {

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
  ) {
  }

  ngOnInit() {

    this.state.setConfig(this.config);
    this.service.changeLang(this.state.currentLang);
    
    this.router.events.subscribe(val => {
      if (val instanceof NavigationEnd) {
        this.isExport = this.router.isActive('export', false) || this.router.isActive('export-mapa', false);
        this.isResults = this.router.isActive('results', false);
        this.isStats = this.router.isActive('stats', false);
        const params = this.route.snapshot.queryParamMap;
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
