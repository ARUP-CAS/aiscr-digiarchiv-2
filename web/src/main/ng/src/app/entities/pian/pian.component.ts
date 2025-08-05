import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, Input, Inject, PLATFORM_ID } from '@angular/core';
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
    RelatedComponent
],
  selector: 'app-pian',
  templateUrl: './pian.component.html',
  styleUrls: ['./pian.component.scss']
})
export class PianComponent implements OnInit {

  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() mapDetail: boolean;
  @Input() isDocumentDialogOpen: boolean;
  @Input() inDocument = false;
  hasDetail: boolean;
  hasRights: boolean;
  bibTex: string;

  relationsChecked = false;
  related: {entity: string, ident_cely: string}[] = [];

  constructor(
    @Inject(PLATFORM_ID) private platformId: any,
    private datePipe: DatePipe,
    public state: AppState,
    public service: AppService,
    public config: AppConfiguration,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    if (this.result?.ident_cely) {
      this.initProperties();
    } else {
      const pianid = this.result.id ? this.result.id : this.result;
      this.service.getIdAsChild([pianid], "pian").subscribe((res: any) => {
        this.result = res.response.docs[0];
        this.initProperties();
      });
    }

  }

  ngOnChanges(c: any) {
    if (c.result) {
      this.checkRelations();
      this.hasDetail = false;
      this.detailExpanded = this.inDocument;
    }
  }

  initProperties() {
    if (!this.result) {
      return;
    }
    // this.hasRights = this.state.hasRights(this.result.pristupnost, this.result.organizace);
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex =
     `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
       author = {Archeologický informační systém České republiky}, 
       title = {Záznam ${this.result.ident_cely}},
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       note = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
  }

  checkRelations() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    if (this.isChild) {
      return;
    }
    this.service.checkRelations(this.result.ident_cely).subscribe((res: any) => {
      this.result.az_dj_pian = res.az_dj_pian;
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

  toggleFav() {
    if (this.result.isFav) {
      this.service.removeFav(this.result.ident_cely).subscribe(res => {
        this.result.isFav = false;
      });
    } else {
      this.service.addFav(this.result.ident_cely).subscribe(res => {
        this.result.isFav = true;
      });
    }
  }

  openFeedback() {
    this.state.dialogRef = this.dialog.open(FeedbackDialogComponent, {
      width: '900px',
      data: this.result.ident_cely,
      panelClass: 'app-feedback-dialog'
    });
  } 

  toggleDetail() {
    this.detailExpanded = !this.detailExpanded;
  }
}
