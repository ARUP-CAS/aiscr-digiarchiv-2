import { APP_INITIALIZER, ApplicationConfig, inject, PLATFORM_ID, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { HttpClient, provideHttpClient, withFetch } from '@angular/common/http';

import { TranslateService, TranslateModule, TranslateLoader, provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader, TranslateHttpLoader } from '@ngx-translate/http-loader';
import { AppService } from './app.service';
import { AppState } from './app.state';
import { AppConfiguration } from './app-configuration';
import { firstValueFrom, tap } from 'rxjs';
import { Configuration } from './shared/config';
import { DatePipe, DecimalPipe, isPlatformBrowser } from '@angular/common';

// const intializeAppFn = () => {
//   const configService = inject(AppConfiguration);
//   console.log("Initializing app");
//   return configService.load();
// };


export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),

    // provideAppInitializer(() => {
    //   let server = '';
    //   const platformId = inject(PLATFORM_ID);
    //   console.log(platformId)
    //   console.log(!isPlatformBrowser(platformId))
    //   if ('browser' !== platformId) {
    //         const args = process.argv;
    //         console.log('args -> ' + args)
    //         if (args.length > 2 && args[2].startsWith('http')) {
    //             server = args[2];
    //         }

    //     }
    //   const http = inject(HttpClient);
    //   const configService = inject(AppConfiguration);
    //   // return configService.load();
    //   return firstValueFrom(
    //     http
    //       .get(server + "assets/config.json")
    //       .pipe(tap(user => { 
    //         // console.log(user);
    //         configService.config = user as Configuration;
    //       }))
    //   );
    // }),
    provideAppInitializer(() => inject(AppConfiguration).load()),
    provideRouter(routes), 
    provideClientHydration(withEventReplay()),
    
    provideTranslateService({
      loader: provideTranslateHttpLoader({
        prefix: '/assets/i18n/',
        suffix: '.json'
      }),
      fallbackLang: 'en',
      lang: 'en'
    }),

    provideHttpClient(withFetch()),
    // { provide: APP_INITIALIZER, useFactory: (config: AppConfiguration) => () => config.load(), deps: [AppConfiguration], multi: true },
    TranslateService, 
    DecimalPipe, DatePipe,
    AppState, 
    AppService
  ]
};
