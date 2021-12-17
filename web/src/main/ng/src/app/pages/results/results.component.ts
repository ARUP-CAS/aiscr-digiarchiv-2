import { SolrDocument } from './../../shared/solr-document';
import { HttpParams } from '@angular/common/http';
import { SolrResponse } from './../../shared/solr-response';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { Title } from '@angular/platform-browser';
import { Router, NavigationEnd, ActivatedRoute, ParamMap, Params } from '@angular/router';
import { trigger, transition, style, animate } from '@angular/animations';
import { MediaMatcher } from '@angular/cdk/layout';
import { NgZone } from '@angular/core';

@Component({
  selector: 'app-results', animations: [
    trigger(
      'enterAnimation', [
      transition(':enter', [
        style({ transform: 'translateY(100%)', height: 0 }),
        animate('100ms', style({ transform: 'translateY(0)', height: 120 }))
      ]),
      transition(':leave', [
        style({ transform: 'translateY(0)', height: 120 }),
        animate('100ms', style({ transform: 'translateY(100%)', height: 0 }))
      ])
    ]
    )
  ],
  templateUrl: './results.component.html',
  styleUrls: ['./results.component.scss']
})
export class ResultsComponent implements OnInit, OnDestroy {
  opened = true;
  matcher: MediaQueryList;
  sideNavMapBreakPoint: string = "(min-width: 1280px)";
  loading = false;
  docs: SolrDocument[] = [];
  isChartBarCollapsed = true;
  exportUrl: string;
  inFav: boolean;
  hasResultsInOther: boolean;
  math = Math;

  constructor(
    private titleService: Title,
    private route: ActivatedRoute,
    private router: Router,
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService,
    public mediaMatcher: MediaMatcher,
    private zone: NgZone,
  ) {
    this.state.bodyClass = 'app-page-results';
  }

  ngOnInit(): void {
    this.service.currentLang.subscribe(res => {
      this.setTitle();
      const parts = this.router.url.split('?');
      const str = (parts.length > 1 ? parts[1] : '') + '&lang=' + this.state.currentLang;
      this.exportUrl = 'export?' + str;
    });
    this.route.queryParams.subscribe(val => {
      this.search(val);
      const parts = this.router.url.split('?');
      const str = (parts.length > 1 ? parts[1] : '') + '&lang=' + this.state.currentLang;
      this.exportUrl = 'export?' + str;
    });

    // this.state.loggedChanged.subscribe(val => {
    //   this.search(this.route.snapshot.queryParams);
    // });


    // set side nav map breakpoint
    this.matcher = this.mediaMatcher.matchMedia(this.sideNavMapBreakPoint);
    this.matcher.addEventListener('change', (e) => {
      this.myListener(e);
    });
  }

  ngOnDestroy() {
    this.matcher.removeEventListener('change', (e) => {
      this.myListener(e);
    });
  }

  // set opened for sidenav
  myListener(event) {
    this.zone.run(() => {
      this.opened = event.matches;
    });
  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('logo_desc') + ' | Results');
  }

  toggleFavorites() {
    this.inFav = !this.inFav;
    const params: Params = this.route.snapshot.queryParams;
    const p = Object.assign({}, params);
    if (this.inFav) {
      p.inFavorites = this.inFav;
    } else {
      p.inFavorites = null;
    }
    this.router.navigate([], { queryParams: p, queryParamsHandling: 'merge' });
  }

  search(params: Params) {
    this.state.loading = true;
    this.loading = true;
    const p = Object.assign({}, params);
    // p.mapa = !this.state.isMapaCollapsed;
    this.service.search(p as HttpParams).subscribe((resp: SolrResponse) => {
      this.state.setSearchResponse(resp);
      this.docs = resp.response.docs;
      if (this.state.isMapaCollapsed) {
        this.state.loading = false;
      }
      this.loading = false;
      this.hasResultsInOther = this.config.entities.findIndex(e => this.state.totals[e] > 0) > -1;
    });

  }


  isActiveFacet() {
    if (this.state.breadcrumbs?.length === 0) {
      return false;
    } else {
      return true;
    }
  }

  showChartBar() {
    this.isChartBarCollapsed = !this.isChartBarCollapsed;
  }

  setItemView(view: string) {
    this.state.itemView = view;
  }

  setEntity(entity) {
    this.router.navigate([], { queryParams: { entity, page: 0 }, queryParamsHandling: 'merge' });
  }
}
