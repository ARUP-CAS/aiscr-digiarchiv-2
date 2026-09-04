
import { ChangeDetectorRef, Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Params, Router, RouterModule } from '@angular/router';
import { HttpParams } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';

import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { Sort } from '../../shared/config';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { CommonModule, DatePipe, isPlatformBrowser } from '@angular/common';
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { AppWindowRef } from '../../app.window-ref';
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

@Component({
  imports: [
    TranslateModule, RouterModule, CommonModule, FormsModule,
    MatProgressBarModule, DatePipe, MatTooltipModule,
    MatPaginatorModule, MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule
],
  selector: 'app-export',
  templateUrl: './export.component.html',
  styleUrls: ['./export.component.scss']
})
export class ExportComponent implements OnInit {

  docs: any[] = [];
  pageIndex: number = 0;
  rows: number;
  page = 0;
  sort: Sort;
  numFound: number;

  constructor(
    @Inject(PLATFORM_ID) private platformId: any,
    private windowRef: AppWindowRef,
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
    
    //this.pageIndex = this.route.snapshot.queryParams['page'] ? this.route.snapshot.queryParams['page'] : 0;
    this.pageIndex = this.state.page + 1;
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
    // this.state.pageChanged = true;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  setPage() {
    const params: any = {};
    params.page = this.pageIndex - 1;
    this.page = this.pageIndex - 1;
    // document.getElementById('scroll-wrapper').scrollTop = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  sortBy(sort: Sort) {
      this.sort = sort;
      this.router.navigate([], { queryParams: { sort: sort.field, page: 0 }, queryParamsHandling: 'merge' });
    }

  search(params: Params) {
    this.state.loading.set(true);
    const p: any = Object.assign({}, params);
    this.page = params['page'] ? +params['page'] : 0;

    if (params['sort']) {
      this.sort = this.state.sorts_by_entity.find(s => (s.field) === params['sort']);
    } else if (this.sort) {
      // this.sort could be from another entity. Check validity
      this.sort = this.state.sorts_by_entity.find(s => s.field === this.sort.field);
    }
    if (!this.sort) {
      this.sort = this.state.sorts_by_entity[0];
    }

    p.sort = this.sort.field;
    console.log(p.sort)


    if (p['rows']) {
      p.rows = p['rows'];
    } else {
      p.rows = this.config.exportRowsLimit;
    }
    this.rows = p.rows;
    
    if (!p['entity']) {
      p['entity'] = 'dokument';
    }
    this.service.export(p as HttpParams).subscribe((resp: any) => {
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

  downloadFormat(format: string) {
    const s = this.config.context + 'api/exp' +  document.location.search + '&format=' + format; 
    if (isPlatformBrowser(this.platformId)) {
      const link = this.windowRef.nativeWindow.document.createElement('a');
      link.href = s;
      link.download = 'export.' + format;
      link.click();
      this.service.showInfoDialog(this.service.getTranslation('dialog.desc.export_started'), 2000);
    }
  }


}
