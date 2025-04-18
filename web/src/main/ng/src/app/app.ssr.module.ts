import { BrowserModule } from '@angular/platform-browser';
import { NgModule, APP_INITIALIZER } from '@angular/core';
import { HttpClientModule, HttpClient, HTTP_INTERCEPTORS } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DatePipe, registerLocaleData, DecimalPipe, CommonModule } from '@angular/common';
import localeCs from '@angular/common/locales/cs';

import { TranslateModule, TranslateLoader } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';

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
import { ConsentSheet, FooterComponent } from './components/footer/footer.component';
import { FreeTextComponent } from './components/free-text/free-text.component';
import { ResultsComponent } from './pages/results/results.component';
import { TranslateHeslar } from 'src/app/translate-heslar.pipe';
import { FacetsComponent } from './components/facets/facets.component';
import { TimelineComponent } from './components/timeline/timeline.component';
import { FileViewerComponent } from './components/file-viewer/file-viewer.component';
import { AppMaterialModule } from 'src/app/app-material.module';
import { LokalitaComponent } from './entity/lokalita/lokalita.component';
import { TvarComponent } from './components/tvar/tvar.component';
import { DokJednotkaComponent } from './components/dok-jednotka/dok-jednotka.component';
import { KomponentaComponent } from './components/komponenta/komponenta.component';
import { ExterniZdrojComponent } from './components/externi-zdroj/externi-zdroj.component';
import { FlexLayoutModule } from '@angular/flex-layout';

import { environment } from 'src/environments/environment';
import { DokumentComponent } from './entity/dokument/dokument.component';
import { SearchbarComponent } from './components/searchbar/searchbar.component';
import { NapovedaComponent } from './pages/napoveda/napoveda.component';
import { PaginatorComponent } from './components/paginator/paginator.component';
import { FacetsUsedComponent } from './components/facets/facets-used/facets-used.component';
import { FacetsSearchComponent } from './components/facets/facets-search/facets-search.component';
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
import { LicenseDialogComponent } from './components/license-dialog/license-dialog.component';
import { DocumentDialogComponent } from './components/document-dialog/document-dialog.component';
import { MAT_DATE_LOCALE, DateAdapter, MAT_DATE_FORMATS } from '@angular/material/core';
import { MAT_MOMENT_DATE_FORMATS, MomentDateAdapter, MAT_MOMENT_DATE_ADAPTER_OPTIONS } from '@angular/material-moment-adapter';
import { InlineFilterComponent } from './components/inline-filter/inline-filter.component';
import { RelatedComponent } from './components/related/related.component';
import { NalezComponent } from './components/nalez/nalez.component';
import { JednotkaDokumentuComponent } from './entity/jednotka-dokumentu/jednotka-dokumentu.component';
import { MapaShimComponent } from './components/mapa-shim/mapa-shim.component';
import { Knihovna3dComponent } from './entity/knihovna3d/knihovna3d.component';
import { VyskovyBodComponent } from './components/vyskovy-bod/vyskovy-bod.component';
import { FacetsDynamicComponent } from './components/facets-dynamic/facets-dynamic.component';
import { BibtextDialogComponent } from './components/bibtext-dialog/bibtext-dialog.component';
import { FeedbackDialogComponent } from './components/feedback-dialog/feedback-dialog.component';
import { KomponentaDokumentComponent } from './components/komponenta-dokument/komponenta-dokument.component';
import { ExportMapaComponent } from './pages/export-mapa/export-mapa.component';
import { RecaptchaFormsModule, RecaptchaModule, RecaptchaSettings, RECAPTCHA_SETTINGS } from 'ng-recaptcha';
import { CitationComponent } from './components/citation/citation.component';
import { ResultActionsComponent } from './components/result-actions/result-actions.component';
import { DokumentCastComponent } from './components/dokument-cast/dokument-cast.component';
import { AlertDialogComponent } from './components/alert-dialog/alert-dialog.component';
import { StatsComponent } from './pages/stats/stats.component';

