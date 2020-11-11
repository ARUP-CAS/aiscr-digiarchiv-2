import { Utils } from 'src/app/shared/utils';
import { Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { AppConfiguration } from 'src/app/app-configuration';
import { NeidentAkce } from 'src/app/shared/neident-akce';
import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { Lokalita } from 'src/app/shared/lokalita';
import { Akce } from 'src/app/shared/akce';
import { KomponentaDok } from 'src/app/shared/komponenta-dok';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { FileViewerComponent } from 'src/app/components/file-viewer/file-viewer.component';
import { DocumentDialogComponent } from 'src/app/components/document-dialog/document-dialog.component';

@Component({
  selector: 'app-knihovna3d',
  templateUrl: './knihovna3d.component.html',
  styleUrls: ['./knihovna3d.component.scss']
})
export class Knihovna3dComponent implements OnInit {
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

  constructor(
    private dialog: MatDialog,
    private router: Router,
    public config: AppConfiguration,
    public state: AppState,
    public service: AppService
  ) { }

  ngOnInit(): void {
    if (this.result.soubor_filepath?.length > 0) {
      this.imgSrc = this.config.context + '/api/img?id=' + this.result.soubor_filepath[0];
    }

    const autor = this.result.autor.join(' and ');
    this.bibTex = `@misc{
      https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
      author = “${autor}”, 
      title = “Dokument ${this.result.ident_cely}”,
      howpublished = “\\url{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}}”,
      year = ${this.result.rok_vzniku}, 
      note = “${this.result.organizace}”
    }`;
  }

  toggleDetail() {
    this.detailExpanded = !this.detailExpanded;
    if (!this.hasDetail && !this.inDocument) {
      this.service.getId(this.result.ident_cely).subscribe((res: any) => {
        this.result.akce = res.response.docs[0].akce;
        this.result.lokalita = res.response.docs[0].lokalita;
        this.hasDetail = true;
      });
    }
  }

  setImg() {
    if (this.result.soubor_filepath?.length > 0) {
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

        const o = org ? this.service.getHeslarTranslation(org, 'field.f_organizace') : '';
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
        width: '755px',
        data: this.result
      });
    } else {
      const msg = this.service.getTranslation('insuficient rights');
      alert(msg);
    }
  }

  testData(val: string) {
    if (val === 'neidentifikovana_akce') {
      if (this.result.neident_akce_katastr || this.result.neident_akce_okres || this.result.neident_akce_vedouci ||
        this.result.neident_akce_rok_zahajeni || this.result.neident_akce_rok_ukonceni ||
        this.result.neident_akce_lokalizace || this.result.neident_akce_popis ||
        this.result.neident_akce_poznamka || this.result.neident_akce_pian) {
        return true;
      }
    } else if (val === 'souvisejici_zaznamy') {
      if (this.result.jednotka_dokumentu_vazba_akce || this.result.jednotka_dokumentu_vazba_druha_akce ||
        this.result.jednotka_dokumentu_vazba_lokalita || this.result.jednotka_dokumentu_vazba_druha_lokalita) {
        return true;
      }
    }
  }

  openDocument() {
    this.state.dialogRef = this.dialog.open(DocumentDialogComponent, {
      width: '900px',
      data: this.result,
      panelClass: 'app-document-dialog'
    });
  }

}
