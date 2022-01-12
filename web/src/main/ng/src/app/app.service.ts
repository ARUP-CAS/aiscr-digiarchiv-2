import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { map } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppState } from 'src/app/app.state';
import { TranslateService } from '@ngx-translate/core';
import { Subject, Observable, BehaviorSubject } from 'rxjs';
import { SolrResponse } from 'src/app/shared/solr-response';
import { DecimalPipe, isPlatformBrowser } from '@angular/common';
import { Crumb } from 'src/app/shared/crumb';
import { Condition } from 'src/app/shared/condition';
import { ParamMap, Router } from '@angular/router';
import { AppWindowRef } from './app.window-ref';
import { MatDialog } from '@angular/material/dialog';
import { BibtextDialogComponent } from './components/bibtext-dialog/bibtext-dialog.component';

@Injectable({
  providedIn: 'root'
})
export class AppService {

  // Observe language
  // private langSubject: ReplaySubject<string> = new ReplaySubject(3);
  private langSubject: Subject<string> = new Subject();
  public currentLang: Observable<string> = this.langSubject.asObservable();

  constructor(
    @Inject(PLATFORM_ID) private platformId: any,
    private dialog: MatDialog,
    private windowRef: AppWindowRef,
    private decimalPipe: DecimalPipe,
    private router: Router,
    private config: AppConfiguration,
    private state: AppState,
    private translate: TranslateService,
    private http: HttpClient) { }

  print() {
    if (isPlatformBrowser(this.platformId)) {
      if (this.windowRef.nativeWindow.print) {
        this.windowRef.nativeWindow.print();
      }
    }
  }

  showBiBText(id: string, bibTex: string) {
    // this.state.dialogRef = this.dialog.open(BibtextDialogComponent, {
    //   width: '800px',
    //   data: bibTex,
    //   panelClass: 'app-login-dialog'
    // });

    const blob: Blob = new Blob([bibTex], {type: 'text/plain'});
    const fileName: string = id + '.bib';
    const objectUrl: string = URL.createObjectURL(blob);
    const a: HTMLAnchorElement = document.createElement('a') as HTMLAnchorElement;

    a.href = objectUrl;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();        

    document.body.removeChild(a);
    URL.revokeObjectURL(objectUrl);


  }

