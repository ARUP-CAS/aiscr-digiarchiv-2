
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Params, Router, RouterModule } from '@angular/router';
import { HttpParams } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';

import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { SolrResponse } from '../../shared/solr-response';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { DatePipe } from '@angular/common';
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";

@Component({
  imports: [
    TranslateModule, RouterModule,
    MatProgressBarModule, DatePipe,
    MatPaginatorModule
  ],
  selector: 'app-export',
  templateUrl: './export.component.html',
  styleUrls: ['./export.component.scss']
})
export class ExportComponent implements OnInit {

  docs: any[] = [];
  pageIndex: number = 0;
  numFound: number;

  constructor(
    private ref: ChangeDetectorRef,
    private titleService: Title,
    private route: ActivatedRoute,
    private router: Router,
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService
  ) {
    this.state.bodyClass = 'app-page-export';
  }


  ngOnInit(): void {
    this.setTitle();
    this.state.hasError = false;
    this.service.currentLang.subscribe(res => {
      this.setTitle();
      this.ref.detectChanges();
    });
    
    this.pageIndex = this.route.snapshot.queryParams['page'] ? this.route.snapshot.queryParams['page'] : 0;
    this.route.queryParams.subscribe(val => {
      this.search(val);
    });

  }

  deepFind(obj: any, path: string) {
    let paths = path.split('.')
      , current = obj
      , i;

    for (i = 0; i < paths.length; ++i) {
      // console.log(paths[i], current[paths[i]]);
      if (current[paths[i]] === undefined) {
        return undefined;
      } else {
        current = current[paths[i]];
      }
    }
    return current;
  }

  getByPath(doc: any, path: string, map: boolean) {
    try {
      //return eval('doc.' + path);
      const o = this.deepFind(doc, path);
      if (map && o) {
        const m = o.map((dk: any) => dk.value).join(', ');
        return m
      } else {
        return o
      }

    } catch (e: any) {
      console.log(e)
      return '';
    }

  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('navbar.desc.logo_desc')
      + ' | ' + this.service.getTranslation('title.export')
      + ' - ' + this.service.getTranslation('entities.' + this.state.entity + '.title'));
  }

  pageChanged(e: PageEvent) {
    const params: any = {};
    params.rows = e.pageSize;
    params.page = e.pageIndex;
    this.pageIndex = e.pageIndex + 1;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  search(params: Params) {
    this.state.loading.set(true);
    const p: any = Object.assign({}, params);
    p.rows = this.config.exportRowsLimit;
    if (!p['entity']) {
      p['entity'] = 'dokument';
    }
    this.service.search(p as HttpParams).subscribe((resp: SolrResponse) => {
      this.state.loading.set(false);
      if (resp.error) {
        return;
      }
      this.docs = resp.response.docs;
      this.numFound = resp.response.numFound;
    });
  }

  numFiles(result: any) {
    if (result.hasOwnProperty('soubor')) {
      return result.soubor.length;
    } else {
      return 0;
    }
  }


}
