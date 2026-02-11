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
import { NalezComponent } from "../nalez/nalez.component";
import { DokumentComponent } from "../dokument/dokument.component";
import { CommonModule } from '@angular/common';
import { Entity } from '../entity/entity';

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule, CommonModule, 
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    InlineFilterComponent, MatButtonModule,
    ResultActionsComponent,
    forwardRef(() => NalezComponent),
    forwardRef(() => DokumentComponent)
],
  selector: 'app-komponenta-dokument',
  templateUrl: './komponenta-dokument.component.html',
  styleUrls: ['./komponenta-dokument.component.scss']
})
export class KomponentaDokumentComponent extends Entity {
}
