import { SolrDocument } from 'src/app/shared/solr-document';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Params } from '@angular/router';
import { HttpParams } from '@angular/common/http';
import { SolrResponse } from 'src/app/shared/solr-response';
import { AppService } from 'src/app/app.service';
import { AppConfiguration } from 'src/app/app-configuration';

@Component({
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
      return eval('doc.' + path)
    } catch (e: any) {
      return '';
    }
    
  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('navbar.desc.logo_desc') + ' | Export');
  }

  search(params: Params) {
    const p = Object.assign({}, params);
    p.rows = this.config.exportRowsLimit;
    this.service.search(p as HttpParams).subscribe((resp: SolrResponse) => {
      this.docs = resp.response.docs;

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

  // okres(result) {
  //   if (result.hasOwnProperty('f_okres')) {
  //     let okresy = [];
  //     let katastry = [];
  //     let ret = "";
  //     for (let idx = 0; idx < result['f_okres'].length; idx++) {
  //       let okres = result['f_okres'][idx];
  //       let katastr = result['f_katastr'][idx];

  //       if (katastry.indexOf(katastr) < 0) {
  //         okresy.push(okres);
  //         katastry.push(katastr);
  //         if (idx > 0) {
  //           ret += ', ';
  //         }
  //         ret += katastr + ' (' + okres + ')';
  //       }
  //     }
  //     return ret;
  //   } else {
  //     return "";
  //   }
  // }

  // organizace(result) {
  //   if (result.hasOwnProperty('organizace')) {
  //     let os = [];
  //     let ret = "";
  //     for (let idx = 0; idx < result['organizace'].length; idx++) {
  //       let o = result['organizace'][idx];
  //       if (os.indexOf(o) < 0 && o.trim() !== '') {
  //         os.push(o);

  //         if (idx > 0) {
  //           ret += ', ';
  //         }
  //         ret += o;
  //       }

  //     }
  //     return ret;
  //   } else {
  //     return "";
  //   }

  // }

}
