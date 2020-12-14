import { SolrDocument } from 'src/app/shared/solr-document';
import { Component, OnInit } from '@angular/core';
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
  pas: any[] = [];

  fields = {
    dokument: [
      {name: 'ident_cely'},
      {name: 'f_autor'},
      {name: 'rok_vzniku'},
      {name: 'f_organizace', heslar: 'organizace'},
      {name: 'typ_dokumentu'},
      {name: 'material_originalu'},
      {name: 'f_popis'},
      {name: 'f_poznamka'},
      {name: 'extra_data_cislo_objektu'},
      {name: 'f_jazyk_dokumentu'},
      {name: 'oznaceni_originalu'},
      {name: 'f_ulozeni_originalu'},
      {name: 'f_okres'},
      {name: 'files'},
     { name: 'pristupnost'},
    ],
    knihovna_3d: [
      {name: 'ident_cely'},
      {name: 'f_autor'},
      {name: 'rok_vzniku'},
      {name: 'f_organizace', heslar: 'organizace'},
      {name: 'typ_dokumentu'},
      {name: 'material_originalu'},
      {name: 'f_popis'},
      {name: 'f_poznamka'},
      {name: 'extra_data_cislo_objektu'},
      {name: 'f_jazyk_dokumentu'},
      {name: 'oznaceni_originalu'},
      {name: 'f_ulozeni_originalu'},
      {name: 'f_okres'},
      {name: 'files'},
     { name: 'pristupnost'},
    ],
    akce: [
      {name: 'ident_cely'},
      {name: 'f_katastr', secured: true},
      {name: 'f_okres'},
      {name: 'vedouci_akce'},
      {name: 'specifikace_data'},
      {name: 'datum_zahajeni'},
      {name: 'datum_ukonceni'},
      {name: 'lokalizace', secured: true},
      {name: 'je_nz'},
     { name: 'pristupnost'},
      {name: 'f_organizace', heslar: 'organizace'},
      {name: 'dalsi_katastry', secured: true}
    ],
    lokalita: [
      {name: 'ident_cely'},
      {name: 'f_katastr', secured: true},
      {name: 'f_okres'},
      {name: 'nazev', secured: true},
      {name: 'f_typ_lokality'},
      {name: 'druh'},
     { name: 'pristupnost'},
      {name: 'dalsi_katastry', secured: true},
      {name: 'f_popis', secured: true},
    ],
    projekt: [
      {name: 'ident_cely'},
      {name: 'f_katastr'},
      {name: 'f_okres'},
      {name: 'vedouci_projektu'},
      {name: 'f_typ_projektu'},
      {name: 'datum_zahajeni'},
      {name: 'datum_ukonceni'},
      {name: 'organizace_prihlaseni', heslar: 'organizace'},
      {name: 'dalsi_katastry'},
      {name: 'podnet'},
    ],
    samostatny_nalez: [
      {name: 'ident_cely'},
      {name: 'f_nalezce'},
      {name: 'datum_nalezu'},
      {name: 'predano_organizace', heslar: 'organizace'},
      {name: 'inv_cislo', heslar: 'pas'},
      {name: 'obdobi'},
      {name: 'druh'},
      {name: 'f_specifikace'},
      {name: 'f_katastr', secured: true},
      {name: 'f_okres'},
      {name: 'pocet'},
      {name: 'nalezove_okolnosti'},
      {name: 'lokalizace', secured: true},
      {name: 'hloubka'},
      {name: 'f_poznamka'},
     { name: 'pristupnost'},
    ]
  };

  constructor(
    private titleService: Title,
    private route: ActivatedRoute,
    private config: AppConfiguration,
    public state: AppState,
    private service: AppService
  ) {
    this.state.bodyClass = 'app-page-export';
  }


  ngOnInit(): void {
    this.service.currentLang.subscribe(res => {
      this.setTitle();
    });
    this.route.queryParams.subscribe(val => {
      this.search(val);
    });

  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('logo_desc') + ' | Export');
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
