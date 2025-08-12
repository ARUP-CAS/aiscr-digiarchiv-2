import { DatePipe } from '@angular/common';
import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { DocumentDialogComponent } from '../../components/document-dialog/document-dialog.component';
import { FeedbackDialogComponent } from '../../components/feedback-dialog/feedback-dialog.component';
import { InlineFilterComponent } from '../../components/inline-filter/inline-filter.component';
import { ResultActionsComponent } from '../../components/result-actions/result-actions.component';
import { AkceComponent } from '../akce/akce.component';
import { LokalitaComponent } from '../lokalita/lokalita.component';
import { KomponentaComponent } from "../komponenta/komponenta.component";
import { DokumentComponent } from "../dokument/dokument.component";

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    InlineFilterComponent, MatButtonModule,
    ResultActionsComponent,
    forwardRef(() => KomponentaComponent),
    forwardRef(() => DokumentComponent)
],
  selector: 'app-dokument-cast',
  templateUrl: './dokument-cast.component.html',
  styleUrls: ['./dokument-cast.component.scss']
})
export class DokumentCastComponent implements OnInit {

  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() mapDetail: boolean;
  @Input() isDocumentDialogOpen: boolean;
  @Input() inDocument = false;
  hasRights: boolean;

  bibTex: string;

  constructor(
    private datePipe: DatePipe,
    private router: Router,
    public service: AppService,
    public state: AppState,
    private dialog: MatDialog,
    public config: AppConfiguration
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

  gotoDoc() {
    this.state.itemView = 'default';
    if (this.state.dialogRef) {
      this.state.dialogRef.close();
    }
    this.router.navigate(['/id', this.result.ident_cely]);
  }

  print() {
    if (this.inDocument) {
      this.service.print();
    } else {
      this.state.printing.set(true);
      this.router.navigate(['/id', this.result.ident_cely]);
    }
  }
  

  openDocument() {
    this.state.dialogRef = this.dialog.open(DocumentDialogComponent, {
      width: '900px',
      data: this.result,
      panelClass: 'app-document-dialog'
    });
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
