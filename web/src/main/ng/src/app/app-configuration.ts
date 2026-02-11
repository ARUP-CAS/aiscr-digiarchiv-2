import { inject, Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap, firstValueFrom } from 'rxjs';
import { Configuration } from './shared/config';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
    providedIn: 'root'
}) export class AppConfiguration {

    public config: Configuration = new Configuration();
    public invalidServer: boolean;

    public obdobi: any;
    public obdobiStats: any;

    
    // public thesauri: { [key: string]: number };
    public get thesauri(): { [key: string]: number } {
        return this.config.thesauri;
    }


    public get context() {
        return this.config.context;
    }

    public get serverUrl() {
        return this.config.serverUrl;
    }

    public get registrationUrl() {
        return this.config.registrationUrl;
    }

    public get accountUrl() {
        return this.config.accountUrl;
    }

    public get restorePassword() {
        return this.config.restorePassword;
    }
    
    public get helpUrl() {
        return this.config.helpUrl;
    }

    public get amcr() {
        return this.config.amcr;
    }

    public get amcr_server() {
        return this.config.amcr_server;
    }

    public get isIndexing() {
        return this.config.isIndexing;
    }

    public get defaultLang() {
        return this.config.defaultLang;
    }

    public get facets() {
        return this.config.facets;
    }

    public get reCaptchaScore() {
        return this.config.reCaptchaScore;
    }

    // public get dateFacets() {
    //     return this.config.dateFacets;
    // }

    // public get numberFacets() {
    //     return this.config.numberFacets;
    // }

    public get mapOptions() {
        return this.config.mapOptions;
    }

    public get hideMenuWidth() {
        return this.config.hideMenuWidth;
    }

    public get home() {
        return this.config.home;
    }

    public get sorts() {
        return this.config.sorts;
    }

    public get selRows() {
        return this.config.selRows;
    }

    public get defaultRows() {
        return this.config.defaultRows;
    }

    public get exportRowsLimit() {
        return this.config.exportRowsLimit;
    }

    public get exportFields() {
        return this.config.exportFields;
    }

    public get urlFields() {
        return this.config.urlFields;
    }

    public get filterFields() {
        return this.config.filterFields;
    }

    public get entityIcons() {
        return this.config.entityIcons;
    }

    public get entities() {
        return this.config.entities;
    }

    public get uiVars() {
        return this.config.uiVars;
    }

    public get choiceApi() {
        return this.config.choiceApi;
    }

    public get feedBackMaxLength() {
        return this.config.feedBackMaxLength;
    }

    public get noPoradiFacets() {
        return this.config.noPoradiFacets;
    }

    public get commonFacets() {
        return this.config.commonFacets;
    }

    server = '';

    constructor(
        @Inject(PLATFORM_ID) private platformId: any
    ) {
        if (!isPlatformBrowser(this.platformId)) {
            const args = process.argv;
            if (args.length > 2 && args[2].startsWith('http')) {
                this.server = args[2];
            }
        }
    }

    public configLoaded() {
        return this.config && true;
    }

    public load() {
        const http = inject(HttpClient);
        console.log('Loading config...')
        return firstValueFrom(http.get(this.server + 'assets/config.json').pipe(
            tap((cfg: any) => {
                this.config = cfg as Configuration;
                this.config.amcr = this.server;
            })
        ));
    }

}
