import { Component, OnInit, Input, OnChanges, Inject, PLATFORM_ID } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { AppService } from 'src/app/app.service';
import { MatDialog } from '@angular/material/dialog';
import { DocumentDialogComponent } from 'src/app/components/document-dialog/document-dialog.component';
import { Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { DatePipe, isPlatformBrowser } from '@angular/common';
import { FeedbackDialogComponent } from 'src/app/components/feedback-dialog/feedback-dialog.component';

@Component({
  selector: 'app-projekt',
  templateUrl: './projekt.component.html',
  styleUrls: ['./projekt.component.scss']
})
export class ProjektComponent implements OnInit, OnChanges {

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
    private dialog: MatDialog,
    private router: Router,
    public config: AppConfiguration
  ) { }

  ngOnInit(): void {
    this.hasRights = this.state.hasRights(this.result.pristupnost, this.result.organizace);
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex =
      `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
       author = {Archeologický informační systém České republiky},
       title = {Záznam ${this.result.ident_cely}},
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       note = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
    
    if (this.inDocument) {
      
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
      this.result.projekt_archeologicky_zaznam = res.projekt_archeologicky_zaznam;
      this.result.projekt_samostatny_nalez = res.projekt_samostatny_nalez;
      this.result.projekt_dokument = res.projekt_dokument;
      this.relationsChecked = true;
      this.related = [];
      res.id_akce.forEach((ident_cely: string) => {
        this.related.push({entity: 'akce', ident_cely})
      });
      res.id_lokalita.forEach((ident_cely: string) => {
        this.related.push({entity: 'lokalita', ident_cely})
      });
      res.projekt_samostatny_nalez.forEach((ident_cely: string) => {
        this.related.push({entity: 'samostatny_nalez', ident_cely})
      });
      res.projekt_dokument.forEach((ident_cely: string) => {
        this.related.push({entity: 'dokument', ident_cely})
      });
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

  formatDate(s: string) {
    // [2023-05-26, 2023-05-26]
    // (d)d.(m)m.rrrr - (d)d.(m)m.rrrr
    let parts = s.replace('[', '').replace(']', '').split(',');
    console.log()
    return this.datePipe.transform(parts[0].trim(), 'd.M.yyyy') + ' - ' + this.datePipe.transform(parts[1].trim(), 'd.M.yyyy');
  }

}