  addFilter(field: string, value: string, operator: string) {
    let used = false;
    const params: any = {};
    params[field] = [];
    this.state.breadcrumbs.forEach((c: Crumb) => {
      if (c.field === field) {
        if (c.value !== value) {
          params[field].push(c.value + ':' + c.operator);
        } else {
          used = true;
        }
      }
    });
    if (!used) {
      params[field].push(value + ':' + operator);
    }
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  changeLang(lang: string) {
    this.translate.use(lang).subscribe(val => {
      this.state.currentLang = lang;
      this.langSubject.next(lang);
    });
  }

  getHeslarTranslation(value: string, heslar: string): any {
    // if (this.config.poleToHeslar.hasOwnProperty(heslar)) {
    //   heslar = this.config.poleToHeslar[heslar];
    // }
    const hkey = heslar + '_' + value;
    const t = this.translate.instant(hkey);
    if (t === hkey) {
      return value;
    } else {
      return t;
    }

  }

  getTranslation(s: string): string {
    return this.translate.instant(s);
  }

  private get<T>(url: string, params: HttpParams = new HttpParams(), responseType?): Observable<T> {
    // const r = re ? re : 'json';
    const options = { params, responseType, withCredentials: true };
    if (environment.mocked) {
      return this.http.get<T>(`api${url}`, options);
    } else {
      const server = isPlatformBrowser(this.platformId) ? '' : this.config.amcr;
      return this.http.get<T>(`${server}api${url}`, options);
    }

  }

  private post(url: string, obj: any) {
    return this.http.post<any>(`api${url}`, obj);
  }

  /**
   * Fired for main search in results page
   * @param params the params
   */
  search(params: HttpParams) {
    return this.get(`/search/query`, params);
  }

  /**
   * Fired search in results page
   * @param params the params
   */
  getDokument(id: string) {
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(`/search/dokument`, params);
  }

  getId(id: string) {
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(`/search/id`, params);
  }

  getGML(id: string) {
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(`/search/gml`, params);
  }

  getWKT(id: string) {
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(`/search/wkt`, params);
  }

  getGeometrie(id: string, format: string) {
    const params: HttpParams = new HttpParams().set('id', id).set('format', format);
    return this.get(`/search/geometrie`, params);
  }

  searchMapa(params: HttpParams) {
    return this.get('/search/mapa', params);
  }

  getPians(params: HttpParams) {
    return this.get('/search/pians', params);
  }

  getHome() {
    const params: HttpParams = new HttpParams();
    return this.get('/search/home');
  }

  getTotals(params: HttpParams) {
    return this.get('/search/totals', params);
  }

  getObdobi() {
    const url = '/search/obdobi';
    return this.get(url);
  }

  getText(id: string): Observable<string> {
    const params: HttpParams = new HttpParams().set('id', id).set('lang', this.state.currentLang);
    return this.get(`/texts/get`, params, 'text');
  }

  getAkce(id: string) {
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(`/search/akce`, params)
      .pipe(map((resp: any) => {
        return resp.response.docs;
      }));
  }

  getLokalita(id: string) {
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(`/search/lokalita`, params)
      .pipe(map((resp: any) => {
        return resp.response.docs;
      }));
  }

  getDokJednotky(id: string) {
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(`/search/dokjednotky`, params)
      .pipe(map((resp: any) => {
        return resp.response.docs;
      }));
  }

  getExterniOdkaz(id: string) {
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(`/search/externiodkaz`, params)
      .pipe(map((resp: any) => {
        return resp.response.docs;
      }));
  }

  getExterniZdroj(id: string) {
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(`/search/externizdroj`, params)
      .pipe(map((resp: any) => {
        return resp.response.docs;
      }));
  }

  getNalez(id: string) {
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(`/search/nalez`, params)
      .pipe(map((resp: any) => {
        return resp.response.docs;
      }));
  }

  getNalezKomponentaDok(id: string) {
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(`/search/nalezkomponentadok`, params)
      .pipe(map((resp: any) => {
        return resp.response.docs;
      }));
  }

  getKomponenty(id: string) {
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(`/search/getkomponenty`, params)
      .pipe(map((resp: any) => {
        return resp.response.docs;
      }));
  }

  getIsFav(uniqueid: string): Observable<boolean> {
    const url = '/fav/get';
    return this.get<boolean>(url, new HttpParams().set('docid', uniqueid))
      .pipe(map((resp: any) => {
        if (resp.error) {
          return false;
        } else {
          return resp.response.numFound > 0;
        }

      }));
  }

  

  feedback(name: string, mail: string, text: string, ident_cely: string) {
    const url = '/feedback';
    return this.post(url, { name, mail, text, ident_cely });
  }

  addFav(identCely: string) {
    const url = '/fav/add';
    return this.post(url, { docid: identCely, entity: this.state.entity });
  }

  removeFav(identCely: string) {
    const url = '/fav/remove';
    return this.post(url, { docid: identCely });
  }

  getLogged(wantsUser: boolean) {
    const url = '/user/islogged';
    return this.get(url, new HttpParams().set('wantsUser', wantsUser+''));
  }

  login(user: string, pwd: string) {
    const url = '/user/login';
    return this.post(url, { user, pwd });
  }

  logout() {
    const url = '/user/logout';
    return this.get(url);
  }

  setCrumbs(params: ParamMap) {
    this.state.breadcrumbs = [];
    this.state.conditions = [];
    this.state.obdobi = null;
    this.state.q = null;
    if (params.has('adv')) {
      this.setConditionsFromUrl(params.getAll('adv'));
    }
    if (params.has('q')) {
      this.state.q = params.get('q');
      this.state.breadcrumbs.push(new Crumb('q', params.get('q'), params.get('q')));
      this.state.breadcrumbs.push(new Crumb('separator', '', ''));
    }

    if (params.has('loc_rpt')) {
      if (this.state.locationFilterEnabled) {
        const value = params.get('loc_rpt');
        this.state.breadcrumbs.push(new Crumb('loc_rpt', value, this.formatLocation(value)));
        this.state.breadcrumbs.push(new Crumb('separator', '', ''));
      }
    } else {
      this.state.locationFilterEnabled = false;
    }

    // this.config.urlFields.forEach(field => {
    //   if (params.has(field)) {
    params.keys.forEach(field => {
      if (field.startsWith('f_') || 
      this.config.urlFields.includes(field) ||
      // this.config.dateFacets.includes(field) ||
      this.config.numberFacets.includes(field) ||
      this.config.filterFields.find(ff => ff.field === field)) {
        let display = '';
        switch (field) {
          case 'obdobi_poradi': {
            const value = params.get(field);
            display = this.formatObdobi(value);
            this.state.obdobi = value;
            this.state.breadcrumbs.push(new Crumb(field, value, display));
            this.state.breadcrumbs.push(new Crumb('separator', '', ''));
            break;
          }
          case 'kategorie_dokumentu': {
            const values = params.getAll(field);
            values.forEach(value => {
              const parts = value.split(':');
              display = this.translate.instant('kategorie_dokumentu.' + parts[0]);
              this.state.breadcrumbs.push(new Crumb(field, parts[0], display, parts[1]));
            });
            this.state.breadcrumbs.push(new Crumb('separator', '', ''));
            break;
          }
          case 'pian_id': {
            break;
          }
          case 'loc_rpt': {
            break;
          }
          default: {
            const values = params.getAll(field);
            const filterField = this.config.filterFields.find(ff => ff.field === field);
            values.forEach(value => {
              const parts = value.split(':');
              if (filterField && filterField.type === 'number') {
                const oddo = parts[0].split(',');
                display = this.getTranslation(oddo[0]) + ' - ' + this.getTranslation(oddo[1]);
              } else {
                display = this.getHeslarTranslation(parts[0], field);
              }
              this.state.breadcrumbs.push(new Crumb(field, parts[0], display, parts[1]));
            });
            this.state.breadcrumbs.push(new Crumb('separator', '', ''));
          }
        }
        
      }
    });
    this.state.breadcrumbs.pop();
  }

  // shouldTranslate(field: string) {
  //   return this.config.facets.translate.hasOwnProperty(field);
  // }

  // getTranslatePrefix(field: string) {
  //   if (this.shouldTranslate(field)) {
  //     return this.config.facets.translate[field].heslar;
  //   } else {
  //     return field;
  //   }
  // }

  formatLocation(val: string): string {
    const coords = val.split(',');
    return '(' +
      this.formatNumber(coords[0]) + ' ' +
      this.formatNumber(coords[1]) + ', ' +
      this.formatNumber(coords[2]) + ' ' +
      this.formatNumber(coords[3]) + ')';
  }

  formatNumber(n: string) {
    return this.decimalPipe.transform(parseFloat(n), '2.1-3');
  }

  formatObdobi(val: string): string {
    const idx1 = val.split(',')[0];
    const idx2 = val.split(',')[1];
    // const v1 = this.config.obdobi[idx1].poradi;
    // const v2 = this.config.obdobi[idx2].poradi;
    const ob1 = this.config.obdobi.find(o => (o.poradi + '') === (idx1 + ''));
    const d1 = this.getTranslation(ob1.nazev);
    const ob2 = this.config.obdobi.find(o => (o.poradi + '') === (idx2 + ''));
    const d2 = this.getTranslation(ob2.nazev);
    return this.getTranslation('from') + ' ' + d1 + ' ' + this.getTranslation('to') + ' ' + d2;
  }

  setConditionsFromUrl(adv: string[]) {
    adv.forEach(adv1 => {
      this.state.conditions.push(JSON.parse(adv1));
    });
  }

  filterFacets(filter: string) {
    if (!filter) {
      this.state.facetsFiltered = Object.assign([], this.state.facets);
      this.state.facetFilterValue = '';
      this.state.areFacetsFiltered = false;
      return;
    }

    const pattern = new RegExp(filter, 'i');
    this.state.facetsFiltered = [];
    this.state.facets.forEach(f => {
      const values = f.values.filter(v => {
        const translated = this.getHeslarTranslation(v.name, f.field);
        return pattern.test(translated);
      });
      if (values.length > 0) {
        this.state.facetsFiltered.push({ field: f.field, values });
      }
    });
    this.state.areFacetsFiltered = true;
  }

  showInMap(result: any, isPian = false) {
    // const top = window.document.getElementsByTagName('header')[0].clientHeight;
    this.windowRef.nativeWindow.document.getElementsByTagName('mat-sidenav-content')[0].scroll(0,0);

    this.state.isMapaCollapsed = false;
    const p: any = {};
    p.mapa = true;
    let url = '/results';
    if (isPian) {
      p.pian_id = result.ident_cely;
      p.entity = result.akce ? 'akce' : (result.lokalita ? 'lokalita' : 'akce');
      this.state.setMapResult(null, false);
    } else if (this.router.isActive('/id', false)) {
      this.state.setMapResult(result, false);
      url = '/id/' + this.state.documentId;
      p.loc_rpt = null;
    } else {
      this.state.mapResult = result;
      // this.state.setMapResult(result, false);
    }
    
    this.router.navigate([url], { queryParams: p, queryParamsHandling: 'merge' });

    setTimeout(() => {
    }, 10);
  }

  /**
   * returns katastr from dalsi_katastry
   * @param s string with format katastr (okres)
   */
  getKatastr(s: string) {
    return s.substring(0, s.indexOf('(')).trim();
  }

  /**
   * returns okres from dalsi_katastry
   * @param s string with format katastr (okres)
   */
  getOkres(s: string) {
    return s.substring(s.indexOf('(') + 1, s.indexOf(')')).trim();
  }

}

