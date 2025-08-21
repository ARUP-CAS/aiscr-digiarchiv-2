
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Params, RouterModule } from '@angular/router';
import { HttpParams } from '@angular/common/http';
import 'wicket';
import { DatePipe } from '@angular/common';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { SolrDocument } from '../../shared/solr-document';
import { SolrResponse } from '../../shared/solr-response';

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule,
    MatProgressBarModule, DatePipe
],
  selector: 'app-export-mapa',
  templateUrl: './export-mapa.component.html',
  styleUrls: ['./export-mapa.component.scss']
})
export class ExportMapaComponent implements OnInit {

  docs: any[] = [];
  format: string | undefined;
  hasPian = true;

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

  search(params: Params) {
    const p = Object.assign({}, params);
    p['rows'] = this.config.exportRowsLimit;
    p['mapa'] = true;
    p['isExport'] = true;
    p['noFacets'] = true;
    p['noStats'] = true;
    if (!p['entity']) {
      p['entity'] = 'dokument';
    }
    this.state.loading.set(true);
    this.service.search(p as HttpParams).subscribe((resp: SolrResponse) => {
      
      this.state.loading.set(false);
      if (resp.error) {
        return;
      }
      if (this.state.entity === 'knihovna_3d') {
        this.docs = resp.response.docs;
        this.docs.forEach(doc => {
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
      } else if (this.state.entity === 'samostatny_nalez') {
        this.docs = resp.response.docs;
        this.docs.forEach(doc => {
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
      } else {
        this.hasPian = true;
        this.docs = [];
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
                  this.docs.push(d);
                }
              });
            });
          }

        });
      }

    });
  }

 

}
