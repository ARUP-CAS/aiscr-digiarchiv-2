import { Component, OnInit, Inject, PLATFORM_ID, forwardRef, input, effect } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { InlineFilterComponent } from '../../components/inline-filter/inline-filter.component';
import { ResultActionsComponent } from '../../components/result-actions/result-actions.component';
import { AkceComponent } from '../akce/akce.component';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { LokalitaComponent } from "../lokalita/lokalita.component";

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule, CommonModule,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    InlineFilterComponent, MatButtonModule, ScrollingModule,
    ResultActionsComponent,
    forwardRef(() => AkceComponent),
    forwardRef(() => LokalitaComponent)
  ],
  selector: 'app-externi-zdroj',
  templateUrl: './externi-zdroj.component.html',
  styleUrls: ['./externi-zdroj.component.scss']
})
export class ExterniZdrojComponent implements OnInit {
  _result: any;
  readonly result = input<any>();
  _detailExpanded: boolean;
  readonly detailExpanded = input<boolean>();
  readonly isChild = input<boolean>();
  readonly inDocument = input(false);
  readonly isDocumentDialogOpen = input<boolean>();
  bibTex: string;

  itemSize = 133;
  vsSize = 0;
  numChildren = 0;

  constructor(
    @Inject(PLATFORM_ID) private platformId: any,
    public state: AppState,
    public service: AppService,
    public config: AppConfiguration
  ) {
    effect(() => {
      this._detailExpanded = this.detailExpanded();
      this._result = this.result();

      if (this._result) {
        this.setData();
      }
    });

  }

  ngOnInit(): void {
    this.setData();
  }

  checkLoading() {
    this.state.loading.set((this.result().akce.length + this.result().lokalita.length + this.result().dokument_cast_projekt.length) < this.numChildren);
  }

  setVsize() {
    this.numChildren = 0;
    this._result = this.result();
    if (this._result.ext_zdroj_ext_odkaz) {
      this.numChildren += this._result.ext_zdroj_ext_odkaz.length;
    }

    this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
  }

  getArchZaznam() {
    this._result.akce = [];
    this._result.lokalita = [];
    this.numChildren = this._result.ext_zdroj_ext_odkaz.length;
    this.state.documentProgress = 0;
    if (this._result.ext_zdroj_ext_odkaz) {
      for (let i = 0; i < this._result.ext_zdroj_ext_odkaz.length; i = i + 10) {
        const ids = this._result.ext_zdroj_ext_odkaz.slice(i, i + 10).map((eo: any) => eo.archeologicky_zaznam.id);
        this.service.getIdAsChild(ids, "akce").subscribe((res: any) => {
          this._result.akce = this._result.akce.concat(res.response.docs.filter((d: { entity: string; }) => d.entity === 'akce'));
          this.numChildren = this.numChildren - ids.length + res.response.docs.length;
          this.state.documentProgress = (this._result.akce.length + this._result.lokalita.length) / this.numChildren * 100;
          this.state.loading.set((this._result.akce.length + this._result.lokalita.length) < this.numChildren);
        });
        this.service.getIdAsChild(ids, "lokalita").subscribe((res: any) => {
          this._result.lokalita = this._result.lokalita.concat(res.response.docs.filter((d: { entity: string; }) => d.entity === 'lokalita'));
          this.numChildren = this.numChildren - ids.length + res.response.docs.length;
          this.state.documentProgress = (this._result.akce.length + this._result.lokalita.length) / this.numChildren * 100;
          this.state.documentProgress = (this._result.akce.length + this._result.lokalita.length) / this.numChildren * 100;
        });
      }
    }

  }

