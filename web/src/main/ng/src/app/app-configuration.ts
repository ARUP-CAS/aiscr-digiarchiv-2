import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { Configuration } from './shared/config';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
    providedIn: 'root'
}) export class AppConfiguration {

    public config: Configuration;
    public invalidServer: boolean;

    public obdobi;
    public obdobiStats;
    public thesauri: {[key: string]: number};

    public get context() {
        return this.config.context;
    }

    public get serverUrl() {
        return this.config.serverUrl;
    }

    public get amcr() {
        return this.config.amcr;
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

    public get poleToHeslar() {
        return this.config.poleToHeslar;
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

    

    /**
     * List the files holding section configuration in assets/configs folder
     * ['search'] will look for /assets/configs/search.json
     */
    private configs: string[] = [];

    server = '';

    constructor(
        @Inject(PLATFORM_ID) private platformId: any,
        private http: HttpClient) {
            if (!isPlatformBrowser(this.platformId)) {
                const args = process.argv;
                if (args.length > 2) {
                    this.server = args[2];
                }
            }
        }

    public configLoaded() {
        return this.config && true;
    }

    public load(): Promise<any> {
        // console.log('loading config ...');
        const promise = this.http.get(this.server + 'assets/config.json')
            .toPromise()
            .then(cfg => {
                this.config = cfg as Configuration;
                this.config.amcr = this.server;
            }).then(() => {
                return this.getObdobi();
            }).then(() => {
                return this.getThesauri();
            });
        return promise;
    }

    getObdobi() {
        const url = this.server + 'api/search/obdobi';
        return this.http.get(url)
            .toPromise()
            .then((res: any) => {
                this.obdobi = res.response.docs;
                this.obdobiStats = res.stats.stats_fields.poradi;
            });
    }

    getThesauri() {
        const url = this.server + 'api/search/thesauri';
        return this.http.get(url)
            .toPromise()
            .then((res: any) => {
                this.thesauri = res;
            });
    }

    mergeFile(url: string): Promise<any> {

        return new Promise((resolve, reject) => {
            this.http.get(url)
                .subscribe(
                    res => {
                        resolve(res);
                    },
                    error => {
                        resolve(false);
                        return of(url + ' not found');
                    }
                );
        });
    }

}
