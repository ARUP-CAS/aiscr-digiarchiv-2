import { Component, OnInit, Input } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { AppService } from 'src/app/app.service';
import { AppConfiguration } from 'src/app/app-configuration';

@Component({
  selector: 'app-externi-zdroj',
  templateUrl: './externi-zdroj.component.html',
  styleUrls: ['./externi-zdroj.component.scss']
})
export class ExterniZdrojComponent implements OnInit {
  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() inDocument = false;
  @Input() isDocumentDialogOpen: boolean;
  bibTex: string;

  constructor(
    public state: AppState,
    public service: AppService,
    public config: AppConfiguration
  ) { }

  ngOnInit(): void {
    this.setData();

    // const id = this.result.ident_cely ? this.result.ident_cely : this.result;

      // this.service.getIdAsChild([id], "ext_zdroj").subscribe((res: any) => {
      //   this.result = res.response.docs[0]; 
      //   this.setData();
      // });

    }

  setData(){  
    const autor = this.result.ext_zdroj_autor ? this.result.ext_zdroj_autor.join(' – ') : '';
    switch (this.result.ext_zdroj_typ) {
      case 'HES-001117': // 'kniha'
        this.bibTex = `@book{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
          author = {${autor}}, 
          title = {${this.result.ext_zdroj_nazev} ${this.result.ext_zdroj_podnazev ? this.result.ext_zdroj_podnazev : ''}},
          ${this.result.ext_zdroj_vydavatel ? `publisher = {${this.result.ext_zdroj_vydavatel}},` : ''}
          year = {${this.result.ext_zdroj_rok_vydani_vzniku}},
          ${this.result.ext_zdroj_edice_rada ? `series = {${this.result.ext_zdroj_edice_rada}},` : ''}
          ${this.result.ext_zdroj_misto ? `address = {${this.result.ext_zdroj_misto}},` : ''}
          ${this.result.ext_zdroj_isbn ? `note = ISBN: {${this.result.ext_zdroj_isbn}},` : ''}
          ${this.result.ext_zdroj_issn ? `note = ISSN: {${this.result.ext_zdroj_issn}},` : ''}
          ${this.result.ext_zdroj_link ? `url = {${this.result.ext_zdroj_link}},` : ''}
        }`;
        break;
      case 'HES-001118': // 'část knihy':
        this.bibTex = `@inproceedings{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
            author = {${autor}}, 
            title = {${this.result.ext_zdroj_nazev} ${this.result.ext_zdroj_podnazev ? this.result.ext_zdroj_podnazev : ''}},
            booktitle = {${this.result.ext_zdroj_sbornik_nazev}},
            year = {${this.result.ext_zdroj_rok_vydani_vzniku}},
            ${this.result.ext_zdroj_sbornik_editor ? `editor = {${this.result.ext_zdroj_sbornik_editor}},` : ''}
            ${this.result.ext_zdroj_vydavatel ? `publisher = {${this.result.ext_zdroj_vydavatel}},` : ''}
            ${this.result.ext_zdroj_edice_rada ? `series = {${this.result.ext_zdroj_edice_rada}},` : ''}
            ${this.result.ext_zdroj_misto ? `address = {${this.result.ext_zdroj_misto}},` : ''}
            ${this.result.ext_zdroj_issn ? `note = ISSN: {${this.result.ext_zdroj_issn}},` : ''}
            ${this.result.ext_zdroj_isbn ? `note = ISBN: {${this.result.ext_zdroj_isbn}},` : ''}
            ${this.result.ext_zdroj_link ? `url = {${this.result.ext_zdroj_link}},` : ''}
            pages = {${this.result.ext_zdroj_paginace_titulu}}
          }`;
        break;
      case 'HES-001119': // 'článek v časopise':
        this.bibTex = `@article{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
              author = {${autor}},
              title = {${this.result.ext_zdroj_nazev} ${this.result.ext_zdroj_podnazev ? this.result.ext_zdroj_podnazev : ''}},
              journal = {${this.result.ext_zdroj_casopis_denik_nazev}},
              year = {${this.result.ext_zdroj_rok_vydani_vzniku}},
              volume = {${this.result.ext_zdroj_casopis_rocnik}},
              ${this.result.ext_zdroj_issn ? `note = ISSN: {${this.result.ext_zdroj_issn}},` : ''}
              ${this.result.ext_zdroj_link ? `url = {${this.result.ext_zdroj_link}},` : ''}
              ${this.result.ext_zdroj_paginace_titulu ? `pages = {${this.result.ext_zdroj_paginace_titulu}}` : ''}
            }`;
        break;
      case 'HES-001120': // 'článek v novinách':
        this.bibTex = `@article{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
                author = {${autor}},
                title = {${this.result.ext_zdroj_nazev} ${this.result.ext_zdroj_podnazev ? this.result.ext_zdroj_podnazev : ''}},
                journal = {${this.result.ext_zdroj_casopis_denik_nazev}},
                year = {${this.result.ext_zdroj_rok_vydani_vzniku}},
                volume = {${this.result.ext_zdroj_datum_rd}},
                ${this.result.ext_zdroj_isbn ? `note = ISSN: {${this.result.ext_zdroj_issn}},` : ''}
                ${this.result.ext_zdroj_link ? `url = {${this.result.ext_zdroj_link}},` : ''}
                ${this.result.ext_zdroj_paginace_titulu ? `pages = {${this.result.ext_zdroj_paginace_titulu}},` : ''}
              }`;
        break;
      case 'HES-001121': // 'nepublikovaná zpráva':
        this.bibTex = `@unpublished{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
                  author = {${autor}},
                  title = {${this.result.ext_zdroj_oznaceni}},
                  year = {${this.result.ext_zdroj_rok_vydani_vzniku}},
                  ${this.result.ext_zdroj_link ? `url = {${this.result.ext_zdroj_link}},` : ''}
                  note = {${this.result.ext_zdroj_organizace}}
                }`;
        break;
    }
  }

  toggleFav() {
    if (this.result.isFav) {
      this.service.removeFav(this.result.ident_cely).subscribe(res => {
        this.result.isFav = false;
      });
    } else {
      this.service.addFav(this.result.ident_cely).subscribe(res => {
        this.result.isFav = true;
      });
    }
  }
}
