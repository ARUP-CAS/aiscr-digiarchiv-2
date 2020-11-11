import { Component, OnInit, Input } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { AppService } from 'src/app/app.service';

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
    public service: AppService
  ) { }

  ngOnInit(): void {

    const autor = this.result.autori ? this.result.autori.split(';').join(' – ') : '';
    switch (this.result.typ) {
      case 'kniha':
        this.bibTex = `@book{
          https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
          author = “${autor}”, 
          title = “${this.result.nazev} ${this.result.podnazev ? this.result.podnazev : ''} ”,
          ${this.result.vydavatel ? `publisher = “${this.result.vydavatel}”,` : ''}
          year = ${this.result.rok_vydani_vzniku},
          ${this.result.edice_rada ? `series = “${this.result.edice_rada}”,` : ''}
          address = “${this.result.misto}”,
          ${this.result.link ? `url = “${this.result.link}”,` : ''}
          note = “ISBN: ${this.result.isbn}”
        }`;
        break;
      case 'část knihy':
        this.bibTex = `@inproceedings{
            https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
            author = “${autor}”, 
            title = “${this.result.nazev} ${this.result.podnazev ? this.result.podnazev : ''} ”,
            booktitle = “${this.result.sbornik_nazev} ”,
            year = ${this.result.rok_vydani_vzniku},
            editor = ${this.result.sbornik_editor},
            ${this.result.vydavatel ? `publisher = “${this.result.vydavatel}”,` : ''}
            ${this.result.edice_rada ? `series = “${this.result.edice_rada}”,` : ''}
            address = “${this.result.misto}”,
            note = “ISSN: ${this.result.isbn}”,
            ${this.result.link ? `url = “${this.result.link}”,` : ''}
            pages = “${this.result.paginace_titulu}”
          }`;
        break;
      case 'článek v časopise':
        this.bibTex = `@article{
              https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
              author = “${autor}”,
              title = “${this.result.nazev} ${this.result.podnazev ? this.result.podnazev : ''} ”,
              journal = “${this.result.casopis_denik_nazev} ”,
              year = ${this.result.rok_vydani_vzniku},
              volume = “${this.result.casopis_rocnik}”,
              note = “ISSN: ${this.result.issn}”,
              ${this.result.link ? `url = “${this.result.link}”,` : ''}
              ${this.result.paginace_titulu ? `pages = “${this.result.paginace_titulu}”` : ''}
            }`;
        break;
      case 'článek v novinách':
        this.bibTex = `@article{
                https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
                author = “${autor}”,
                title = “${this.result.nazev} ${this.result.podnazev ? this.result.podnazev : ''} ”,
                journal = “${this.result.casopis_denik_nazev} ”,
                year = ${this.result.rok_vydani_vzniku},
                volume = “${this.result.datum_rd}”,
                ${this.result.link ? `url = “${this.result.link}”,` : ''}
                ${this.result.paginace_titulu ? `pages = “${this.result.paginace_titulu}”` : ''}
              }`;
        break;
      case 'nepublikovaná zpráva':
        this.bibTex = `@unpublished{
                  https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
                  author = “${autor}”,
                  title = “${this.result.oznaceni} ”,
                  year = ${this.result.rok_vydani_vzniku},
                  ${this.result.link ? `url = “${this.result.link}”,` : ''}
                  note = “${this.result.organizace}”
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
