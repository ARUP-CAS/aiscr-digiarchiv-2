import { Utils } from 'src/app/shared/utils';
import { Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { AppConfiguration } from 'src/app/app-configuration';
import { Component, OnInit, Input, OnChanges } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { FileViewerComponent } from 'src/app/components/file-viewer/file-viewer.component';
import { DocumentDialogComponent } from 'src/app/components/document-dialog/document-dialog.component';
import { FeedbackDialogComponent } from 'src/app/components/feedback-dialog/feedback-dialog.component';

@Component({
  selector: 'app-knihovna3d',
  templateUrl: './knihovna3d.component.html',
  styleUrls: ['./knihovna3d.component.scss']
})
export class Knihovna3dComponent implements OnInit, OnChanges {
  @Input() result;
  @Input() highlighting;
  @Input() inModal = false;
  @Input() inDocument = false;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() mapDetail: boolean;
  @Input() isDocumentDialogOpen: boolean;

  hasDetail: boolean;

  imgSrc: string;
  bibTex: string;

  itemSize = 133;
  vsSize = 0;
  numChildren = 0;
  math = Math;

  constructor(
    private dialog: MatDialog,
    private router: Router,
    public config: AppConfiguration,
    public state: AppState,
    public service: AppService
  ) { }

  ngOnInit(): void {
    if (this.result.soubor_filepath?.length > 0) {
      this.result.soubor.sort((a: any, b: any) => {
        return a.nazev.localeCompare(b.nazev);
      });
      this.imgSrc = this.config.context + '/api/img?id=' + this.result.soubor_filepath[0];
    }

    this.setBibTex();
    this.service.currentLang.subscribe(l => {
      this.setBibTex();
    });
    this.setVsize();
    if (this.inDocument) {
      this.state.loading = true;
      this.state.documentProgress = 0;
      this.getArchZaznam();
    }
  }

  getArchZaznam() {
    this.result.akce = [];
    this.result.lokalita = [];
    if (this.result.dokument_cast_akce) {
      for (let i = 0; i < this.result.dokument_cast_akce.length; i = i + 10) {
        const ids = this.result.dokument_cast_akce.slice(i, i + 10);
        this.service.getIdAsChild(ids, "akce").subscribe((res: any) => {
          this.result.akce = this.result.akce.concat(res.response.docs.filter(d => d.entity === 'akce'));
          this.result.lokalita = this.result.lokalita.concat(res.response.docs.filter(d => d.entity === 'lokalita'));
          this.numChildren = this.numChildren - ids.length + res.response.docs.length;
          this.state.documentProgress = (this.result.akce.length + this.result.lokalita.length) / this.numChildren * 100;
          this.state.loading = (this.result.akce.length + this.result.lokalita.length) < this.numChildren;
        });
      }
    }
    if (this.result.dokument_cast_lokalita) {
      for (let i = 0; i < this.result.dokument_cast_lokalita.length; i = i + 10) {
        const ids = this.result.dokument_cast_lokalita.slice(i, i + 10);
        this.service.getIdAsChild(ids, "lokalita").subscribe((res: any) => {
          this.result.akce = this.result.akce.concat(res.response.docs.filter(d => d.entity === 'akce'));
          this.result.lokalita = this.result.lokalita.concat(res.response.docs.filter(d => d.entity === 'lokalita'));
          this.numChildren = this.numChildren - ids.length + res.response.docs.length;
          this.state.documentProgress = (this.result.akce.length + this.result.lokalita.length) / this.numChildren * 100;
          this.state.loading = (this.result.akce.length + this.result.lokalita.length) < this.numChildren;
        });
      }
    }
  }

  setVsize() {

    if (this.result.jednotka_dokumentu_vazba_akce) {
      this.numChildren += this.result.jednotka_dokumentu_vazba_akce.length;
    }
    if (this.result.jednotka_dokumentu_vazba_druha_akce) {
      this.numChildren += this.result.jednotka_dokumentu_vazba_druha_akce.length;
    }
    if (this.result.jednotka_dokumentu_vazba_lokalita) {
      this.numChildren += this.result.jednotka_dokumentu_vazba_lokalita.length;
    }
    if (this.result.jednotka_dokumentu_vazba_druha_lokalita) {
      this.numChildren += this.result.jednotka_dokumentu_vazba_druha_lokalita.length;
    }
    this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
  }

  

  setBibTex() {
    const organizace = this.service.getHeslarTranslation(this.result.organizace, 'organizace');
    const autor = this.result.autor ? this.result.autor.join(' and ') : '';
    this.bibTex = `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
      author = {${autor}}, 
      title = {Dokument ${this.result.ident_cely}},
      url = {https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
      year = {${this.result.rok_vzniku}},
      note = {${organizace}}
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


  toggleDetail() {
    this.detailExpanded = !this.detailExpanded;
    if (!this.hasDetail && !this.inDocument) {
      this.getFullId();
    }
  }

  getFullId() {
    this.service.getId(this.result.ident_cely).subscribe((res: any) => {
      this.result = res.response.docs[0];
      this.getArchZaznam();
      this.hasDetail = true;
    });
  }

  setImg() {
    if (this.result.soubor_filepath?.length > 0) {
      this.result.soubor.sort((a: any, b: any) => {
        return a.nazev.localeCompare(b.nazev);
      });
      this.imgSrc = this.config.context + '/api/img?id=' + this.result.soubor_filepath[0];
    }

  }

  hasValue(field: string): boolean {
    return Utils.hasValue(this.result, field);
  }

  okres() {
    if (this.result.hasOwnProperty('f_okres')) {
      const okresy = [];
      const katastry = [];
      let ret = '';
      for (let idx = 0; idx < this.result.f_okres.length; idx++) {
        const okres = this.result.f_okres[idx];
        const katastr = this.result.f_katastr[idx];

        if (katastry.indexOf(katastr) < 0) {
          okresy.push(okres);
          katastry.push(katastr);
          if (idx > 0) {
            ret += ', ';
          }
          ret += katastr + ' (' + okres + ')';
        }
      }
      return ret;
    } else {
      return '';
    }
  }

  organizace() {
    if (this.result.hasOwnProperty('organizace')) {
      const os = [];
      let ret = '';
      for (let idx = 0; idx < this.result.organizace.length; idx++) {
        let org = this.result.organizace[idx];
        if (org) {
          org = org.trim();
        }

        const o = org ? this.service.getHeslarTranslation(org, 'card.field.f_organizace') : '';
        if ((o !== '') && (os.indexOf(o) < 0)) {
          os.push(o);

          if (idx > 0) {
            ret += ', ';
          }
          ret += o;
        }

      }
      return ret;
    } else {
      return '';
    }
  }

  popisObsahu(): string {
    const s: string = this.result.popis;
    return s.replace(/\[new_line\]/gi, '<br/>');
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
      this.state.printing = true;
      this.router.navigate(['/id', this.result.ident_cely]);
    }
  }

  viewFiles() {
    const canView = this.state.hasRights(this.result.pristupnost, this.result.organizace);
    // const canView = true;
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

  testData(val: string) {
    if (val === 'souvisejici_zaznamy') {
      if (this.result.jednotka_dokumentu_vazba_akce || this.result.jednotka_dokumentu_vazba_druha_akce ||
        this.result.jednotka_dokumentu_vazba_lokalita || this.result.jednotka_dokumentu_vazba_druha_lokalita) {
        return true;
      }
    }
    return false;
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
