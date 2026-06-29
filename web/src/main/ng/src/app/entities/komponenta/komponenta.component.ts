import { Component, forwardRef } from '@angular/core';
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

import { InlineFilterComponent } from '../../components/inline-filter/inline-filter.component';
import { ResultActionsComponent } from '../../components/result-actions/result-actions.component';
import { AkceComponent } from '../akce/akce.component';
import { DokumentComponent } from "../dokument/dokument.component";
import { LokalitaComponent } from "../lokalita/lokalita.component";
import { NalezComponent } from "../nalez/nalez.component";
import { Entity } from '../entity/entity';
import { DokJednotkaComponent } from "../dok-jednotka/dok-jednotka.component";
import { PianComponent } from "../pian/pian.component";
import { RelatedComponent } from '../../components/related/related.component';

@Component({
  imports: [
    TranslateModule, RouterModule, CommonModule,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    InlineFilterComponent, MatButtonModule,
    ResultActionsComponent,
    forwardRef(() => RelatedComponent),
    // forwardRef(() => AkceComponent),
    // forwardRef(() => DokumentComponent),
    // forwardRef(() => LokalitaComponent),
    forwardRef(() => NalezComponent),
    DokJednotkaComponent,
    PianComponent
],
  selector: 'app-komponenta',
  templateUrl: './komponenta.component.html',
  styleUrls: ['./komponenta.component.scss']
})
export class KomponentaComponent extends Entity {


  opened = false;
  idShort: string;

  // nalez: Nalez[] = [];
  aktivity: string[] = []; 

  override setBibTex() {
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex =
      `@misc{https://digiarchiv.aiscr.cz/id/${this.result().ident_cely},
       author = {Archeologický informační systém České republiky},
       title = {Záznam ${this.result().ident_cely}},
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this.result().ident_cely}},
       note = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
  }

  override checkRelations() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    if (this.isChild() || (!this.state.isMapaCollapsed && !this.mapDetail())) {
      return;
    }

    const related: { entity: string; ident_cely: string; }[] = [];
    this.relationsChecked = true;
    if (this.result().komponenta_zdroj_ident_cely) {
      related.push({entity: this.result().komponenta_zdroj, ident_cely: this.result().komponenta_zdroj_ident_cely})
    }
    this.related.set(related);

  }


  hasAktivita(field: string) {
    const result = this.result();
    return result[field] && result[field][0] !== '0';
  }

}
