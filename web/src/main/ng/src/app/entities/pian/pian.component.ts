import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, Inject, PLATFORM_ID, forwardRef, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
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
import { FeedbackDialogComponent } from '../../components/feedback-dialog/feedback-dialog.component';
import { InlineFilterComponent } from '../../components/inline-filter/inline-filter.component';
import { ResultActionsComponent } from '../../components/result-actions/result-actions.component';
import { RelatedComponent } from "../../components/related/related.component";
import { Entity } from '../entity/entity';

@Component({
  imports: [
    TranslateModule,
    RouterModule,
    FlexLayoutModule,
    MatCardModule,
    MatIconModule,
    MatSidenavModule,
    MatTabsModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatExpansionModule,
    InlineFilterComponent,
    MatButtonModule,
    ResultActionsComponent,
    forwardRef(() => RelatedComponent)
],
  selector: 'app-pian',
  templateUrl: './pian.component.html',
  styleUrls: ['./pian.component.scss']
})
export class PianComponent extends Entity {

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
    if (this.isChild()) {
      return;
    }
    this.service.checkRelations(this._result.ident_cely).subscribe((res: any) => {
      this._result.az_dj_pian = res.az_dj_pian;
      this.relationsChecked = true;
      this.related = [];
      res.id_akce.forEach((ident_cely: string) => {
        this.related.push({entity: 'akce', ident_cely})
      });
      res.id_lokalita.forEach((ident_cely: string) => {
        this.related.push({entity: 'lokalita', ident_cely})
      });
    });
  }

}
