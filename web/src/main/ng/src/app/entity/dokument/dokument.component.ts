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
import { Console } from 'console';

@Component({
  selector: 'app-dokument',
  templateUrl: './dokument.component.html',
  styleUrls: ['./dokument.component.scss']
})
export class DokumentComponent implements OnInit, OnChanges {

  private _result: any;

  @Input() set result(value: any) {
    this._result = value;
    if (this._result) {
      this.setImg();
    }
    
  }

  get result(): any {
    return this._result;
  }

  // @Input() result;

  @Input() inDocument = false;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() mapDetail: boolean;
  @Input() isDocumentDialogOpen: boolean;
  hasDetail: boolean;
  
  hasMapRights: boolean;

  imgSrc: string;

  bibTex: string;

  relationsChecked = false;
  related: {entity: string, ident_cely: string}[] = [];

  okresy: string[] = [];

  constructor(
    private dialog: MatDialog,
    private router: Router,
    public config: AppConfiguration,
    public state: AppState,
    public service: AppService
  ) { }

  ngOnChanges(c) {
    if (c.result) {
      this.checkRelations();
      this.hasDetail = false;
      this.detailExpanded = this.inDocument;// && !this.mapDetail;
      if (this.mapDetail) {
        this.getFullId();
      }
    }
  }

  ngOnInit(): void {
    if (!this.result) {
      return;
    }
    if (this.result.location_info) {
      this.result.location_info.forEach(li => {
        if (!this.okresy.includes(li.okres)) {
          this.okresy.push(li.okres);
        }
      });
    }
    if (this.result.pian) {
      this.result.pian.forEach(pian => {
        this.hasMapRights = this.hasMapRights || this.state.hasRights(pian.pristupnost, this.result.organizace);
      });
    }

    

    if (this.result.soubor_filepath?.length > 0) {
      this.result.soubor.sort((a: any, b: any) => {
        return a.nazev.localeCompare(b.nazev);
      });
      this.imgSrc = this.config.context + '/api/img/thumb?id=' + this.result.soubor[0].id;
    }
    this.setBibTex();
    this.service.currentLang.subscribe(l => {
      this.setBibTex();
    });
    if (this.inDocument) {
      this.state.documentProgress = 0;
      this.state.loading = false;
    }
  }

  isArray(obj : any ) {
    return Array.isArray(obj)
 }

 checkRelations() {
  if (!this.result.ident_cely || this.isChild || (!this.state.isMapaCollapsed && !this.mapDetail)) {
    return;
  }
  this.service.checkRelations(this.result.ident_cely).subscribe((res: any) => {
    this.result.dokument_cast_archeologicky_zaznam = res.dokument_cast_archeologicky_zaznam;
    this.result.dokument_cast_projekt = res.dokument_cast_projekt;
    this.relationsChecked = true;
    this.related = [];
    res.id_akce.forEach((ident_cely: string) => {
      this.related.push({entity: 'akce', ident_cely})
    });
    res.id_lokalita.forEach((ident_cely: string) => {
      this.related.push({entity: 'lokalita', ident_cely})
    });
    res.dokument_cast_projekt.forEach((ident_cely: string) => {
      this.related.push({entity: 'projekt', ident_cely})
    });
  });
}

  imageLoaded() {
    this.state.imagesLoaded++;
    // this.state.imagesLoading =  this.state.imagesLoaded < this.state.numImages;
  }

  setBibTex() {
    const organizace = this.service.getHeslarTranslation(this.result.dokument_organizace, 'organizace');
    const autor = this.result.dokument_autor ? this.result.dokument_autor.join(' and ') : '';
    this.bibTex = `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
      author = {${autor}}, 
      title = {Dokument ${this.result.ident_cely}},
      howpublished = url{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
      year = {${this.result.dokument_rok_vzniku}},
      note = {${organizace}},
      doi = {${this.result.dokument_doi}}
    }`;
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
      if (this.result.dokument_cast) {
        this.result.dokument_cast.sort((dc1, dc2) => dc1.ident_cely.localeCompare(dc2.ident_cely) );
      }
      // this.setVsize();
      // this.getArchZaznam();
      // this.getProjekts();
      this.hasDetail = true;
    });
  }

  setImg() {
    if (this.result.soubor_filepath?.length > 0) {
      this.result.soubor.sort((a: any, b: any) => {
        return a.nazev.localeCompare(b.nazev);
      });
      this.imgSrc = this.config.context + '/api/img/thumb?id=' + this.result.soubor[0].id;
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
    if (this.isChild) {
      this.gotoDoc();
      return;
    }
    const canView = this.state.hasRights(this.result.pristupnost, this.result.dokument_organizace);
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
