
import { AppConfiguration } from 'src/app/app-configuration';
import { SolrResponse } from 'src/app/shared/solr-response';

import { Observable, Subject, ReplaySubject, BehaviorSubject } from 'rxjs';
import { Params, ParamMap } from '@angular/router';
import { NavigationExtras } from '@angular/router';
import { Configuration, Sort } from './shared/config';
import { User } from 'src/app/shared/user';
import { Crumb } from 'src/app/shared/crumb';
import { Condition } from 'src/app/shared/condition';
import { MatDialogRef } from '@angular/material/dialog';

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

  private mapResultSubject: Subject<string> = new Subject();
  public mapResultChanged: Observable<string> = this.mapResultSubject.asObservable();

  private mapViewSubject: Subject<string> = new Subject();
  public mapViewChanged: Observable<string> = this.mapViewSubject.asObservable();

  entity: string;

  public user: User;
  public logged = false;

  config: AppConfiguration;

  bodyClass = 'app-page-home';
  currentLang = 'cs';
  itemView = 'default';
  isMapaCollapsed = true;
  timelineOpened = true;
  printing = false;
  documentId: string;

  dialogRef: MatDialogRef<any, any>;
  mapResult: any; // Select entity in map view
  pianId: string; // Selected pian in map
  locationFilterEnabled: boolean; // Vyber na mape
  locationFilterBounds: any; // Vyber na mape


  // public loading: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  // public loading: Observable<boolean> = this._loading.asObservable();
  // loading = true;

  solrResponse: SolrResponse;
  loading: boolean;
  numFound: number;
  facets: { field: string, values: { name: string, type: string, value: number, operator: string }[] }[] = [];
  facetsFiltered: { field: string, values: { name: string, type: string, value: number, operator: string }[] }[];
  facetFilterValue: string;
  areFacetsFiltered: boolean;
  stats: { [field: string]: { min: any, max: any, count: number, from: any, until: any } };

  totals: { [entity: string]: number } = {};

  facetRanges;
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

  heatMaps;

  q: string;
  rows: number;
  page = 0;
  pageChanged = false;
  totalPages: number;
  sorts: Sort[];
  sort: Sort;

  // Pokud uzivatel zvoli jine razeni pro danou facetu, napr: {"obdobi": "poradi"}
  facetSort: {[facetname: string]: string} = {};

  hideWithoutThumbs = false;
  inFavorites: boolean;
  obdobi: any;

  breadcrumbs: Crumb[];
  conditions: Condition[];

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

  setSearchResponse(resp: SolrResponse) {
    this.solrResponse = resp;
    this.numFound = resp.response.numFound;
    this.totalPages = this.numFound / this.rows;
    this.stats = resp.stats.stats_fields;
    const keys = Object.keys(this.stats);
    keys.forEach(k => {
      this.stats[k].from = this.stats[k].min;
      this.stats[k].until = this.stats[k].max;
    });
    this.stats.datum_provedeni = {
      min: this.stats.datum_provedeni_od.min,
      max: this.stats.datum_provedeni_do.max,
      count: this.stats.datum_provedeni_od.count,
      from: this.stats.datum_provedeni_od.min,
      until: this.stats.datum_provedeni_do.max
    };

    if (resp.facet_counts) {
      this.setEntityTotals(resp.facet_counts.facet_fields['entity']);
    }
    

    this.resultsSubject.next({typ: 'results', pageChanged: this.pageChanged});
    this.pageChanged = false;
    setTimeout(() => {
      this.setFacets(resp);
    }, 100);
  }

  setFacets(resp) {
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
    this.facetsSubject.next('facets');
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
          case 'kategorie_dokumentu':
            const fp = { field, values: [], count: -1 };
            pivots[f].forEach(f1 => {
              fp.values.push(f1);
            });
            this.facetPivots.push(fp);
            break;
          default:
            const facetPivoted = {};
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
            const values = [];
            fpKeys.forEach(k => {
              values.push(facetPivoted[k]);
            });
            this.facetsFiltered.push({ field, values });

        }
      });
    }

  }

  setRouteChanged(val) {
    this.routeSubject.next(val);
  }

  processParams(params: ParamMap) {
    this.hideWithoutThumbs = params.has('hideWithoutThumbs') ? params.get('hideWithoutThumbs') === 'true' : false;
    this.inFavorites = params.has('inFavorites') ? params.get('inFavorites') === 'true' : false;
    this.entity = params.has('entity') ? params.get('entity') : 'dokument';
    this.sorts = this.config.sorts.filter(s => !s.entity || s.entity.includes(this.entity));
    this.page = params.has('page') ? +params.get('page') : 0;
    this.isMapaCollapsed = params.has('mapa') ? params.get('mapa') === 'false' : true;
    if (this.isMapaCollapsed) {
      this.rows = params.has('rows') ? +params.get('rows') : this.config.defaultRows;
    } else {
      this.rows = this.config.mapOptions.docsForMarker;
    }
    this.pianId = params.has('pian_id') ? params.get('pian_id') : null;

    this.sort = null;
    if (params.has('sort')) {
      this.sort = this.sorts.find(s => (s.field + ' ' + s.dir) === params.get('sort'));
    }
    if (!this.sort) {
      this.sort = this.sorts[0];
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
    if (pristupnost === 'A') {
      return true;
    } else if (this.logged) {
      const sameOrg = this.user.organizaceNazev === organizace;
      return this.user.pristupnost.localeCompare(pristupnost) > -1 || ((this.user.pristupnost.localeCompare('C') > -1 && sameOrg));
    } else {
      return false;
    }
  }

  setMapResultById(docId: string) {
    if (docId === this.mapResult?.ident_cely) {
      this.setMapResult(null, false);
    } else {
      const doc = this.solrResponse.response.docs.find(d => d.ident_cely === docId);
      this.setMapResult(doc, false);
    }
  }

  setMapResult(result, mapDetail) {
    if (mapDetail) {
      return;
    }
    const changed = (!result || (result.ident_cely !== this.mapResult?.ident_cely));
    this.mapResult = result;
    if (changed) {
      this.mapResultSubject.next(result);
    }
  }

  changeMapView(sidenav) {
    sidenav.toggle();
    this.mapViewSubject.next('');
  }

  setLogged(res: any) {
    const changed = this.logged;
    if (res.error) {
      this.logged = false;
      this.user = null;
    } else {
      this.logged = false;
      this.user = null;
      for (const first in res) {
        if (res[first]) {
          this.user = res[first];
          this.logged = true;
          break;
        }
      }
    }
    this.loggedSubject.next(changed === this.logged);
  }

}