import { NgxEchartsModule } from 'ngx-echarts';
import * as echarts from 'echarts/core';
import { BarChart } from 'echarts/charts';
import { LineChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { MapViewShimComponent } from './components/mapa-shim/map-view-shim.component';
import { MapViewContainerComponent } from './pages/map-view/map-view-container.component';
echarts.use([TitleComponent, TooltipComponent, GridComponent, BarChart, LineChart, LegendComponent, CanvasRenderer]);

registerLocaleData(localeCs, 'cs');

export function HttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http);
}

export function createTranslateLoader(http: HttpClient) {
  if (environment.mocked) {
    return new TranslateHttpLoader(http, './assets/i18n/', '.json');
  } else {
    // let server = 'http://localhost:8080/amcr/';
    let server = '';
    const args = process.argv;
    if (args.length > 2) {
      server = args[2];
    }
    return new TranslateHttpLoader(http, server + 'api/assets/i18n/', '.json');
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

const providers: any[] = [
  { provide: HTTP_INTERCEPTORS, useClass: ApiInterceptor, multi: true },
  { provide: MAT_DATE_LOCALE, useValue: 'cs-CZ' },
  { provide: DateAdapter, useClass: MomentDateAdapter, deps: [MAT_DATE_LOCALE, MAT_MOMENT_DATE_ADAPTER_OPTIONS] },
  // {provide: MAT_DATE_FORMATS, useValue: MAT_MOMENT_DATE_FORMATS},
  { provide: MAT_DATE_FORMATS, useValue: MY_FORMATS },
  AppState, AppConfiguration, HttpClient,
  { provide: APP_INITIALIZER, useFactory: (config: AppConfiguration) => () => config.load(), deps: [AppConfiguration], multi: true },
  {
    provide: RECAPTCHA_SETTINGS,
    useValue: { siteKey: environment.recaptcha.siteKey } as RecaptchaSettings,
  },
  DatePipe, DecimalPipe, AppService, AppHeslarService
];

@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    DocumentComponent,
    ExportComponent,
    ExportMapaComponent,
    NavbarComponent,
    FooterComponent,
    FreeTextComponent,
    ResultsComponent,
    TranslateHeslar,
    FacetsComponent,
    TimelineComponent,
    FileViewerComponent,
    LokalitaComponent,
    TvarComponent,
    DokJednotkaComponent,
    KomponentaComponent,
    ExterniZdrojComponent,
    DokumentComponent,
    SearchbarComponent,
    NapovedaComponent,
    PaginatorComponent,
    FacetsUsedComponent,
    FacetsSearchComponent,
    FacetsDynamicComponent,
    ProjektComponent,
    AkceComponent,
    SamostatnyNalezComponent,
    LoginDialogComponent,
    AdbComponent,
    PianComponent,
    DokumentCastComponent, 
    LetComponent,
    SidenavListComponent,
    KontaktDialogComponent,
    LicenceDialogComponent,
    LicenseDialogComponent,
    DocumentDialogComponent,
    InlineFilterComponent,
    NalezComponent,
    JednotkaDokumentuComponent,
    MapaShimComponent,
    MapViewContainerComponent,
    MapViewShimComponent,
    Knihovna3dComponent,
    VyskovyBodComponent,
    BibtextDialogComponent,
    FeedbackDialogComponent,
    AlertDialogComponent,
    KomponentaDokumentComponent,
    CitationComponent,
    ResultActionsComponent,
    StatsComponent,
    ConsentSheet,
    RelatedComponent
  ],
  imports: [
    CommonModule,
    // BrowserModule.withServerTransition({ appId: 'serverApp' }),
    HttpClientModule,
    FormsModule,
    AppRoutingModule,
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
    FlexLayoutModule,
    RecaptchaModule,
    RecaptchaFormsModule,
    NgxEchartsModule.forRoot({
      // echarts: () => import('echarts'), 
      echarts
    })
  ],
  // entryComponents: [
  //   FileViewerComponent
  // ],
  providers
})
export class AppSsrModule { }
