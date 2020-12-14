import { BrowserModule } from '@angular/platform-browser';
import { NgModule, APP_INITIALIZER } from '@angular/core';
import { HttpClientModule, HttpClient, HTTP_INTERCEPTORS } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DatePipe, registerLocaleData, DecimalPipe, CommonModule } from '@angular/common';
import localeCs from '@angular/common/locales/cs';

import { TranslateModule, TranslateLoader, TranslateService, TranslateParser } from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { OverlayModule } from '@angular/cdk/overlay';

import { NguCarouselModule } from '@ngu/carousel';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AppService } from 'src/app/app.service';
import { AppHeslarService } from 'src/app/app.heslar.service';
import { ApiInterceptor } from 'src/app/api.interceptor';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppState } from 'src/app/app.state';
import { HomeComponent } from 'src/app/pages/home/home.component';
import { DocumentComponent } from 'src/app/pages/document/document.component';
import { ExportComponent } from 'src/app/pages/export/export.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { FooterComponent } from './components/footer/footer.component';
import { FreeTextComponent } from './components/free-text/free-text.component';
import { ResultsComponent } from './pages/results/results.component';
import { TranslateHeslar } from 'src/app/translate-heslar.pipe';
import { FacetsComponent } from './components/facets/facets.component';
import { TimelineComponent } from './components/timeline/timeline.component';
import { FileViewerComponent } from './components/file-viewer/file-viewer.component';
import { AppMaterialModule } from 'src/app/app-material.module';
import { LokalitaComponent } from './entity/lokalita/lokalita.component';
import { TvarComponent } from './components/tvar/tvar.component';
import { NeidentAkceComponent } from './components/neident-akce/neident-akce.component';
import { DokJednotkaComponent } from './components/dok-jednotka/dok-jednotka.component';
import { KomponentaComponent } from './components/komponenta/komponenta.component';
import { ExterniZdrojComponent } from './components/externi-zdroj/externi-zdroj.component';
import { FlexLayoutModule } from '@angular/flex-layout';

import { environment } from 'src/environments/environment';
import { DokumentComponent } from './entity/dokument/dokument.component';
import { SearchbarComponent } from './components/searchbar/searchbar.component';
import { NapovedaComponent } from './pages/napoveda/napoveda.component';
import { PaginatorComponent } from './components/paginator/paginator.component';
import { PaginatorI18n } from './components/paginator/paginator-i18n';
import { FacetsUsedComponent } from './components/facets/facets-used/facets-used.component';
import { FacetsSearchComponent } from './components/facets/facets-search/facets-search.component';
import { FlotComponent } from './components/flot/flot.component';
import { ChartBarComponent } from './components/chart-bar/chart-bar.component';
import { ProjektComponent } from './entity/projekt/projekt.component';
import { AkceComponent } from './entity/akce/akce.component';
import { SamostatnyNalezComponent } from './entity/samostatny-nalez/samostatny-nalez.component';
import { LoginDialogComponent } from './components/login-dialog/login-dialog.component';
import { AdbComponent } from './components/adb/adb.component';
import { PianComponent } from './components/pian/pian.component';
import { LetComponent } from './components/let/let.component';
import { SidenavListComponent } from './components/sidenav-list/sidenav-list.component';
import { KontaktDialogComponent } from './components/footer/kontakt-dialog/kontakt-dialog.component';
import { LicenceDialogComponent } from './components/footer/licence-dialog/licence-dialog.component';
import { FavoritesComponent } from './pages/favorites/favorites.component';
import { LicenseDialogComponent } from './components/license-dialog/license-dialog.component';
import { DocumentDialogComponent } from './components/document-dialog/document-dialog.component';
import { MAT_DATE_LOCALE, DateAdapter, MAT_DATE_FORMATS } from '@angular/material/core';
import { MAT_MOMENT_DATE_FORMATS, MomentDateAdapter, MAT_MOMENT_DATE_ADAPTER_OPTIONS } from '@angular/material-moment-adapter';
import { InlineFilterComponent } from './components/inline-filter/inline-filter.component';
import { NalezComponent } from './components/nalez/nalez.component';
import { JednotkaDokumentuComponent } from './entity/jednotka-dokumentu/jednotka-dokumentu.component';
import { MapaComponent } from './components/mapa/mapa.component';
import { LeafletModule } from '@asymmetrik/ngx-leaflet';
import { Knihovna3dComponent } from './entity/knihovna3d/knihovna3d.component';
import { VyskovyBodComponent } from './components/vyskovy-bod/vyskovy-bod.component';
import { FacetsDynamicComponent } from './components/facets-dynamic/facets-dynamic.component';
import { BibtextDialogComponent } from './components/bibtext-dialog/bibtext-dialog.component';
import { MatPaginatorIntl } from '@angular/material/paginator';

