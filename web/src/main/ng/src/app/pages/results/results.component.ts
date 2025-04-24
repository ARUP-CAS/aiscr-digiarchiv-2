import { SolrDocument } from './../../shared/solr-document';
import { HttpParams } from '@angular/common/http';
import { SolrResponse } from './../../shared/solr-response';
import { Component, OnInit, OnDestroy, ViewChild, ChangeDetectorRef } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { Title } from '@angular/platform-browser';
import { Router, NavigationEnd, ActivatedRoute, ParamMap, Params } from '@angular/router';
import { trigger, transition, style, animate } from '@angular/animations';
import { MediaMatcher } from '@angular/cdk/layout';
import { NgZone } from '@angular/core';

@Component({
  selector: 'app-results', 
  animations: [
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

  @ViewChild('leftElement') leftElement: any;

  opened = false;
  matcher: MediaQueryList;
  sideNavMapBreakPoint: string = "(min-width: 1280px)";
  loading = false;
  docs: SolrDocument[] = [];
  isChartBarCollapsed = true;
  exportUrl: string;
  inFav: boolean;
  hasResultsInOther: boolean;
  math = Math;
  subs: any[] = [];

  itemSize = 70;
  vsSize = 0;

  constructor(
    private titleService: Title,
    private route: ActivatedRoute,
    private router: Router,
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService,
    public mediaMatcher: MediaMatcher,
    private zone: NgZone,
    private cd: ChangeDetectorRef
  ) {
    this.state.bodyClass = 'app-page-results';
  }

  ngOnInit(): void {
    this.setTitle();
    this.state.hasError = false;
    this.subs.push(this.service.currentLang.subscribe(res => {
      this.setTitle();
      const parts = this.router.url.split('?');
      const str = (parts.length > 1 ? parts[1] : '') + '&lang=' + this.state.currentLang;
      this.exportUrl = 'export?' + str;
    }));

    this.subs.push(this.state.mapResultChanged.subscribe(res => {
      if (window.innerWidth < this.config.hideMenuWidth) {
        this.opened = false;
      }
    }));
    this.subs.push(this.route.queryParams.subscribe(val => {
      this.search(val);
      const parts = this.router.url.split('?');
      const str = (parts.length > 1 ? parts[1] : '') + '&lang=' + this.state.currentLang;
      this.exportUrl = 'export?' + str;
    }));

    this.subs.push(this.state.resultsChanged.subscribe(val => {
      if (val.typ === 'map') {
        this.docs = this.state.solrResponse.response.docs;
        setTimeout(() => {
          this.vsSize = this.leftElement.nativeElement.clientHeight - 107;
        }, 100);
      }
    }));

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
    
    this.subs.forEach(s => s.unsubscribe());
  }

  // set opened for sidenav
  myListener(event: any) {
    this.zone.run(() => {
      this.opened = event.matches;
    });
  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('navbar.desc.logo_desc') 
    + ' | ' + (this.state.isMapaCollapsed ? this.service.getTranslation('title.results') : this.service.getTranslation('title.map'))
    + ' - ' + this.service.getTranslation('entities.'+ this.state.entity+'.title') );
  }

  toggleFavorites() {
    this.inFav = !this.inFav;
    const params: Params = this.route.snapshot.queryParams;
    const p: any = Object.assign({}, params);
    if (this.inFav) {
      p.inFavorites = this.inFav;
    } else {
      p.inFavorites = null;
    }
    this.router.navigate([], { queryParams: p, queryParamsHandling: 'merge' });
  }

  search(params: Params) {
    if (params.mapa) {
      // Zpracuje mapa
      // setTimeout(() => {
      //   this.vsSize = this.leftElement.nativeElement.clientHeight - 107;
      // }, 100);
      return;
    }
    this.state.loading = true;
    const p = Object.assign({}, params);
    this.state.switchingMap = false;
    this.state.documentProgress = 0;
    this.loading = true;
    this.state.facetsLoading = true;
    this.state.hasError = false;
    
    if (!p['entity']) {
      p['entity'] = 'dokument';
    }
    // p.mapa = !this.state.isMapaCollapsed;
    this.docs = [];
    p['noFacets'] = 'true';
    this.service.search(p as HttpParams).subscribe((resp: SolrResponse) => {
      if (resp.error) {
        this.state.loading = false;
        this.state.facetsLoading = false;
        this.loading = false;
        this.state.hasError = true;
        this.service.showErrorDialog('dialog.alert.error', 'dialog.alert.search_error');
        return;
      }
      this.state.setSearchResponse(resp);

      if (p.rows && this.state.ui?.[this.state.entity]) {
        this.state.ui[this.state.entity].rows = p.rows;
      }

      if (this.state.ui?.[this.state.entity]?.sort) {
        this.state.sort = this.state.sorts_by_entity.find(s => (s.field) === this.state.ui[this.state.entity].sort);
      }

      this.setTitle();
      this.docs = resp.response.docs;
      if (this.state.isMapaCollapsed) {
        this.state.loading = false;
      }
      this.loading = false;
      this.hasResultsInOther = this.config.entities.findIndex(e => this.state.totals[e] > 0) > -1;
      setTimeout(() => {
        this.vsSize = this.leftElement.nativeElement.clientHeight - 107;
      }, 100);

      p['noFacets'] = 'false';
      p['onlyFacets'] = 'true';
      this.service.search(p as HttpParams).subscribe((resp: SolrResponse) => {
        this.state.setFacets(resp);
        this.state.facetsLoading = false;
      });
      
      // Math.min(9*itemSize, docs.length * itemSize)
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

  setFacetsOpened() {
    this.state.isFacetsCollapsed =! this.state.isFacetsCollapsed;
  }

  loadingFinished() {
    setTimeout(() => {
      this.state.loading = false;
      this.cd.detectChanges();
    }, 100)
    
  }
}
