import { Component, OnInit, Input, OnChanges, Inject, PLATFORM_ID, forwardRef } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Entity } from '../entity/entity';
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
import { FileViewerComponent } from '../../components/file-viewer/file-viewer.component';
import { InlineFilterComponent } from '../../components/inline-filter/inline-filter.component';
import { RelatedComponent } from '../../components/related/related.component';
import { ResultActionsComponent } from '../../components/result-actions/result-actions.component';

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    InlineFilterComponent, DatePipe, MatButtonModule,
    ResultActionsComponent,
    MatAccordion,
    forwardRef(() => RelatedComponent)
],
  selector: 'app-samostatny-nalez',
  templateUrl: './samostatny-nalez.component.html',
  styleUrls: ['./samostatny-nalez.component.scss']
})
export class SamostatnyNalezComponent extends Entity {

  override setBibTex() {
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex = 
     `@misc{https://digiarchiv.aiscr.cz/id/${this._result.ident_cely},
       author = {Archeologický informační systém České republiky}, 
       title = {Záznam ${this._result.ident_cely}},
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this._result.ident_cely}},
       note = {Archeologická mapa České republiky [cit. ${now}]},
       doi = {${this._result.samostatny_nalez_igsn}}
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
      this._result.samostatny_nalez_projekt = res.samostatny_nalez_projekt;
      this.relationsChecked = true;
      
      const related: { entity: string; ident_cely: string; }[] = [];
      if (res.samostatny_nalez_projekt) {
        related.push({entity: 'projekt', ident_cely: res.samostatny_nalez_projekt})
      }
      this.related.set(related);
    });
  }

  override viewFiles() {
    const canView = true;
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
