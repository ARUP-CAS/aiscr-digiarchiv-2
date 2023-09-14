import { Component, OnInit, Input, OnChanges } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { AppService } from 'src/app/app.service';
import { MatDialog } from '@angular/material/dialog';
import { DocumentDialogComponent } from 'src/app/components/document-dialog/document-dialog.component';
import { Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { DatePipe } from '@angular/common';
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

  itemSize = 133;
  vsSize = 0;
  numChildren = 0;
  math = Math;

  constructor(
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
       author = {AMČR},
       title = {Záznam ${this.result.ident_cely}},
       url = {https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       publisher = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
     this.setVsize();
     if (this.inDocument) {
       this.state.loading = false;
       this.state.documentProgress = 0;
       this.getAkce();
       this.getSamostatnyNalez();
     }
     // this.checkRelations();
  } 

  checkRelations() {
    this.service.checkRelations(this.result.ident_cely).subscribe((res: any) => {
      this.result.samostatny_nalez = res.samostatny_nalez;
    });
  }

  setVsize() {

    if (this.result.archeologicky_zaznam) {
      this.numChildren += this.result.archeologicky_zaznam.length;
    }
    if (this.result.samostatny_nalez) {
      this.numChildren += this.result.samostatny_nalez.length;
    }
    this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
  }

  getAkce() {
    this.result.akce = [];
    console.log(this.result.archeologicky_zaznam,this.hasRights)
    if (this.result.archeologicky_zaznam && this.hasRights) {
      for (let i = 0; i < this.result.archeologicky_zaznam.length; i=i+10) {
        const ids = this.result.archeologicky_zaznam.slice(i, i+10);
        this.service.getIdAsChild(ids, "akce").subscribe((res: any) => {
          this.result.akce = this.result.akce.concat(res.response.docs);
          this.numChildren = this.numChildren - ids.length + res.response.docs.length;
          this.state.documentProgress = (this.result.akce.length + this.result.samostatny_nalez.length) / this.numChildren *100;
          this.state.loading = (this.result.akce.length + this.result.samostatny_nalez.length) < this.numChildren;

        });
      }
    }
  }

  getSamostatnyNalez() {
    this.result.valid_samostatny_nalez = [];
    if (this.result.samostatny_nalez && this.hasRights) {
      for (let i = 0; i < this.result.samostatny_nalez.length; i=i+10) {
        const ids = this.result.samostatny_nalez.slice(i, i+10);
        this.service.getIdAsChild(ids, "samostatny_nalez").subscribe((res: any) => {
          this.result.valid_samostatny_nalez = this.result.valid_samostatny_nalez.concat(res.response.docs);
          if (res.response.docs.length < 10) {
            // To znamena, ze v indexu nejsou zaznamy odkazovane. Snizime pocet 
            this.numChildren = this.numChildren - 10 + res.response.docs.length; 
            this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
          }
          this.state.documentProgress = (this.result.akce.length + this.result.valid_samostatny_nalez.length) / this.numChildren *100;
          this.state.loading = (this.result.akce.length + this.result.valid_samostatny_nalez.length) < this.numChildren;
          if (!this.state.loading) {
            this.result.valid_samostatny_nalez.sort((a:any, b:any) => a.ident_cely.localeCompare(b.ident_cely))
          }
        });
      }
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

  getFullId() {
    this.service.getId(this.result.ident_cely).subscribe((res: any) => {
      this.result = res.response.docs[0];
      this.state.loading = true;
      this.state.documentProgress = 0;
      this.getAkce();
      this.getSamostatnyNalez();
      this.hasDetail = true;
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
