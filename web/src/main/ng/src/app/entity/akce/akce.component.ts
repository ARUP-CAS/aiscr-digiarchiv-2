import { AppService } from 'src/app/app.service';
import { Component, OnInit, Input, OnChanges } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { DocumentDialogComponent } from 'src/app/components/document-dialog/document-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { DatePipe } from '@angular/common';
import { FeedbackDialogComponent } from 'src/app/components/feedback-dialog/feedback-dialog.component';

@Component({
  selector: 'app-akce',
  templateUrl: './akce.component.html',
  styleUrls: ['./akce.component.scss']
})
export class AkceComponent implements OnInit, OnChanges {

  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() mapDetail: boolean;
  @Input() isDocumentDialogOpen: boolean;
  @Input() inDocument = false;
  hasRights: boolean;
  hasDetail: boolean;
  bibTex: string;

  math = Math;

  itemSize = 133;
  vsSize = 0;
  numChildren = 0;

  constructor(
    private datePipe: DatePipe,
    public state: AppState,
    public service: AppService,
    private dialog: MatDialog,
    private router: Router,
    public config: AppConfiguration
  ) {}

  ngOnInit(): void {
    this.hasRights = this.state.hasRights(this.result.pristupnost, this.result.organizace);
    const sd = new Date(this.result.specifikace_data);
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex =
     `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
       author = {AMČR},
       title = {Záznam ${this.result.ident_cely}},
       url = {https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       publisher = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
     if (this.inDocument) {
      if (this.result.child_dokument) {
        this.numChildren += this.result.child_dokument.length;
      }
      if (this.result.vazba_projekt) {
        this.numChildren += this.result.vazba_projekt.length;
      }
      this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
      this.getDokuments();
      this.getProjekts();
     }
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

  getDokuments() {
    if (this.result.child_dokument) {
      this.result.dokument = [];
      this.result.child_dokument.forEach(id => {
        this.service.getIdAsChild(id, "dokument").subscribe((res: any) => {
          this.result.dokument.push(res.response.docs[0]);
        });
      });
    }
  }

  getProjekts() {
    if (this.result.vazba_projekt) {
      this.result.projekt = [];
      this.result.vazba_projekt.forEach(id => {
        this.service.getIdAsChild(id, "projekt").subscribe((res: any) => {
          this.result.projekt.push(res.response.docs[0]);
        });
      });
    }
  }

  getFullId() {
    this.service.getId(this.result.ident_cely).subscribe((res: any) => {
      this.result = res.response.docs[0];
      if (this.result.child_dokument) {
        this.numChildren += this.result.child_dokument.length;
      }
      if (this.result.vazba_projekt) {
        this.numChildren += this.result.vazba_projekt.length;
      }
      this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
      this.getDokuments();
      this.getProjekts();
      // this.result.akce = res.response.docs[0].akce;
      // this.result.lokalita = res.response.docs[0].lokalita;
      this.hasDetail = true;
    });
  }

  toggleDetail() {
    if (!this.hasDetail && !this.inDocument) {
      this.service.getId(this.result.ident_cely).subscribe((res: any) => {
        this.getFullId();
      });
    }
    this.detailExpanded = !this.detailExpanded;
  }

  cropped(s: string) {
    if (s.length > 100) {
      return s.slice(0, 100) + '(...)'
    } else {
      return s;
    }
  }

  print() {
    if (this.inDocument) {
      this.service.print();
    } else {
      this.state.printing = true;
      this.router.navigate(['/id', this.result.ident_cely]);
    }
  }

  pian(id: string) {
    return this.result.pian.filter(p => p.ident_cely === id);
  }

  adb(id: string) {
    return this.result.adb.filter(p => p.ident_cely === id);
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
