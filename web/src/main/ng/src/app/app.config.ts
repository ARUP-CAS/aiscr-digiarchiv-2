import { ApplicationConfig, importProvidersFrom, inject, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideHttpClient, withFetch } from '@angular/common/http';

import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { AppService } from './app.service';
import { AppState } from './app.state';
import { AppConfiguration } from './app-configuration';
import { DatePipe, DecimalPipe, registerLocaleData } from '@angular/common';

import { MAT_DATE_LOCALE, MAT_DATE_FORMATS } from '@angular/material/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { PaginatorI18n } from './components/paginator/paginator-i18n';
import { RECAPTCHA_V3_SITE_KEY, RecaptchaV3Module } from 'ng-recaptcha-2';
import { environment } from '../environments/environment';

import {provideLuxonDateAdapter} from '@angular/material-luxon-adapter';

import localeCs from '@angular/common/locales/cs';
registerLocaleData(localeCs);

export const MY_FORMATS = {
  parse: {
    // dateInput: 'YYYY-MM-DD',
    dateInput: 'd.M.yyyy'
  },
  display: {
    dateInput: 'dd.MM.yyyy',
    monthYearLabel: 'MMM yyyy',
    dateA11yLabel: 'LL',
    monthYearA11yLabel: 'MMMM yyyy',
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

    // provideNativeDateAdapter(),
    // provideMomentDateAdapter(), 
    { provide: MAT_DATE_LOCALE, useValue: 'cs-CZ' },
    provideLuxonDateAdapter(),
    //{ provide: DateAdapter, useClass: MomentDateAdapter, deps: [MAT_DATE_LOCALE, MAT_MOMENT_DATE_ADAPTER_OPTIONS] },
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

    //importProvidersFrom(RecaptchaV3Module),
    //ReCaptchaV3Service,

    importProvidersFrom(RecaptchaV3Module),
    {
      provide: RECAPTCHA_V3_SITE_KEY,
      useValue: environment.recaptcha.siteKey,
    },
    
    provideHttpClient(withFetch()),
    // { provide: APP_INITIALIZER, useFactory: (config: AppConfiguration) => () => config.load(), deps: [AppConfiguration], multi: true },
    TranslateService,
    DecimalPipe, DatePipe,
    AppState,
    AppService
  ]
};
