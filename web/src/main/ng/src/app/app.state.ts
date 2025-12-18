

import { Observable, Subject, ReplaySubject } from 'rxjs';
import { Params, ParamMap } from '@angular/router';
import { Sort } from './shared/config';
import { MatDialogRef } from '@angular/material/dialog';
import { AppConfiguration } from './app-configuration';
import { Condition } from './shared/condition';
import { Crumb } from './shared/crumb';
import { SolrResponse } from './shared/solr-response';
import { User } from './shared/user';
import { signal } from '@angular/core';

export class AppState {

  // Observe state
  private resultsSubject: Subject<{typ: string, pageChanged: boolean}> = new Subject();
  public resultsChanged: Observable<{typ: string, pageChanged: boolean}> = this.resultsSubject.asObservable();

  private routeSubject: ReplaySubject<Params> = new ReplaySubject(0);
  public routeChanged: Observable<Params> = this.routeSubject.asObservable();

  private facetsSubject: Subject<string> = new Subject();
  public facetsChanged: Observable<string> = this.facetsSubject.asObservable();

  private loggedSubject: Subject<boolean> = new Subject();
  public loggedChanged: Observable<boolean> = this.loggedSubject.asObservable();

  private mapResultSubject: Subject<string> = new ReplaySubject(1);
  public mapResultChanged: Observable<string> = this.mapResultSubject.asObservable();

  private mapViewSubject: Subject<string> = new Subject();
  public mapViewChanged: Observable<string> = this.mapViewSubject.asObservable();
  public sidenavOpened: boolean;

  entity: string;

  public user: User;
  public logged = false;

  config: AppConfiguration;

  bodyClass = 'app-page-home';
  currentLang = 'cs';
  itemView = 'default';
  isMapaCollapsed = true;
  timelineOpened = true;
  documentId: string;
  isFacetsCollapsed = true;

  dialogRef: MatDialogRef<any, any>;
  mapResult = signal<any>(null); // Select entity in map view
  pianId: string; // Selected pian in map
  locationFilterEnabled: boolean; // Vyber na mape
  locationFilterBounds: any; // Vyber na mape
  mapBounds: any;

  solrResponse: SolrResponse;
  loading = signal<boolean>(true);
  facetsLoading = signal<boolean>(true);
  printing = signal<boolean>(false);
  switchingMap = false;
  closingMapResult = false;
  hasError = false;
  imagesLoading: boolean;
  imagesLoaded = 0;
  numFound: number;
  numChildren = 0;
  numImages = 0;
  facets: { field: string, values: { name: string, type: string, value: number, operator: string }[] }[] = [];
  facetsFiltered: { field: string, values: { name: string, type: string, value: number, operator: string }[] }[];
  facetFilterValue: string;
  areFacetsFiltered: boolean;
  stats: { [field: string]: { min: any, max: any, count: number, from: any, until: any } };

  totals: { [entity: string]: number } = {};

  facetRanges: unknown;
  currentObdobiOd: number;
  currentObdobiDo: number;

  facetPivots: {
    field: string,
    count: number,
    values: {
      visible: boolean;
      field: string;
      count: number;
      value: string;
      pivot: { field: string, count: number, value: string }[]
    }[]
  }[] = [];

  heatMaps:any;

  q: string;
  rows: number;
  page = 0;
  pageChanged = false;
  totalPages: number
  sorts_by_entity: Sort[];
  sort: Sort;
  ui: { sort: {[entity: string]:string}, rows?:number} = {sort:{}};

  // Pokud uzivatel zvoli jine razeni pro danou facetu, napr: {"obdobi": "poradi"}
  facetSort: {[facetname: string]: string} = {};

  hideWithoutThumbs = false;
  inFavorites = signal<boolean>(false);
  obdobi: any;

  breadcrumbs: Crumb[];
  conditions: Condition[];

  documentProgress: number;

