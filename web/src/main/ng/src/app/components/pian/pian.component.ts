import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, Input, Inject, PLATFORM_ID } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { FeedbackDialogComponent } from '../feedback-dialog/feedback-dialog.component';

@Component({
  selector: 'app-pian',
  templateUrl: './pian.component.html',
  styleUrls: ['./pian.component.scss']
})
export class PianComponent implements OnInit {

  @Input() result;
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

  ngOnChanges(c) {
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
