import { Component, forwardRef } from '@angular/core';
import { RouterModule } from '@angular/router';
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

import { InlineFilterComponent } from '../../components/inline-filter/inline-filter.component';
import { RelatedComponent } from '../../components/related/related.component';
import { ResultActionsComponent } from '../../components/result-actions/result-actions.component';
import { Entity } from '../entity/entity';

@Component({
  imports: [
    TranslateModule, RouterModule, 
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    InlineFilterComponent, DatePipe, MatButtonModule,
    ResultActionsComponent,
    MatAccordion,
    forwardRef(() => RelatedComponent)
],
  selector: 'app-projekt',
  templateUrl: './projekt.component.html',
  styleUrls: ['./projekt.component.scss']
})
export class ProjektComponent extends Entity {
  
override setBibTex() {
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex =
      `@misc{https://digiarchiv.aiscr.cz/id/${this._result.ident_cely},
       author = {Archeologický informační systém České republiky},
       title = {Záznam ${this._result.ident_cely}},
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this._result.ident_cely}},
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
    this.service.checkRelations(this._result.ident_cely).subscribe((res: any) => {
      this._result.projekt_archeologicky_zaznam = res.projekt_archeologicky_zaznam;
      this._result.projekt_samostatny_nalez = res.projekt_samostatny_nalez;
      this._result.projekt_dokument = res.projekt_dokument;
      this.relationsChecked = true;
      
      const related: { entity: string; ident_cely: string; }[] = [];
      res.id_akce.forEach((ident_cely: string) => {
        related.push({entity: 'akce', ident_cely})
      });
      res.id_lokalita.forEach((ident_cely: string) => {
        related.push({entity: 'lokalita', ident_cely})
      });
      res.projekt_samostatny_nalez.forEach((ident_cely: string) => {
        related.push({entity: 'samostatny_nalez', ident_cely})
      });
      res.projekt_dokument.forEach((ident_cely: string) => {
        related.push({entity: 'dokument', ident_cely})
      });
      this.related.set(related);
    });
  }


  override getFullId() {
    this.service.getId(this._result.ident_cely).subscribe((res: any) => {
      this.result = res.response.docs[0];
      this.hasDetail = true;
    });
  }

  formatDate(s: string) {
    // [2023-05-26, 2023-05-26]
    // (d)d.(m)m.rrrr - (d)d.(m)m.rrrr
    let parts = s.replace('[', '').replace(']', '').split(',');
    console.log()
    return this.datePipe.transform(parts[0].trim(), 'd.M.yyyy') + ' - ' + this.datePipe.transform(parts[1].trim(), 'd.M.yyyy');
  }

}