  setConfig(cfg: AppConfiguration) {
    this.config = cfg;
    this.currentLang = cfg.defaultLang;
    this.rows = cfg.defaultRows;
    this.sort = cfg.sorts[0];
  }

  setEntityTotals(facet: { name: string, type: string, value: number }[]) {
    facet.forEach(f => {
      this.totals[f.name] = f.value;
    });
  }

  setSearchResponse(resp: SolrResponse, typ: string = 'results') {
    this.solrResponse = resp;
    this.numFound = resp.response.numFound;
    this.totalPages = this.numFound / this.rows;
    this.stats = resp.stats.stats_fields;
    const keys = Object.keys(this.stats);
    keys.forEach(k => {
      this.stats[k].from = this.stats[k].min;
      this.stats[k].until = this.stats[k].max;
    });
    this.stats['datum_provedeni'] = {
      min: this.stats['datum_provedeni_od'].min,
      max: this.stats['datum_provedeni_do'].max,
      count: this.stats['datum_provedeni_od'].count,
      from: this.stats['datum_provedeni_od'].min,
      until: this.stats['datum_provedeni_do'].max
    };

    if (resp.facet_counts) {
      this.setEntityTotals(resp.facet_counts.facet_fields['entity']);
    }
    

    this.resultsSubject.next({typ, pageChanged: this.pageChanged});
    this.pageChanged = false;
    if (resp.facet_counts) {
      setTimeout(() => {
        this.setFacets(resp);
      }, 100);
    }
  }

  setFacets(resp: any) {
    if (this.page !== 0 && this.heatMaps?.loc_rpt) {
      return;
    }
    this.facets = [];
    this.heatMaps = resp.facet_counts?.facet_heatmaps;
    this.facetsFiltered = [];
    this.areFacetsFiltered = false;
    this.facetFilterValue = '';
    if (!resp.facet_counts) {
      return;
    }
    this.config.facets.forEach(f => {
      if (resp.facet_counts.facet_fields[f]) {
        const ff: { name: string, type: string, value: number, operator: string }[] = resp.facet_counts.facet_fields[f];
        ff.forEach(v => {
          v.operator = this.breadcrumbs.find(c => c.field === f && c.value === v.name)?.operator;
          // v.poradi = 
        });
        this.facets.push({ field: f, values: ff });
      }
    });
    const fields = Object.keys(resp.facet_counts.facet_fields);
    fields.forEach(f => {
      if (!this.config.facets.includes(f)) {
        const ff: { name: string, type: string, value: number, operator: string }[] = resp.facet_counts.facet_fields[f];
        if (f === 'entity') {
          this.setEntityTotals(ff);
        } else {
          ff.forEach(v => {
            v.operator = this.breadcrumbs.find(c => c.field === f && c.value === v.name)?.operator;
          });
          this.facets.push({ field: f, values: ff });
        }
      }
    });

    

    this.facetsFiltered = Object.assign([], this.facets);
    this.setFacetPivots(resp);
    this.facetRanges = resp.facet_counts.facet_ranges;

    this.facetsSubject.next('facets');
  }

  setFacetChanged() {
    this.stats = null;
    this.facetsSubject.next('direct');
  }

  setFacetPivots(resp: SolrResponse) {

    this.facetPivots = [];
    if (resp.facet_counts.facet_pivot) {
      const pivots = resp.facet_counts.facet_pivot;
      const keys = Object.keys(pivots);

      if (keys.length === 0) {
        return;
      }

      keys.forEach(f => {
        const field = f.split(',')[0];
        switch (field) {
          case 'dokument_kategorie_dokumentu':
            const fp: any = { field: field, values: [], count: -1 };
            pivots[f].forEach(f1 => {
              fp.values.push(f1);
            });
            this.facetPivots.push(fp);
            break;
          default:
            const facetPivoted: any = {};
            pivots[f].forEach(f1 => {
              if (this.getUserPristupnost().localeCompare(f1.value) > -1) {
                f1.pivot.forEach(f2 => {
                  if (facetPivoted[f2.value]) {
                    facetPivoted[f2.value].value += f2.count;
                  } else {
                    facetPivoted[f2.value] = { name: f2.value, type: 'int', value: f2.count };
                  }
                });
              }
            });
            const fpKeys = Object.keys(facetPivoted);
            const values: any = [];
            fpKeys.forEach(k => {
              values.push(facetPivoted[k]);
            });
            this.facetsFiltered.push({ field, values });

        }
      });
    }

  }

