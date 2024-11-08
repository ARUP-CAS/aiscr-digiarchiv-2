import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, of, tap, lastValueFrom, map, switchMap, catchError } from 'rxjs';
import { Configuration } from './shared/config';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
    providedIn: 'root'
}) export class AppConfiguration {

    public config: Configuration;
    public invalidServer: boolean;

    public obdobi;
    public obdobiStats;
    public thesauri: { [key: string]: number };

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

    public load() {
        return this.http.get(this.server + 'assets/config.json').pipe(
            switchMap((cfg: any) => {
                this.config = cfg as Configuration;
                this.config.amcr = this.server;
                return this.http.get(this.server + 'api/search/thesauri').pipe(tap((res: any) => {
                    // this.obdobi = res.response.docs;
                    // this.obdobiStats = res.stats.stats_fields.poradi;
                    this.thesauri = res;
                }));
            }),
            catchError((err) => {
                // this.alertSubject.next(err);
                return of(err);
            })
        );
    }

    // public loadOld(): Promise<any> {
    //     // console.log('loading config ...');
    //     const promise = this.http.get(this.server + 'assets/config.json')
    //         .toPromise()
    //         .then(cfg => {
    //             this.config = cfg as Configuration;
    //             this.config.amcr = this.server;
    //             // }).then(() => {
    //             //     return this.getObdobi();
    //         }).then(() => {
    //             return this.getThesauri();
    //         });
    //     return promise;
    // }

    // getObdobi() {
    //     const url = this.server + 'api/search/obdobi';
    //     return this.http.get(url)
    //         .toPromise()
    //         .then((res: any) => {
    //             this.obdobi = res.response.docs;
    //             this.obdobiStats = res.stats.stats_fields.poradi;
    //         });
    // }

    getThesauri() {
        const url = this.server + 'api/search/thesauri';
        return this.http.get(url)
            .toPromise()
            .then((res: any) => {
                this.thesauri = res;
            });
    }

}
