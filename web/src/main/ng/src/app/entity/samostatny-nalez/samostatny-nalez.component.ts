import { Component, OnInit, Input, OnChanges, Inject, PLATFORM_ID } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { MatDialog } from '@angular/material/dialog';
import { FileViewerComponent } from 'src/app/components/file-viewer/file-viewer.component';
import { AppService } from 'src/app/app.service';
import { DatePipe, isPlatformBrowser } from '@angular/common';
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

  relationsChecked = false;
  related: {entity: string, ident_cely: string}[] = [];
  

  constructor(
    @Inject(PLATFORM_ID) private platformId: any,
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
      this.result.soubor.sort((a: any, b: any) => {
        return a.nazev.localeCompare(b.nazev);
      });
      this.imgSrc = this.config.context + '/api/img/thumb?id=' + this.result.soubor[0].id;

    }
    if (this.result.loc_rpt) {
      const coords = this.result.loc_rpt[0].split(',');
      this.result.centroid_e = coords[0];
      this.result.centroid_n = coords[1];
    }
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex = 
     `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
       author = {Archeologický informační systém České republiky}, 
       title = {Záznam ${this.result.ident_cely}},
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       note = {Archeologická mapa České republiky [cit. ${now}]},
       doi = {${this.result.samostatny_nalez_igsn}}
     }`;
     if (this.inDocument) {
      //this.checkRelations();
     }
  }  

  checkRelations() {
      if (!isPlatformBrowser(this.platformId)) {
        return;
      }
    if (this.isChild || (!this.state.isMapaCollapsed && !this.mapDetail)) {
      return;
    }
    this.service.checkRelations(this.result.ident_cely).subscribe((res: any) => {
      this.result.samostatny_nalez_projekt = res.samostatny_nalez_projekt;
      this.relationsChecked = true;
      this.related = [];
      if (res.samostatny_nalez_projekt) {
        this.related.push({entity: 'projekt', ident_cely: res.samostatny_nalez_projekt})
      }
    });
  }

  ngOnChanges(c) {
    if (c.result) {
      this.checkRelations();
      this.hasDetail = false;
      this.detailExpanded = this.inDocument;
    }
    if (this.mapDetail) {
      this.getFullId(false);
    }
  }

  getFullId(shouldLog: boolean) {
    this.service.getId(this.result.ident_cely, shouldLog).subscribe((res: any) => {
      this.result = res.response.docs[0];
      
      this.hasDetail = true;
    });
  }

  toggleDetail() {
    if (!this.hasDetail && !this.inDocument) {
      this.getFullId(true);
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
      const msg = this.service.getTranslation('alert.insuficient rights');
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