  setRouteChanged(val: any) {
    this.routeSubject.next(val);
  }

  processParams(params: ParamMap) {
    this.hideWithoutThumbs = params.has('hideWithoutThumbs') ? params.get('hideWithoutThumbs') === 'true' : false;
    this.inFavorites.set(params.has('inFavorites') ? params.get('inFavorites') === 'true' : false);
    console.log(this.inFavorites())
    this.entity = params.has('entity') ? params.get('entity') : 'dokument';
    this.sorts_by_entity = this.config.sorts.filter(s => !s.entity || s.entity.includes(this.entity));
    this.page = params.has('page') ? +params.get('page') : 0;
    
    if (this.isMapaCollapsed) {
      this.rows = params.has('rows') ? +params.get('rows') : this.config.defaultRows;
    } else {
      this.rows = this.config.mapOptions.docsForMarker;
    }
    this.pianId = params.has('pian_id') ? params.get('pian_id') : null;

    // this.sort = null;
    if (params.has('sort')) {
      this.sort = this.sorts_by_entity.find(s => (s.field) === params.get('sort'));
    } else if(this.sort) {
      // this.sort could be from another entity. Check validity
      this.sort = this.sorts_by_entity.find(s => s.field === this.sort.field);
    }
    if (!this.sort) {
      this.sort = this.sorts_by_entity[0];
    }

  }

  getUserPristupnost() {
    if (this.user) {
      return this.user.pristupnost;
    } else {
      return 'A';
    }
  }

  hasRights(pristupnost: string, organizace: string) {
    if (pristupnost.toUpperCase() === 'A') {
      return true;
    } else if (this.logged) {
      const sameOrg = this.user.organizace.id === organizace;
      const orgCanRead = this.user.pristupnost.toUpperCase().localeCompare('C'.toUpperCase()) > -1 && this.user.cteni_dokumentu;
      return orgCanRead ||
             this.user.pristupnost.toUpperCase().localeCompare(pristupnost.toUpperCase()) > -1 || 
             ((this.user.pristupnost.toUpperCase().localeCompare('C') > -1 && sameOrg));
    } else {
      return false;
    }
  }

  setMapResultById(docId: string) {
    if (docId === this.mapResult()?.ident_cely) {
      this.setMapResult(null, false);
    } else {
      const doc = this.solrResponse.response.docs.find(d => d.ident_cely === docId);
      this.setMapResult(doc, false);
    }
  }

  setMapResult(result: any, mapDetail: any) {
    const changed = (!result || (result.ident_cely !== this.mapResult()?.ident_cely));
    this.mapResult.set(result);
    // if (!result && !this.isMapaCollapsed) {
      
    //   return;
    // }
    if (mapDetail) {
      return;
    }
    if (changed) {
      this.mapResultSubject.next(result);
    }
  }

  changeMapView(sidenav: any) {
    sidenav.toggle();
    this.sidenavOpened = sidenav.opened;
    this.mapViewSubject.next('');
  }

  setLogged(res: any) {
    const changed = this.logged;
    if (res.error) {
      this.logged = false;
      this.user = null;
      this.ui = {sort:{}};
    } else {
      this.logged = true;
      this.user = res;

      
      if (this.user.ui) {
        this.ui = this.user.ui;
      }

      // for (const first in res) {
      //   if (res[first]) {
      //     this.user = res[first];
      //     this.logged = true;
      //     break;
      //   }
      // }
    }
    this.loggedSubject.next(changed === this.logged);
  }

}