registerLocaleData(localeCs, 'cs');

export function HttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http);
}

export function createTranslateLoader(http: HttpClient) {
  if (environment.mocked) {
    return new TranslateHttpLoader(http, './assets/i18n/', '.json');
  } else {
    return new TranslateHttpLoader(http, '/api/assets/i18n/', '.json');
  }
}

export const MY_FORMATS = {
  parse: {
    // dateInput: 'YYYY-MM-DD',
    dateInput: 'DD.MM.YYYY'
  },
  display: {
    dateInput: 'DD.MM.YYYY',
    monthYearLabel: 'MMM YYYY',
    dateA11yLabel: 'LL',
    monthYearA11yLabel: 'MMMM YYYY',
  },
};

export function createCustomMatPaginatorIntl(
  translateService: TranslateService
  ) {return new PaginatorI18n(translateService);}

const providers: any[] =[
  { provide: HTTP_INTERCEPTORS, useClass: ApiInterceptor, multi: true },
  { provide: MAT_DATE_LOCALE, useValue: 'cs-CZ' },
  {provide: DateAdapter, useClass: MomentDateAdapter,deps: [MAT_DATE_LOCALE, MAT_MOMENT_DATE_ADAPTER_OPTIONS]},
  // {provide: MAT_DATE_FORMATS, useValue: MAT_MOMENT_DATE_FORMATS},
  {provide: MAT_DATE_FORMATS, useValue: MY_FORMATS},
  AppState, AppConfiguration, HttpClient, 
  { provide: APP_INITIALIZER, useFactory: (config: AppConfiguration) => () => config.load(), deps: [AppConfiguration], multi: true },
  {
    provide: MatPaginatorIntl, deps: [TranslateService],
    useFactory: createCustomMatPaginatorIntl
  },
  DatePipe, DecimalPipe, AppService, AppHeslarService
];

@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    DocumentComponent,
    ExportComponent,
    ResultsComponent,
    NavbarComponent,
    FooterComponent,
    FreeTextComponent,
    TranslateHeslar,
    FacetsComponent,
    FacetsUsedComponent,
    FacetsSearchComponent,
    TimelineComponent,
    FileViewerComponent,
    DokumentComponent,
    Knihovna3dComponent,
    LokalitaComponent,
    AkceComponent,
    ProjektComponent,
    SamostatnyNalezComponent,
    DokJednotkaComponent,
    TvarComponent,
    NeidentAkceComponent,
    KomponentaComponent,
    ExterniZdrojComponent,
    SearchbarComponent,
    NapovedaComponent,
    PaginatorComponent,
    FlotComponent,
    ChartBarComponent,
    LoginDialogComponent,
    AdbComponent,
    PianComponent,
    LetComponent,
    SidenavListComponent,
    KontaktDialogComponent,
    LicenceDialogComponent,
    FavoritesComponent,
    LicenseDialogComponent,
    DocumentDialogComponent,
    InlineFilterComponent,
    NalezComponent,
    JednotkaDokumentuComponent,
    MapaComponent,
    VyskovyBodComponent,
    FacetsDynamicComponent,
    BibtextDialogComponent
  ],
  imports: [
    CommonModule,
    BrowserModule.withServerTransition({ appId: 'serverApp' }),
    HttpClientModule,
    FormsModule,
    AppRoutingModule,
    LeafletModule.forRoot(),
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: (createTranslateLoader),
        deps: [HttpClient]
      }
    }),
    BrowserAnimationsModule,
    OverlayModule,
    NguCarouselModule,
    AppMaterialModule,
    FlexLayoutModule
  ],
  entryComponents: [
    FileViewerComponent
  ],
  providers
})
export class AppModule { }
