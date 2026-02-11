import { DatePipe } from '@angular/common';
import { Component, forwardRef } from '@angular/core';
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
import { InlineFilterComponent } from '../../components/inline-filter/inline-filter.component';
import { ResultActionsComponent } from '../../components/result-actions/result-actions.component';
import { AkceComponent } from '../akce/akce.component';
import { LokalitaComponent } from "../lokalita/lokalita.component";
import { Entity } from '../entity/entity';

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule, DatePipe,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    InlineFilterComponent, MatButtonModule,
    ResultActionsComponent,
    forwardRef(() => AkceComponent),
    forwardRef(() => LokalitaComponent)
],
  selector: 'app-adb',
  templateUrl: './adb.component.html',
  styleUrls: ['./adb.component.scss']
})
export class AdbComponent extends Entity {

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
}
