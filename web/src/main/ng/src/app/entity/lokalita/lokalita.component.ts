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
  selector: 'app-lokalita',
  templateUrl: './lokalita.component.html',
  styleUrls: ['./lokalita.component.scss']
})
export class LokalitaComponent implements OnInit, OnChanges {

  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() mapDetail: boolean;
  @Input() isDocumentDialogOpen: boolean;
  @Input() inDocument = false;
  // hasRights: boolean;
  hasDetail: boolean;
  bibTex: string;

  math = Math;

  itemSize = 133;
  vsSize = 0;
  numChildren = 0;

  constructor(
    private datePipe: DatePipe,
    public service: AppService,
    public state: AppState,
    private dialog: MatDialog,
    private router: Router,
    public config: AppConfiguration
  ) { }

  ngOnInit(): void {
    // this.hasRights = this.state.hasRights(this.result.pristupnost, this.result.organizace);
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex =
     `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
       author = {AMČR}, 
       title = {Záznam ${this.result.ident_cely}},
       url = {https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       publisher = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
     if (this.inDocument) {
      this.setVsize();
      this.state.documentProgress = 0;
      this.state.loading = true;
      this.getDokuments();
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

  setVsize() {
      if (this.result.dokument) {
        this.numChildren += this.result.dokument.length;
      }
      this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
  }

  getDokuments() {
    if (this.result.dokument && this.result.dokument.length > 0) {
      this.result.valid_dokument = [];
      for (let i = 0; i < this.result.dokument.length; i=i+10) {
        const ids = this.result.dokument.slice(i, i+10);
        this.service.getIdAsChild(ids, "dokument").subscribe((res: any) => {
          this.result.valid_dokument = this.result.valid_dokument.concat(res.response.docs);
          this.state.documentProgress = this.result.valid_dokument.length / this.numChildren *100;
          this.state.loading = (this.result.valid_dokument.length) < this.numChildren;
        });
      }
    }
    this.state.loading = false;
  }

  getFullId() {
    this.service.getId(this.result.ident_cely).subscribe((res: any) => {
      this.result = res.response.docs[0];
      this.setVsize();
      this.getDokuments();
      this.hasDetail = true;
    });
  }

  toggleDetail() {
    if (!this.hasDetail && !this.inDocument) {
      this.getFullId();
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
