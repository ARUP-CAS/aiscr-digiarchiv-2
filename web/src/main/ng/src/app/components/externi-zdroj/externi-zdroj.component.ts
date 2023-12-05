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
    const id = this.result.ident_cely ? this.result.ident_cely : this.result;

      // this.service.getIdAsChild([id], "ext_zdroj").subscribe((res: any) => {
      //   this.result = res.response.docs[0]; 
      //   this.setData();
      // });

    }

  setData(){  
    const autor = this.result.autor ? this.result.autor.join(' – ') : '';
    switch (this.result.typ) {
      case 'kniha':
        this.bibTex = `@book{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
          author = {${autor}}, 
          title = {${this.result.nazev} ${this.result.podnazev ? this.result.podnazev : ''}},
          ${this.result.vydavatel ? `publisher = {${this.result.vydavatel}},` : ''}
          year = {${this.result.rok_vydani_vzniku}},
          ${this.result.edice_rada ? `series = {${this.result.edice_rada}},` : ''}
          ${this.result.link ? `url = {${this.result.link}},` : ''}
          ${this.result.isbn ? `isbn = {${this.result.isbn}},` : ''}
          ${this.result.issn ? `issn = {${this.result.issn}},` : ''}
          address = {${this.result.misto}}
        }`;
        break;
      case 'část knihy':
        this.bibTex = `@inproceedings{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
            author = {${autor}}, 
            title = {${this.result.nazev} ${this.result.podnazev ? this.result.podnazev : ''}},
            booktitle = {${this.result.sbornik_nazev}},
            year = {${this.result.rok_vydani_vzniku}},
            editor = {${this.result.sbornik_editor}},
            ${this.result.vydavatel ? `publisher = {${this.result.vydavatel}},` : ''}
            ${this.result.edice_rada ? `series = {${this.result.edice_rada}},` : ''}
            address = {${this.result.misto}},
            ${this.result.issn ? `issn = {${this.result.issn}},` : ''}
            ${this.result.isbn ? `isbn = {${this.result.isbn}},` : ''}
            ${this.result.link ? `url = {${this.result.link}},` : ''}
            pages = {${this.result.paginace_titulu}}
          }`;
        break;
      case 'článek v časopise':
        this.bibTex = `@article{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
              author = {${autor}},
              title = {${this.result.nazev} ${this.result.podnazev ? this.result.podnazev : ''}},
              journal = {${this.result.casopis_denik_nazev}},
              year = {${this.result.rok_vydani_vzniku}},
              ${this.result.issn ? `issn = {${this.result.issn}},` : ''}
              ${this.result.link ? `url = {${this.result.link}},` : ''}
              ${this.result.paginace_titulu ? `pages = {${this.result.paginace_titulu}},` : ''}
              volume = {${this.result.casopis_rocnik}}
            }`;
        break;
      case 'článek v novinách':
        this.bibTex = `@article{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
                author = {${autor}},
                title = {${this.result.nazev} ${this.result.podnazev ? this.result.podnazev : ''}},
                journal = {${this.result.casopis_denik_nazev}},
                year = {${this.result.rok_vydani_vzniku}},
                ${this.result.link ? `url = {${this.result.link}},` : ''}
                ${this.result.paginace_titulu ? `pages = {${this.result.paginace_titulu}},` : ''}
                volume = {${this.result.datum_rd}}
              }`;
        break;
      case 'nepublikovaná zpráva':
        this.bibTex = `@unpublished{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
                  author = {${autor}},
                  title = {${this.result.oznaceni}},
                  year = {{this.result.rok_vydani_vzniku}},
                  ${this.result.link ? `url = {${this.result.link}},` : ''}
                  publisher = {${this.result.organizace}}
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
