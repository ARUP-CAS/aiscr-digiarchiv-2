import { Component, OnInit, forwardRef, input } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
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
import { Utils } from '../../shared/utils';
import { AkceComponent } from '../akce/akce.component';
import { AdbComponent } from '../adb/adb.component';
import { DokumentComponent } from "../dokument/dokument.component";
import { LokalitaComponent } from "../lokalita/lokalita.component";
import { NalezComponent } from "../nalez/nalez.component";
import { Entity } from '../entity/entity';

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule, CommonModule,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    InlineFilterComponent, MatButtonModule,
    ResultActionsComponent,
    forwardRef(() => AkceComponent),
    forwardRef(() => DokumentComponent),
    forwardRef(() => LokalitaComponent),
    forwardRef(() => NalezComponent)
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


  hasAktivita(field: string) {
    const result = this.result();
    return result[field] && result[field][0] !== '0';
  }

}
