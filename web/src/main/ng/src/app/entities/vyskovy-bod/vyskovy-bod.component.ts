import { DatePipe } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { AppState } from '../../app.state';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ResultActionsComponent } from "../../components/result-actions/result-actions.component";
import { MatExpansionModule } from "@angular/material/expansion";
import { AdbComponent } from "../adb/adb.component";
import { InlineFilterComponent } from "../../components/inline-filter/inline-filter.component";

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule,
    MatCardModule, MatIconModule, MatTooltipModule,
    MatButtonModule,
    ResultActionsComponent,
    MatExpansionModule,
    AdbComponent,
    InlineFilterComponent
],
  selector: 'app-vyskovy-bod',
  templateUrl: './vyskovy-bod.component.html',
  styleUrls: ['./vyskovy-bod.component.scss']
})
export class VyskovyBodComponent implements OnInit {

  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() inDocument = false;

  bibTex: string;

  constructor(
    private datePipe: DatePipe,
    public state: AppState) { }

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
