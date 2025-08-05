import { DatePipe } from '@angular/common';
import { Component, OnInit, Input } from '@angular/core';
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
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { InlineFilterComponent } from '../../components/inline-filter/inline-filter.component';
import { ResultActionsComponent } from '../../components/result-actions/result-actions.component';
import { AkceComponent } from '../akce/akce.component';

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    InlineFilterComponent, MatButtonModule,
    ResultActionsComponent,
    AkceComponent
],
  selector: 'app-adb',
  templateUrl: './adb.component.html',
  styleUrls: ['./adb.component.scss']
})
export class AdbComponent implements OnInit {

  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() inDocument = false;
  @Input() onlyHead = false;

  bibTex: string;

  constructor(
    private datePipe: DatePipe,
    public service: AppService,
    public state: AppState
  ) { }

  ngOnInit(): void {
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex =
      `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
       author = {Archeologický informační systém České republiky},
       title = {Záznam ${this.result.ident_cely}},
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       note = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
    if (this.result?.ident_cely) { 
    } else {
      const pianid = this.result.id ? this.result.id : this.result;

      this.service.getIdAsChild([pianid], "adb").subscribe((res: any) => {
        this.result = res.response.docs[0]; 
      });
    }
  }

  toggleDetail() {
    this.detailExpanded = !this.detailExpanded;
  }

}
