import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideHttpClient, withFetch } from '@angular/common/http';

import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { AppService } from './app.service';
import { AppState } from './app.state';
import { AppConfiguration } from './app-configuration';
import { DatePipe, DecimalPipe } from '@angular/common';

import { MAT_DATE_LOCALE, DateAdapter, MAT_DATE_FORMATS, provideNativeDateAdapter } from '@angular/material/core';
import { MomentDateAdapter, MAT_MOMENT_DATE_ADAPTER_OPTIONS } from '@angular/material-moment-adapter';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { PaginatorI18n } from './components/paginator/paginator-i18n';



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
) { return new PaginatorI18n(translateService); }


export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideAppInitializer(() => inject(AppConfiguration).load()),
    provideRouter(routes),
    provideClientHydration(withEventReplay()),

    provideNativeDateAdapter(),
    { provide: MAT_DATE_LOCALE, useValue: 'cs-CZ' },
    { provide: DateAdapter, useClass: MomentDateAdapter, deps: [MAT_DATE_LOCALE, MAT_MOMENT_DATE_ADAPTER_OPTIONS] },
    { provide: MAT_DATE_FORMATS, useValue: MY_FORMATS },

    provideTranslateService({
      loader: provideTranslateHttpLoader({
        prefix: 'api/assets/i18n/',
        suffix: '.json'
      }),
      fallbackLang: 'en',
      lang: 'en'
    }),

    {
      provide: MatPaginatorIntl, deps: [TranslateService],
      useFactory: createCustomMatPaginatorIntl
    },

    provideHttpClient(withFetch()),
    // { provide: APP_INITIALIZER, useFactory: (config: AppConfiguration) => () => config.load(), deps: [AppConfiguration], multi: true },
    TranslateService,
    DecimalPipe, DatePipe,
    AppState,
    AppService
  ]
};
