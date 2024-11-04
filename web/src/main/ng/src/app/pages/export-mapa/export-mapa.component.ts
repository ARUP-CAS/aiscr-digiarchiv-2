import { SolrDocument } from 'src/app/shared/solr-document';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Params } from '@angular/router';
import { HttpParams } from '@angular/common/http';
import { SolrResponse } from 'src/app/shared/solr-response';
import { AppService } from 'src/app/app.service';
import { AppConfiguration } from 'src/app/app-configuration';
import * as Wkt from 'wicket';

@Component({
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
    this.state.hasError = false;
    this.service.currentLang.subscribe(res => {
      this.setTitle();
      this.ref.detectChanges();
    });
    this.route.queryParams.subscribe(val => {
      this.format = val.format;
      this.search(val);
    });

  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('navbar.desc.logo_desc') + ' | Export');
  }

  getByPath(doc: any, path: string) {
    // let res = path.split('.').reduce(function(o, k) {
    //   return o && o[k];
    // }, doc);
    // return res;
    try {
      return eval('doc.' + path)
    } catch (e: any) {
      return '';
    }
    
  }

  search(params: Params) {
    const p = Object.assign({}, params);
    p.rows = this.config.exportRowsLimit;
    p.mapa = true;
    p.noFacets = true;
    p.noStats = true;

    this.service.search(p as HttpParams).subscribe((resp: SolrResponse) => {
      
      if (resp.error) {
        this.state.loading = false;
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
              this.service.getGeometrie(pian.ident_cely, this.format, p.loc_rpt).subscribe((resp: any) => {
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

  hasRights(r: SolrDocument) {
    return this.state.hasRights(r.pristupnost, r.organizace);
  }

  numFiles(result) {
    if (result.hasOwnProperty('soubor')) {
      return result.soubor.length;
    } else {
      return 0;
    }
  }

  okres(result) {
    if (result.hasOwnProperty('f_okres')) {
      let okresy = [];
      let katastry = [];
      let ret = "";
      for (let idx = 0; idx < result['f_okres'].length; idx++) {
        let okres = result['f_okres'][idx];
        let katastr = result['f_katastr'][idx];

        if (katastry.indexOf(katastr) < 0) {
          okresy.push(okres);
          katastry.push(katastr);
          if (idx > 0) {
            ret += ', ';
          }
          ret += katastr + ' (' + okres + ')';
        }
      }
      return ret;
    } else {
      return "";
    }
  }

  organizace(result) {
    if (result.hasOwnProperty('organizace')) {
      let os = [];
      let ret = "";
      for (let idx = 0; idx < result['organizace'].length; idx++) {
        let o = result['organizace'][idx];
        if (os.indexOf(o) < 0 && o.trim() !== '') {
          os.push(o);

          if (idx > 0) {
            ret += ', ';
          }
          ret += o;
        }

      }
      return ret;
    } else {
      return "";
    }

  }

}
