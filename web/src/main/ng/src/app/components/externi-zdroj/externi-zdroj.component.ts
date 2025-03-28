import { Component, OnInit, Input, Inject, PLATFORM_ID } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { AppService } from 'src/app/app.service';
import { AppConfiguration } from 'src/app/app-configuration';
import { isPlatformBrowser } from '@angular/common';

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

  itemSize = 133;
  vsSize = 0;
  numChildren = 0;

  constructor(
    @Inject(PLATFORM_ID) private platformId: any,
    public state: AppState,
    public service: AppService,
    public config: AppConfiguration
  ) { }

  ngOnInit(): void {
    this.setData();
  }

  checkLoading() {
    this.state.loading = (this.result.akce.length + this.result.lokalita.length + this.result.dokument_cast_projekt.length) < this.numChildren;
  }

  setVsize() {
    this.numChildren = 0;
    if (this.result.ext_zdroj_ext_odkaz) {
      this.numChildren += this.result.ext_zdroj_ext_odkaz.length;
    }

    this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
  }

  getArchZaznam() {
    this.result.akce = [];
    this.result.lokalita = [];
    this.numChildren = this.result.ext_zdroj_ext_odkaz.length;
    this.state.documentProgress = 0;
    if (this.result.ext_zdroj_ext_odkaz) {
      for (let i = 0; i < this.result.ext_zdroj_ext_odkaz.length; i = i + 10) {
        const ids = this.result.ext_zdroj_ext_odkaz.slice(i, i + 10).map((eo: any) => eo.archeologicky_zaznam.id);
        this.service.getIdAsChild(ids, "akce").subscribe((res: any) => {
          this.result.akce = this.result.akce.concat(res.response.docs.filter(d => d.entity === 'akce'));
          this.numChildren = this.numChildren - ids.length + res.response.docs.length;
          this.state.documentProgress = (this.result.akce.length + this.result.lokalita.length) / this.numChildren * 100;
          this.state.loading = (this.result.akce.length + this.result.lokalita.length) < this.numChildren;
        });
        this.service.getIdAsChild(ids, "lokalita").subscribe((res: any) => {
          this.result.lokalita = this.result.lokalita.concat(res.response.docs.filter(d => d.entity === 'lokalita'));
          this.numChildren = this.numChildren - ids.length + res.response.docs.length;
          this.state.documentProgress = (this.result.akce.length + this.result.lokalita.length) / this.numChildren * 100;
          this.state.loading = (this.result.akce.length + this.result.lokalita.length) < this.numChildren;
        });
      }
    }
    
  }

  setData() {
    if (this.inDocument && isPlatformBrowser(this.platformId)) {
      this.setVsize();
      setTimeout(() => {
        this.getArchZaznam();
      }, 1)
      
    }
    const autor = this.result.ext_zdroj_autor ? this.result.ext_zdroj_autor.join(' – ') : '';
    switch (this.result.ext_zdroj_typ) {
      case 'HES-001117': // 'kniha'
        this.bibTex = `@book{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
          author = {${autor}}, 
          title = {${this.result.ext_zdroj_nazev}},
          ${this.result.ext_zdroj_vydavatel ? `publisher = {${this.result.ext_zdroj_vydavatel}},` : ''}
          year = {${this.result.ext_zdroj_rok_vydani_vzniku}},
          ${this.result.ext_zdroj_edice_rada ? `series = {${this.result.ext_zdroj_edice_rada}},` : ''}
          ${this.result.ext_zdroj_misto ? `address = {${this.result.ext_zdroj_misto}},` : ''}
          ${this.result.ext_zdroj_isbn ? `note = ISBN: {${this.result.ext_zdroj_isbn}},` : ''}${this.result.ext_zdroj_issn ? `note = ISSN: {${this.result.ext_zdroj_issn}},` : ''}
          ${this.result.ext_zdroj_link ? `url = {${this.result.ext_zdroj_link}},` : ''}
          ${this.result.ext_zdroj_doi ? `doi = {${this.result.ext_zdroj_doi}}` : ''}
        }`;
        break;
      case 'HES-001118': // 'část knihy':
        this.bibTex = `@inproceedings{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
            author = {${autor}}, 
            title = {${this.result.ext_zdroj_nazev}},
            booktitle = {${this.result.ext_zdroj_sbornik_nazev}},
            year = {${this.result.ext_zdroj_rok_vydani_vzniku}},
            ${this.result.ext_zdroj_editor ? `editor = {${this.result.ext_zdroj_editor}},` : ''}
            ${this.result.ext_zdroj_vydavatel ? `publisher = {${this.result.ext_zdroj_vydavatel}},` : ''}
            ${this.result.ext_zdroj_edice_rada ? `series = {${this.result.ext_zdroj_edice_rada}},` : ''}
            ${this.result.ext_zdroj_misto ? `address = {${this.result.ext_zdroj_misto}},` : ''}
            ${this.result.ext_zdroj_issn ? `note = ISSN: {${this.result.ext_zdroj_issn}},` : ''}${this.result.ext_zdroj_isbn ? `note = ISBN: {${this.result.ext_zdroj_isbn}},` : ''}
            ${this.result.ext_zdroj_link ? `url = {${this.result.ext_zdroj_link}},` : ''}
            ${this.result.ext_zdroj_doi ? `doi = {${this.result.ext_zdroj_doi}},` : ''}
            pages = {${this.result.ext_zdroj_paginace_titulu}}
          }`;
        break;
      case 'HES-001119': // 'článek v časopise':
        this.bibTex = `@article{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
              author = {${autor}},
              title = {${this.result.ext_zdroj_nazev}},
              journal = {${this.result.ext_zdroj_casopis_denik_nazev}},
              year = {${this.result.ext_zdroj_rok_vydani_vzniku}},
              volume = {${this.result.ext_zdroj_casopis_rocnik}},
              ${this.result.ext_zdroj_issn ? `note = ISSN: {${this.result.ext_zdroj_issn}},` : ''}
              ${this.result.ext_zdroj_link ? `url = {${this.result.ext_zdroj_link}},` : ''}
              ${this.result.ext_zdroj_paginace_titulu ? `pages = {${this.result.ext_zdroj_paginace_titulu}}, ` : ''}
              doi = {${this.result.ext_zdroj_doi}}
            }`;
        break;
      case 'HES-001120': // 'článek v novinách':
        this.bibTex = `@article{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
                author = {${autor}},
                title = {${this.result.ext_zdroj_nazev}},
                journal = {${this.result.ext_zdroj_casopis_denik_nazev}},
                year = {${this.result.ext_zdroj_rok_vydani_vzniku}},
                volume = {${this.result.ext_zdroj_datum_rd}},
                ${this.result.ext_zdroj_isbn ? `note = ISSN: {${this.result.ext_zdroj_issn}},` : ''}
                ${this.result.ext_zdroj_link ? `url = {${this.result.ext_zdroj_link}},` : ''}
                ${this.result.ext_zdroj_paginace_titulu ? `pages = {${this.result.ext_zdroj_paginace_titulu}},` : ''}
                doi = {${this.result.ext_zdroj_doi}}
              }`;
        break;
      case 'HES-001121': // 'nepublikovaná zpráva':
        this.bibTex = `@unpublished{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
                  author = {${autor}},
                  title = {${this.result.ext_zdroj_oznaceni}},
                  year = {${this.result.ext_zdroj_rok_vydani_vzniku}},
                  ${this.result.ext_zdroj_link ? `url = {${this.result.ext_zdroj_link}},` : ''}
                  note = {${this.result.ext_zdroj_organizace}},
                  doi = {${this.result.ext_zdroj_doi}}
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

  toggleDetail() {
    this.detailExpanded = !this.detailExpanded;
  }
}
