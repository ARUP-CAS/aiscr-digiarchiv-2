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

  search(params: Params) {
    const p = Object.assign({}, params);
    p.rows = this.config.exportRowsLimit;
    p.mapa = true;
    this.service.search(p as HttpParams).subscribe((resp: SolrResponse) => {
      if (this.state.entity === 'samostatny_nalez' || this.state.entity === 'knihovna_3d') {
        this.docs = resp.response.docs;
        this.hasPian = false;
      } else {
        this.hasPian = true;
        this.docs = [];
        resp.response.docs.forEach(doc => {
          doc.pian.forEach(p => {
            const d = JSON.parse(JSON.stringify(doc));
            d.pian = p;
            this.service.getGeometrie(p.ident_cely, this.format).subscribe((resp: any) => {
              if (this.format === 'GeoJSON') {
                // console.log(ident_cely, resp.geom_wkt_c);
                const wkt = new Wkt.Wkt();
                wkt.read(resp.geometrie);
                d.geometrie = JSON.stringify(wkt.toJson());
              } else {
                d.geometrie = resp.geometrie;
              }
              d.lat = p.centroid_n;
              d.lng = p.centroid_e;
              this.docs.push(d);
            });
          });

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
