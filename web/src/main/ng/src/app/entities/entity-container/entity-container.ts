import { Component, forwardRef, input } from '@angular/core';
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
import { PianComponent } from "../pian/pian.component";
import { ExterniZdrojComponent } from "../externi-zdroj/externi-zdroj.component";
import { AdbComponent } from "../adb/adb.component";
import { DokJednotkaComponent } from "../dok-jednotka/dok-jednotka.component";
import { KomponentaComponent } from "../komponenta/komponenta.component";
import { DokumentCastComponent } from "../dokument-cast/dokument-cast.component";
import { LetComponent } from "../let/let.component";
import { VyskovyBodComponent } from "../vyskovy-bod/vyskovy-bod.component";
import { KomponentaDokumentComponent } from "../komponenta-dokument/komponenta-dokument.component";

@Component({
  selector: 'app-entity-container',
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    forwardRef(() => DokumentComponent),
    forwardRef(() => SamostatnyNalezComponent),
    forwardRef(() => ProjektComponent),
    forwardRef(() => AkceComponent),
    forwardRef(() => LokalitaComponent),
    MatCardModule, MatButtonModule, MatIconModule,
    PianComponent,
    ExterniZdrojComponent,
    AdbComponent,
    DokJednotkaComponent,
    KomponentaComponent,
    DokumentCastComponent,
    LetComponent,
    VyskovyBodComponent,
    KomponentaDokumentComponent
],
  templateUrl: './entity-container.html',
  styleUrl: './entity-container.scss'
})
export class EntityContainer  {
  // entity = input<string>();
  result = input<any>();

  inDocument = input<boolean>(false);
  detailExpanded = input<boolean>(false);
  _detailExpanded: boolean;
  isChild = input<boolean>(false);
  mapDetail = input<boolean>(false);
  isDocumentDialogOpen = input<boolean>(false);
}
