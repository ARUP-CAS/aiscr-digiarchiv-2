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
  relationsChecked = false;

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
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       note = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
    this.result.sn_toprocess = [];
    
    this.checkRelations();
    // this.setVsize();
    if (this.inDocument) {
      this.state.loading = false;
      this.state.documentProgress = 0;

      this.result.valid_samostatny_nalez = [];
      this.result.akce = [];
      this.result.lokalita = [];
      if (this.result.projekt_samostatny_nalez) {
        this.result.sn_toprocess = JSON.parse(JSON.stringify(this.result.projekt_samostatny_nalez));
      }
      
      this.result.sn_toprocess.sort();
      this.getArchZaznam();
      this.getSamostatnyNalez(false);
      this.getDokument();
    }
  }

  checkRelations() {
    this.service.checkRelations(this.result.ident_cely).subscribe((res: any) => {
      this.result.projekt_archeologicky_zaznam = res.projekt_archeologicky_zaznam;
      this.result.projekt_samostatny_nalez = res.projekt_samostatny_nalez;
      this.result.projekt_dokument = res.projekt_dokument;
      this.relationsChecked = true;
      this.setVsize();
    });
  }

  setVsize() {
    this.numChildren = 0;
    if (this.result.projekt_archeologicky_zaznam) {
      this.numChildren += this.result.projekt_archeologicky_zaznam.length;
    }
    if (this.result.projekt_samostatny_nalez) {
      this.numChildren += this.result.projekt_samostatny_nalez.length;
    }
    if (this.result.projekt_dokument) {
      this.numChildren += this.result.projekt_dokument.length;
    }
    this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
  }

  getDokument() {
    this.result.valid_projekt_dokument = [];
    if (this.result.projekt_dokument) {
      for (let i = 0; i < this.result.projekt_dokument.length; i = i + 10) {
        const ids = this.result.projekt_dokument.slice(i, i + 10);
        this.service.getIdAsChild(ids, "dokument").subscribe((res: any) => {
          this.result.valid_projekt_dokument = this.result.valid_projekt_dokument.concat(res.response.docs);
          if (res.response.docs.length < ids.length) {
            // To znamena, ze v indexu nejsou zaznamy odkazovane. Snizime pocet 
            this.numChildren = this.numChildren - ids.length + res.response.docs.length;
            this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
          }
          this.state.documentProgress = (this.result.akce.length + this.result.valid_samostatny_nalez.length + this.result.valid_projekt_dokument.length) / this.numChildren * 100;
          this.state.loading = (this.result.akce.length + this.result.valid_samostatny_nalez.length + this.result.valid_projekt_dokument.length) < this.numChildren;
          if (!this.state.loading) {
            this.result.valid_projekt_dokument.sort((a: any, b: any) => a.ident_cely.localeCompare(b.ident_cely))
          }
        });
      }
    }
  }

  getSamostatnyNalez(loadAll: boolean) {

    const idsSize = 20;
    if (this.result.sn_toprocess && this.result.sn_toprocess.length > 0) {
      const ids = this.result.sn_toprocess.splice(0, idsSize);
      this.service.getIdAsChild(ids, "samostatny_nalez").subscribe((res: any) => {
        this.result.valid_samostatny_nalez = this.result.valid_samostatny_nalez.concat(res.response.docs);
        if (res.response.docs.length < ids.length) {
          // To znamena, ze v indexu nejsou zaznamy odkazovane. Snizime pocet 
          this.numChildren = this.numChildren - ids.length + res.response.docs.length;
          this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
        }
        this.state.documentProgress = (this.result.akce.length + this.result.valid_samostatny_nalez.length + this.result.valid_projekt_dokument.length) / this.numChildren * 100;
        this.state.loading = (this.result.akce.length + this.result.valid_samostatny_nalez.length + this.result.valid_projekt_dokument.length) < this.numChildren;

        if (loadAll && this.state.loading) {
          this.getSamostatnyNalez(loadAll)
        }
      });
    }
  }

  getSamostatnyNalezAll() {
    this.result.valid_samostatny_nalez = [];
    const idsSize = 20;
    if (this.result.projekt_samostatny_nalez) {
      this.result.projekt_samostatny_nalez.sort();
      for (let i = 0; i < this.result.projekt_samostatny_nalez.length; i = i + idsSize) {
        const ids = this.result.projekt_samostatny_nalez.slice(i, i + idsSize);
        this.service.getIdAsChild(ids, "samostatny_nalez").subscribe((res: any) => {
          this.result.valid_samostatny_nalez = this.result.valid_samostatny_nalez.concat(res.response.docs);
          if (res.response.docs.length < ids.length) {
            // To znamena, ze v indexu nejsou zaznamy odkazovane. Snizime pocet 
            this.numChildren = this.numChildren - ids.length + res.response.docs.length;
            this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
          }
          this.state.documentProgress = (this.result.akce.length + this.result.valid_samostatny_nalez.length + this.result.valid_projekt_dokument.length) / this.numChildren * 100;
          this.state.loading = (this.result.akce.length + this.result.valid_samostatny_nalez.length + this.result.valid_projekt_dokument.length) < this.numChildren;
          if (!this.state.loading) {
            this.result.valid_samostatny_nalez.sort((a: any, b: any) => a.ident_cely.localeCompare(b.ident_cely))
          }
        });
        return;
      }
    }
  }


  ngOnChanges(c) {
    if (c.result) {
      this.setVsize();
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

      this.state.loading = (this.result.projekt_archeologicky_zaznam.length + this.result.projekt_samostatny_nalez.length) < this.numChildren;
      this.state.documentProgress = 0;
      this.result.valid_samostatny_nalez = [];
      this.result.akce = [];
      this.result.lokalita = [];
      if (this.result.projekt_samostatny_nalez) {
        this.result.sn_toprocess = JSON.parse(JSON.stringify(this.result.projekt_samostatny_nalez));
      }
      this.setVsize();
      this.getArchZaznam();
      this.getSamostatnyNalez(false);
      this.getDokument();
      this.hasDetail = true;
    });
  }

  loadAll() {

    this.getArchZaznam();
    this.getSamostatnyNalez(true);
    this.getDokument();
  }

  getArchZaznam() {
    if (this.result.id_akce) {
      for (let i = 0; i < this.result.id_akce.length; i = i + 10) {
        const ids = this.result.id_akce.slice(i, i + 10);
        this.service.getIdAsChild(ids, "akce").subscribe((res: any) => {
          this.result.akce = this.result.akce.concat(res.response.docs.filter(d => d.entity === 'akce'));
          this.result.lokalita = this.result.lokalita.concat(res.response.docs.filter(d => d.entity === 'lokalita'));
          this.numChildren = this.numChildren - ids.length + res.response.docs.length;
          this.state.documentProgress = (this.result.akce.length + this.result.valid_samostatny_nalez.length + this.result.valid_projekt_dokument.length) / this.numChildren * 100;
          this.state.loading = (this.result.akce.length + this.result.valid_samostatny_nalez.length + this.result.valid_projekt_dokument.length) < this.numChildren;
        });
      }
    }
    if (this.result.id_lokalita) {
      for (let i = 0; i < this.result.id_lokalita.length; i = i + 10) {
        const ids = this.result.id_lokalita.slice(i, i + 10);
        this.service.getIdAsChild(ids, "lokalita").subscribe((res: any) => {
          this.result.akce = this.result.akce.concat(res.response.docs.filter(d => d.entity === 'akce'));
          this.result.lokalita = this.result.lokalita.concat(res.response.docs.filter(d => d.entity === 'lokalita'));
          this.numChildren = this.numChildren - ids.length + res.response.docs.length;
          this.state.documentProgress = (this.result.akce.length + this.result.valid_samostatny_nalez.length + this.result.valid_projekt_dokument.length) / this.numChildren * 100;
          this.state.loading = (this.result.akce.length + this.result.valid_samostatny_nalez.length + this.result.valid_projekt_dokument.length) < this.numChildren;
        });
      }
    }
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
