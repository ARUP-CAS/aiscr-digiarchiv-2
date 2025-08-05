import { Component, input } from '@angular/core';
import { AppState } from '../../app.state';
import { DokumentComponent } from "../dokument/dokument.component";
import { SamostatnyNalezComponent } from "../samostatny-nalez/samostatny-nalez.component";
import { ProjektComponent } from "../projekt/projekt.component";
import { AkceComponent } from "../akce/akce.component";
import { LokalitaComponent } from "../lokalita/lokalita.component";
import { RelatedComponent } from "../../components/related/related.component";
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ResultActionsComponent } from "../../components/result-actions/result-actions.component";
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { InlineFilterComponent } from '../../components/inline-filter/inline-filter.component';
import { DatePipe } from '@angular/common';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { Entity } from '../entity/entity';

@Component({
  selector: 'app-entity-container',
  imports: [
     TranslateModule, RouterModule, FlexLayoutModule, DatePipe,
     MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
      MatProgressBarModule, MatTooltipModule, MatExpansionModule,
      ResultActionsComponent,
      DokumentComponent, SamostatnyNalezComponent, ProjektComponent, AkceComponent, LokalitaComponent, RelatedComponent,
      MatCardModule, MatButtonModule, MatIconModule, ResultActionsComponent],
  templateUrl: './entity-container.html',
  styleUrl: './entity-container.scss'
})
export class EntityContainer extends Entity {
  entity = input<string>();
}
