
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Params, RouterModule } from '@angular/router';
import { HttpParams } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';

import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { SolrResponse } from '../../shared/solr-response';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { DatePipe } from '@angular/common';

@Component({
  imports: [
    TranslateModule, RouterModule, 
    MatProgressBarModule, DatePipe
],
  selector: 'app-export',
  templateUrl: './export.component.html',
  styleUrls: ['./export.component.scss']
})
export class ExportComponent implements OnInit {

  docs: any[] = [];

  constructor(
    private ref: ChangeDetectorRef,
    private titleService: Title,
    private route: ActivatedRoute,
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
    this.route.queryParams.subscribe(val => {
      this.search(val);
    });

  }

  getByPath(doc: any, path: string) {
    // let res = path.split('.').reduce(function(o, k) {
    //   return o && o[k];
    // }, doc);
    // return res;
    try {
      return window.eval('doc.' + path)
    } catch (e: any) {
      return '';
    }
    
  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('navbar.desc.logo_desc') 
    + ' | ' + this.service.getTranslation('title.export') 
    + ' - ' + this.service.getTranslation('entities.'+ this.state.entity+'.title') );
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
