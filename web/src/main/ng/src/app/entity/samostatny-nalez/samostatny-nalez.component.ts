import { Component, OnInit, Input, OnChanges } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { MatDialog } from '@angular/material/dialog';
import { FileViewerComponent } from 'src/app/components/file-viewer/file-viewer.component';
import { AppService } from 'src/app/app.service';
import { DatePipe } from '@angular/common';
import { DocumentDialogComponent } from 'src/app/components/document-dialog/document-dialog.component';
import { FeedbackDialogComponent } from 'src/app/components/feedback-dialog/feedback-dialog.component';

@Component({
  selector: 'app-samostatny-nalez',
  templateUrl: './samostatny-nalez.component.html',
  styleUrls: ['./samostatny-nalez.component.scss']
})
export class SamostatnyNalezComponent implements OnInit, OnChanges {

  @Input() result;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() inDocument = false;
  @Input() mapDetail: boolean;
  @Input() isDocumentDialogOpen: boolean;
  hasRights: boolean;
  hasDetail: boolean;
  imgSrc: string;
  bibTex: string;
  

  constructor(
    private datePipe: DatePipe,
    private dialog: MatDialog,
    private router: Router,
    public config: AppConfiguration,
    public state: AppState,
    public service: AppService
  ) { }

  ngOnInit(): void {
    this.hasRights = this.state.hasRights(this.result.pristupnost, this.result.organizace);
    if (this.result.soubor_filepath?.length > 0) {
      //this.imgSrc = this.config.server + '/api/img?id=' + this.result.soubor_filepath[0];
      this.imgSrc = this.config.context + '/api/img?id=' + this.result.soubor_filepath[0];
    }
    if (this.result.loc_rpt) {
      const coords = this.result.loc_rpt[0].split(',');
      this.result.centroid_e = coords[0];
      this.result.centroid_n = coords[1];
    }
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex = 
     `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
       author = {AMČR}, 
       title = {Záznam ${this.result.ident_cely}},
       url = {https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       publisher = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
  }

  ngOnChanges(c) {
    if (c.result) {
      this.hasDetail = false;
      this.detailExpanded = this.inDocument;
    }
    if (this.mapDetail) {
      this.getFullId();
    }
  }

  getFullId() {
    this.service.getId(this.result.ident_cely).subscribe((res: any) => {
      this.result = res.response.docs[0];
      this.hasDetail = true;
      if (this.result.loc_rpt) {
        const coords = this.result.loc_rpt[0].split(',');
        this.result.centroid_e = coords[0];
        this.result.centroid_n = coords[1];
      }
    });
  }

  toggleDetail() {
    if (!this.hasDetail && !this.inDocument) {
      this.getFullId();
    }
    this.detailExpanded = !this.detailExpanded;
  }

  print() {
    if (this.inDocument) {
      this.service.print();
    } else {
      this.state.printing = true;
      this.router.navigate(['/id', this.result.ident_cely]);
    }
  }

  viewFiles() {
    const canView = true;
    if (canView) {
      this.state.dialogRef = this.dialog.open(FileViewerComponent, {
        panelClass: 'app-file-viewer',
        width: '1000px',
        height: '900px',
        data: this.result
      });
    } else {
      const msg = this.service.getTranslation('insuficient rights');
      alert(msg);
    }
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
}