  setData() {
    if (this.inDocument() && isPlatformBrowser(this.platformId)) {
      this.setVsize();
      setTimeout(() => {
        this.getArchZaznam();
      }, 1)

    }
    const autor = this._result.ext_zdroj_autor ? this._result.ext_zdroj_autor.join(' – ') : '';
    
    switch (this._result.ext_zdroj_typ) {
      case 'HES-001117': // 'kniha'
        this.bibTex = `@book{https://digiarchiv.aiscr.cz/id/${this._result.ident_cely},
          author = {${autor}}, 
          title = {${this._result.ext_zdroj_nazev}},
          ${this._result.ext_zdroj_vydavatel ? `publisher = {${this._result.ext_zdroj_vydavatel}},` : ''}
          year = {${this._result.ext_zdroj_rok_vydani_vzniku}},
          ${this._result.ext_zdroj_edice_rada ? `series = {${this._result.ext_zdroj_edice_rada}},` : ''}
          ${this._result.ext_zdroj_misto ? `address = {${this._result.ext_zdroj_misto}},` : ''}
          ${this._result.ext_zdroj_isbn ? `note = ISBN: {${this._result.ext_zdroj_isbn}},` : ''}${this._result.ext_zdroj_issn ? `note = ISSN: {${this._result.ext_zdroj_issn}},` : ''}
          ${this._result.ext_zdroj_link ? `url = {${this._result.ext_zdroj_link}},` : ''}
          ${this._result.ext_zdroj_doi ? `doi = {${this._result.ext_zdroj_doi}}` : ''}
        }`;
        break;
      case 'HES-001118': // 'část knihy':
        this.bibTex = `@inproceedings{https://digiarchiv.aiscr.cz/id/${this._result.ident_cely},
            author = {${autor}}, 
            title = {${this._result.ext_zdroj_nazev}},
            booktitle = {${this._result.ext_zdroj_sbornik_nazev}},
            year = {${this._result.ext_zdroj_rok_vydani_vzniku}},
            ${this._result.ext_zdroj_editor ? `editor = {${this._result.ext_zdroj_editor}},` : ''}
            ${this._result.ext_zdroj_vydavatel ? `publisher = {${this._result.ext_zdroj_vydavatel}},` : ''}
            ${this._result.ext_zdroj_edice_rada ? `series = {${this._result.ext_zdroj_edice_rada}},` : ''}
            ${this._result.ext_zdroj_misto ? `address = {${this._result.ext_zdroj_misto}},` : ''}
            ${this._result.ext_zdroj_issn ? `note = ISSN: {${this._result.ext_zdroj_issn}},` : ''}${this._result.ext_zdroj_isbn ? `note = ISBN: {${this._result.ext_zdroj_isbn}},` : ''}
            ${this._result.ext_zdroj_link ? `url = {${this._result.ext_zdroj_link}},` : ''}
            ${this._result.ext_zdroj_doi ? `doi = {${this._result.ext_zdroj_doi}},` : ''}
            pages = {${this._result.ext_zdroj_paginace_titulu}}
          }`;
        break;
      case 'HES-001119': // 'článek v časopise':
        this.bibTex = `@article{https://digiarchiv.aiscr.cz/id/${this._result.ident_cely},
              author = {${autor}},
              title = {${this._result.ext_zdroj_nazev}},
              journal = {${this._result.ext_zdroj_casopis_denik_nazev}},
              year = {${this._result.ext_zdroj_rok_vydani_vzniku}},
              volume = {${this._result.ext_zdroj_casopis_rocnik}},
              ${this._result.ext_zdroj_issn ? `note = ISSN: {${this._result.ext_zdroj_issn}},` : ''}
              ${this._result.ext_zdroj_link ? `url = {${this._result.ext_zdroj_link}},` : ''}
              ${this._result.ext_zdroj_paginace_titulu ? `pages = {${this._result.ext_zdroj_paginace_titulu}}, ` : ''}
              doi = {${this._result.ext_zdroj_doi}}
            }`;
        break;
      case 'HES-001120': // 'článek v novinách':
        this.bibTex = `@article{https://digiarchiv.aiscr.cz/id/${this._result.ident_cely},
                author = {${autor}},
                title = {${this._result.ext_zdroj_nazev}},
                journal = {${this._result.ext_zdroj_casopis_denik_nazev}},
                year = {${this._result.ext_zdroj_rok_vydani_vzniku}},
                volume = {${this._result.ext_zdroj_datum_rd}},
                ${this._result.ext_zdroj_isbn ? `note = ISSN: {${this._result.ext_zdroj_issn}},` : ''}
                ${this._result.ext_zdroj_link ? `url = {${this._result.ext_zdroj_link}},` : ''}
                ${this._result.ext_zdroj_paginace_titulu ? `pages = {${this._result.ext_zdroj_paginace_titulu}},` : ''}
                doi = {${this._result.ext_zdroj_doi}}
              }`;
        break;
      case 'HES-001121': // 'nepublikovaná zpráva':
        this.bibTex = `@unpublished{https://digiarchiv.aiscr.cz/id/${this._result.ident_cely},
                  author = {${autor}},
                  title = {${this._result.ext_zdroj_oznaceni}},
                  year = {${this._result.ext_zdroj_rok_vydani_vzniku}},
                  ${this._result.ext_zdroj_link ? `url = {${this._result.ext_zdroj_link}},` : ''}
                  note = {${this._result.ext_zdroj_organizace}},
                  doi = {${this._result.ext_zdroj_doi}}
                }`;
        break;
    }
  }

  toggleFav() {
    const result = this.result();
    if (result.isFav) {
      this.service.removeFav(result.ident_cely).subscribe(res => {
        this.result().isFav = false;
      });
    } else {
      this.service.addFav(result.ident_cely).subscribe(res => {
        this.result().isFav = true;
      });
    }
  }

  toggleDetail() {
    this._detailExpanded = !this.detailExpanded();
  }
}
