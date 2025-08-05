
import { Component, OnInit, Input } from '@angular/core';
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
import { AkceComponent } from "../akce/akce.component";
import { AdbComponent } from "../adb/adb.component";
import { KomponentaComponent } from "../komponenta/komponenta.component";
import { PianComponent } from "../pian/pian.component";
import { LokalitaComponent } from "../lokalita/lokalita.component";

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule, CommonModule,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    InlineFilterComponent, MatButtonModule,
    ResultActionsComponent,
    AkceComponent,
    AdbComponent,
    KomponentaComponent,
    PianComponent,
    LokalitaComponent
],
  selector: 'app-dok-jednotka',
  templateUrl: './dok-jednotka.component.html',
  styleUrls: ['./dok-jednotka.component.scss']
})
export class DokJednotkaComponent implements OnInit {

  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() pians: any[];
  @Input() adbs: any[];
  @Input() inDocument = false;

  bibTex: string;

  constructor(
    private datePipe: DatePipe,
    private service: AppService,
    public state: AppState,
    public config: AppConfiguration
  ) {}

  ngOnInit(): void {
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex =
      `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
       author = {Archeologický informační systém České republiky},
       title = {Záznam ${this.result.ident_cely}},
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       note = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
  }

  toggleDetail() {
    this.detailExpanded = !this.detailExpanded;
  }
}
