
import { Component, OnInit, Input, OnChanges, Inject, PLATFORM_ID } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Router, RouterModule } from '@angular/router';
import { DatePipe, isPlatformBrowser } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule, MatAccordion } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { InlineFilterComponent } from '../../components/inline-filter/inline-filter.component';
import { RelatedComponent } from '../../components/related/related.component';
import { ResultActionsComponent } from '../../components/result-actions/result-actions.component';
import { Entity } from '../entity/entity';
import { DokJednotkaComponent } from "../dok-jednotka/dok-jednotka.component";
import { ExterniZdrojComponent } from "../externi-zdroj/externi-zdroj.component";

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    InlineFilterComponent, DatePipe, MatButtonModule,
    ResultActionsComponent,
    MatAccordion,
    RelatedComponent,
    DokJednotkaComponent,
    ExterniZdrojComponent
],
  selector: 'app-lokalita',
  templateUrl: './lokalita.component.html',
  styleUrls: ['./lokalita.component.scss']
})
export class LokalitaComponent extends Entity {
  

  override setBibTex() {
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex =
     `@misc{https://digiarchiv.aiscr.cz/id/${this._result.ident_cely},
       author = {Archeologický informační systém České republiky}, 
       title = {Záznam ${this._result.ident_cely}},
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this._result.ident_cely}},
       note = {Archeologická mapa České republiky [cit. ${now}]},
       doi = {${this._result.lokalita_igsn}}
     }`;
  }

  override checkRelations() {
      if (!isPlatformBrowser(this.platformId)) {
        return;
      }
    if (this.isChild || (!this.state.isMapaCollapsed && !this.mapDetail)) {
      return;
    }
    this.service.checkRelations(this._result.ident_cely).subscribe((res: any) => {
      this._result.az_dokument = res.az_dokument;
      this._result.akce_projekt = res.akce_projekt;
      this.relationsChecked = true;
      this.related = [];
      res.az_dokument.forEach((ident_cely: string) => {
        this.related.push({entity: 'dokument', ident_cely})
      });
      
    });
  }

  override getFullId() {
    this.service.getId(this._result.ident_cely).subscribe((res: any) => {
      this.result = res.response.docs[0];
      // this.getDokuments();
      this.getExtZdroj();
      this.hasDetail = true;
    });
  }

  getExtZdroj() {
    if (this._result.az_ext_zdroj) {
      const orig = JSON.parse(JSON.stringify(this._result.az_ext_zdroj));
      this._result.az_ext_zdroj = [];
      for (let i = 0; i < orig.length; i = i + 20) {
        const ids = orig.slice(i, i + 20);
        this.service.getIdAsChild(ids, "ext_zdroj").subscribe((res: any) => {
          this._result.az_ext_odkaz.forEach((eo:any) => {
            const ez = res.response.docs.find((ez: any) => eo.ext_zdroj.id === ez.ident_cely);
            ez.ext_odkaz_paginace = eo.paginace;
            this._result.az_ext_zdroj.push(ez);
          });

          this._result.az_ext_zdroj.sort((ez1: any, ez2: any) => {
            let res = 0;
            res = ez1.ext_zdroj_autor[0].localeCompare(ez2.ext_zdroj_autor[0], 'cs');
            if (res === 0) {
              res = ez1.ext_zdroj_rok_vydani_vzniku - ez2.ext_zdroj_rok_vydani_vzniku;
            }
            if (res === 0) {
              res = ez1.ext_zdroj_nazev.localeCompare(ez2.ext_zdroj_nazev);
            }
            return res;
          })
        });
      }
    }
  }

  

  cropped(s: string) {
    if (s.length > 100) {
      return s.slice(0, 100) + '(...)'
    } else {
      return s;
    }
  }

  pian(id: string) {
    return this._result.pian.filter((p: any) => p.ident_cely === id);
  }

  adb(id: string) {
    return this._result.adb.filter((p: any) => p.ident_cely === id);
  }
}
