
import { ChangeDetectorRef, Component, Inject, OnInit, PLATFORM_ID, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Params, Router, RouterModule } from '@angular/router';
import { HttpParams } from '@angular/common/http';

import * as Wkt from 'wicket';
import { CommonModule, DatePipe, isPlatformBrowser } from '@angular/common';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TranslateModule } from '@ngx-translate/core';

import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { SolrResponse } from '../../shared/solr-response';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { Sort } from '../../shared/config';
import { AppWindowRef } from '../../app.window-ref';

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
  selector: 'app-export-mapa',
  templateUrl: './export-mapa.component.html',
  styleUrls: ['./export-mapa.component.scss']
})
export class ExportMapaComponent implements OnInit {

  docs = signal<any[]>([]);
  pageIndex: number = 0;
  rows: number;
  page = 0;
  sort: Sort;
  numFound: number;
  format: string | undefined;
  hasPian = true;

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
    this.route.queryParams.subscribe(val => {
      this.format = val['format'];
      this.search(val);
    });

  }

  setTitle() {
      this.titleService.setTitle(this.service.getTranslation('navbar.desc.logo_desc') 
      + ' | ' + this.service.getTranslation('title.export-mapa') 
      + ' - ' + this.service.getTranslation('entities.'+ this.state.entity+'.title') );
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
    const p:any = Object.assign({}, params);

    
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

    
    p['mapa'] = true;
    p['isExport'] = true;
    p['noFacets'] = true;
    p['noStats'] = true;
    if (!p['entity'] && !p['id']) {
      p['entity'] = 'dokument';
    }
    this.state.loading.set(true);
    this.service.searchExportMapa(p as HttpParams).subscribe((resp: SolrResponse) => {
      let docs: any[] = resp.response.docs;
      this.state.loading.set(false);
      if (resp.error) {
        return;
      }
      if (this.state.entity === 'knihovna_3d') {
        docs.forEach(doc => {
          if (this.format === 'GeoJSON') {
            // console.log(ident_cely, resp.geom_wkt_c);
            const wkt = new Wkt.Wkt();
            wkt.read(doc.dokument_extra_data.geom_wkt.value);
            doc.geometrie = JSON.stringify(wkt.toJson());
          } else if (this.format === 'GML') {
            doc.geometrie = doc.dokument_extra_data.geom_gml;
          } else {
            doc.geometrie = doc.dokument_extra_data.geom_wkt.value;
          }
        });
        this.hasPian = false;
        this.docs.update(d => [...docs]);
      } else if (this.state.entity === 'samostatny_nalez') {
        docs.forEach(doc => {
          if (this.format === 'GeoJSON') {
            // console.log(ident_cely, resp.geom_wkt_c);
            const wkt = new Wkt.Wkt();
            wkt.read(doc.samostatny_nalez_chranene_udaje.geom_wkt.value);
            doc.geometrie = JSON.stringify(wkt.toJson());
          } else if (this.format === 'GML') {
            doc.geometrie = doc.samostatny_nalez_chranene_udaje.geom_gml;
          } else {
            doc.geometrie = doc.samostatny_nalez_chranene_udaje.geom_wkt.value;
          }
        });
        this.hasPian = false;
        this.docs.update(d => [...docs]);
      } else if (p['id'] && docs[0]?.entity === 'pian') {
        this.state.entity = 'pian';
        docs.forEach(doc => {
          doc.pian = doc;
          if (this.format === 'GeoJSON') {
            // console.log(ident_cely, resp.geom_wkt_c);
            const wkt = new Wkt.Wkt();
            wkt.read(doc.pian_chranene_udaje.geom_wkt.value);
            doc.geometrie = JSON.stringify(wkt.toJson());
          } else if (this.format === 'GML') {
            doc.geometrie = doc.pian_chranene_udaje.geom_gml;
          } else {
            doc.geometrie = doc.pian_chranene_udaje.geom_wkt.value;
          }
        });
        this.hasPian = true;
        this.docs.update(d => [...docs]);
      } else {
        docs = [];
        this.hasPian = true;
        resp.response.docs.forEach(doc => {
          if(doc.pian) {
            
            doc.pian.forEach(pian => {
              const d = JSON.parse(JSON.stringify(doc));
              d.pian = pian;
              this.service.getGeometrie(pian.ident_cely, this.format, p['loc_rpt']).subscribe((resp: any) => {
                if (resp.geometrie) {
                  if (this.format === 'GeoJSON') {
                    // console.log(ident_cely, resp.geom_wkt_c);
                    const wkt = new Wkt.Wkt();
                    wkt.read(resp.geometrie);
                    d.geometrie = JSON.stringify(wkt.toJson());
                  } else {
                    d.geometrie = resp.geometrie;
                  }
                  // d.lat = p.centroid_n;
                  // d.lng = p.centroid_e;
                  docs.push(d);
                  this.docs.update(d => [...docs]);
                }
              });
            });
          }

        });
      }

    });
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
