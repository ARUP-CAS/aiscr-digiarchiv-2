
import { Component, forwardRef, input } from '@angular/core';
import { CommonModule } from '@angular/common';
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
import { AkceComponent } from "../akce/akce.component";
import { AdbComponent } from "../adb/adb.component";
import { KomponentaComponent } from "../komponenta/komponenta.component";
import { PianComponent } from "../pian/pian.component";
import { LokalitaComponent } from "../lokalita/lokalita.component";
import { Entity } from '../entity/entity';

@Component({
  imports: [
    TranslateModule, RouterModule,  CommonModule,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    InlineFilterComponent, MatButtonModule,
    ResultActionsComponent,
    forwardRef(() => AkceComponent),
    forwardRef(() => AdbComponent),
    forwardRef(() => KomponentaComponent),
    forwardRef(() => PianComponent),
    forwardRef(() => LokalitaComponent)
],
  selector: 'app-dok-jednotka',
  templateUrl: './dok-jednotka.component.html',
  styleUrls: ['./dok-jednotka.component.scss']
})
export class DokJednotkaComponent extends Entity {

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

  readonly pians = input<any[]>();
  readonly adbs = input<any[]>();

}
