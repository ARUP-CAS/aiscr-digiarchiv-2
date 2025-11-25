
import { Component, OnInit, Input, OnChanges, Inject, PLATFORM_ID, forwardRef } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Entity } from '../entity/entity';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { FileViewerComponent } from '../../components/file-viewer/file-viewer.component';
import { Utils } from '../../shared/utils';
import { InlineFilterComponent } from "../../components/inline-filter/inline-filter.component";
import { ResultActionsComponent } from "../../components/result-actions/result-actions.component";
import { MatAccordion, MatExpansionModule } from "@angular/material/expansion";
import { RelatedComponent } from "../../components/related/related.component";
import { MatButtonModule } from '@angular/material/button';
import { KomponentaComponent } from "../komponenta/komponenta.component";

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    InlineFilterComponent, DatePipe, MatButtonModule,
    ResultActionsComponent,
    MatAccordion,
    forwardRef(() => RelatedComponent),
    KomponentaComponent
],
  selector: 'app-dokument',
  templateUrl: './dokument.component.html',
  styleUrls: ['./dokument.component.scss']
})
export class DokumentComponent extends Entity {


 override checkRelations() {
     if (!isPlatformBrowser(this.platformId)) {
       return;
     }
  if (!this._result.ident_cely || this.isChild() || (!this.state.isMapaCollapsed && !this.mapDetail())) {
    return;
  }
  this.service.checkRelations(this._result.ident_cely).subscribe((res: any) => {
    this._result.dokument_cast_archeologicky_zaznam = res.dokument_cast_archeologicky_zaznam;
    this._result.dokument_cast_projekt = res.dokument_cast_projekt;
    this.relationsChecked = true;
    
    const related: { entity: string; ident_cely: string; }[] = [];
    res.id_akce.forEach((ident_cely: string) => {
      related.push({entity: 'akce', ident_cely})
    });
    res.id_lokalita.forEach((ident_cely: string) => {
      related.push({entity: 'lokalita', ident_cely})
    });
    res.dokument_cast_projekt.forEach((ident_cely: string) => {
      related.push({entity: 'projekt', ident_cely})
    });
    this.related.set(related);
  });
}


  override setBibTex() {
    const organizace = this.service.getHeslarTranslation(this._result.dokument_organizace, 'organizace');
    const autor = this._result.dokument_autor ? this._result.dokument_autor.join(' and ') : '';
    this.bibTex = `@misc{https://digiarchiv.aiscr.cz/id/${this._result.ident_cely},
      author = {${autor}}, 
      title = {Dokument ${this._result.ident_cely}},
      howpublished = url{https://digiarchiv.aiscr.cz/id/${this._result.ident_cely}},
      year = {${this._result.dokument_rok_vzniku}},
      note = {${organizace}},
      doi = {${this._result.dokument_doi}}
    }`;
  }

  override getFullId() {
    this.service.getId(this._result.ident_cely).subscribe((res: any) => {
      this._result = res.response.docs[0];
      if (this._result.dokument_cast) {
        this._result.dokument_cast.sort((dc1: { ident_cely: string; }, dc2: { ident_cely: any; }) => dc1.ident_cely.localeCompare(dc2.ident_cely) );
      }
      // this.setVsize();
      // this.getArchZaznam();
      // this.getProjekts();
      this.hasDetail = true;
    });
  }

  override setImg() {
    if (this._result.soubor_filepath?.length > 0) {
      this._result.soubor.sort((a: any, b: any) => {
        return a.nazev.localeCompare(b.nazev);
      });
      this.imgSrc = this.config.context + '/api/img/thumb?id=' + this._result.soubor[0].id;
    }

  }

  override okres() {

    if (this._result.location_info) {
      this._result.location_info.forEach((li: { okres: string; }) => {
        if (!this.okresy.includes(li.okres)) {
          this.okresy.push(li.okres);
        }
      });
    }

    if (this._result.hasOwnProperty('f_okres')) {
      const okresy = [];
      const katastry = [];
      let ret = '';
      for (let idx = 0; idx < this._result.f_okres.length; idx++) {
        const okres = this._result.f_okres[idx];
        const katastr = this._result.f_katastr[idx];

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
      return '';
    }
  }

  override popisObsahu(): string {
    const s: string = this._result.popis;
    return s.replace(/\[new_line\]/gi, '<br/>');
  }

  override viewFiles() {
    if (this.isChild) {
      this.gotoDoc();
      return;
    }
    const canView = this.state.hasRights(this._result.pristupnost, this._result.dokument_organizace);
    // const canView = true;
    if (canView) {
      this.state.dialogRef = this.dialog.open(FileViewerComponent, {
        panelClass: 'app-file-viewer',
        width: '1000px',
        height: '900px',
        data: this._result
      });
    } else {
      const msg = this.service.getTranslation('alert.insuficient rights');
      alert(msg);
    }
  }

}
