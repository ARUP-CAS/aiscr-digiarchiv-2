import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { map, catchError, finalize } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppState } from 'src/app/app.state';
import { TranslateService } from '@ngx-translate/core';
import { Subject, Observable, of } from 'rxjs';
import { SolrResponse } from 'src/app/shared/solr-response';
import { DecimalPipe, isPlatformBrowser } from '@angular/common';
import { Crumb } from 'src/app/shared/crumb';
import { ParamMap, Router } from '@angular/router';
import { AppWindowRef } from './app.window-ref';
import { MatDialog } from '@angular/material/dialog';
import { AlertDialogComponent } from './components/alert-dialog/alert-dialog.component';
declare var L;

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
        this.state.printing = false;
        this.state.loading = false;
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

  getTranslationByField(value: string, field: string): any {
    switch (field) {
      case 'obdobi_poradi': {
        return this.formatObdobi(value);
      }
      case 'dokument_kategorie_dokumentu': {
        
        const hkey = 'dokument_kategorie_dokumentu.' + value;
        const t = this.translate.instant(hkey);
        if (t === hkey) {
          return value;
        } else {
          return t;
        }
      }
      case 'pian_id': {
        break;
      }
      case 'loc_rpt': {
        break;
      }
      case 'vyber': {
        break;
      }
      default: {
        const hkey = 'heslar.' + value;
        const t = this.translate.instant(hkey);
        if (t === hkey) {
          return value;
        } else {
          return t;
        }
      }
    }
    

  }

  getHeslarTranslation(value: string, heslar: string): any {
    // const hkey = heslar + '_' + value;
    const hkey = 'heslar.' + value;
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

  stopLoading() {
    this.state.loading = false;
    this.state.facetsLoading = false;
  }

  public showInfoDialog(message: string, duration: number = 2000) {
    const data = {
      type: 'success',
      title: 'Info',
      message: [message]
    };
    const dialogRef = this.dialog.open(AlertDialogComponent, {
         data,
         width: '400px',
         panelClass: 'app-alert-dialog'
    });
    
    dialogRef.afterOpened().subscribe(_ => {
      setTimeout(() => {
         dialogRef.close();
      }, duration)
    })
  }

  public showErrorDialog(title: string, message: string) {
    const data = {
      type: 'error',
      title: title,
      message: [message]
    };
    const dialogRef = this.dialog.open(AlertDialogComponent, {
         data,
         width: '400px',
         panelClass: 'app-alert-dialog'
    });
    
  }

  private handleError(error: HttpErrorResponse, me: any) {
    //  console.log(error);
    if (error.status === 0) {
      // A client-side or network error occurred. Handle it accordingly.
      console.error('An error occurred:', error.error);
      this.showErrorDialog('error', 'An error occurred: ' + error.error);
    } else if (error.status === 503 || error.status === 504) {
      // Forbiden. Redirect to login
      console.log("Service Unavailable");
      this.showErrorDialog('error', "Service Unavailable");
      
    } else if (error.status === 403) {
      // Forbiden. Redirect to login
      console.log("Forbiden");
      this.showErrorDialog('error', "Forbiden");
    } else {
      console.error(`Backend returned code ${error.status}, body was: `, error.error);
      this.showErrorDialog('error', `Backend returned code ${error.status}, body was: ` + error.error);
    }
    // Return an observable with a user-facing error message.
    // return throwError({'status':error.status, 'message': error.message});
    this.state.hasError = true;
    return of({ 'status': error.status, 'message': error.message, 'error': [error.error] });
  }

  private get<T>(url: string, params: HttpParams = new HttpParams(), responseType?): Observable<Object> {
    // const r = re ? re : 'json';
    this.state.hasError = false;
    const options = { params, responseType, withCredentials: true };
    if (environment.mocked) {
      return this.http.get<T>(`api${url}`, options);
    } else {
      const server = isPlatformBrowser(this.platformId) ? '' : this.config.amcr;
      return this.http.get<T>(`${server}api${url}`, options)
      .pipe(map((r: any) => {
        if (r.response?.status === -1) {
          r.response.errors = { path: [{ errorMessage: r.response.errorMessage }] };
        }
        return r;

      }))
      .pipe(finalize(() => this.stopLoading()))
      .pipe(catchError(err => this.handleError(err, this)));
    }

  }

  private post(url: string, obj: any) {
    return this.http.post<any>(`api${url}`, obj);
  }

  /**
   * Fired for main search in results page
   * @param params the params
   */
  search(params: HttpParams): Observable<any> {
    this.state.hasError = false;
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

  checkRelations(id: string): Observable<any> {
    const params: HttpParams = new HttpParams()
      .set('id', id);
    return this.get(`/search/check_relations`, params);
  }

  getId(id: string): Observable<any > {
    const params: HttpParams = new HttpParams()
      .set('id', id);
    return this.get(`/search/id`, params);
  }

  getIdAsChild(ids: string[], entity: string) {
    let params: HttpParams = new HttpParams()
    .set('entity', entity);
    ids.forEach(id => {
      params = params.append('id', id);
    });
    
    return this.get(`/search/id_as_child`, params);
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
    return this.get(`/texts/get`, params, 'text')
    .pipe(map((response: any) => response));
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

    if (params.has('vyber')) {

      // if (this.state.locationFilterEnabled) {
        this.state.locationFilterEnabled = true;
        const value = params.get('vyber');


        this.state.breadcrumbs.push(new Crumb('vyber', value, this.formatLocation(value)));
        this.state.breadcrumbs.push(new Crumb('separator', '', ''));

        const loc_rpt = value.split(',');
        const southWest = L.latLng(loc_rpt[0], loc_rpt[1]);
        const northEast = L.latLng(loc_rpt[2], loc_rpt[3]);
        this.state.locationFilterBounds = L.latLngBounds(southWest, northEast);

      // }
    } else {
      this.state.locationFilterEnabled = false;
      this.state.locationFilterBounds = null;
    }

    // this.config.urlFields.forEach(field => {
    //   if (params.has(field)) {
    params.keys.forEach(field => {
      if (field.startsWith('f_') || 
      this.config.urlFields.includes(field) ||
      // this.config.dateFacets.includes(field) ||
      // this.config.numberFacets.includes(field) ||
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
          case 'dokument_kategorie_dokumentu': {
            const values = params.getAll(field);
            values.forEach(value => {
              const parts = value.split(':');
              display = this.translate.instant('dokument_kategorie_dokumentu.' + parts[0]);
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
          case 'vyber': {
            break;
          }
          // case 'az_dj_negativni_jednotka': {
          //   const value = params.get(field);
          //   display = 'a' + value;
          //   this.state.breadcrumbs.push(new Crumb(field, value, display));
          //   break;
          // }
          default: {
            const values = params.getAll(field);
            const filterField = this.config.filterFields.find(ff => ff.field === field);
            values.forEach(value => {
              const parts = value.split(':');
              const op = parts[parts.length - 1];
              const val = value.substring(0,value.lastIndexOf(':'));
              if (filterField && filterField.type === 'number') {
                const oddo = parts[0].split(',');
                display = this.getTranslation(oddo[0]) + ' - ' + this.getTranslation(oddo[1]);
              } else {
                // display = this.getHeslarTranslation(parts[0], field);
                display = null;

              }
              this.state.breadcrumbs.push(new Crumb(field, val, display, op));
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
      this.state.setFacetChanged();
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
    this.state.setFacetChanged();
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
      p.vyber = null;
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

