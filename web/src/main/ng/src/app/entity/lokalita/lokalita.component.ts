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

  relationsChecked = false;
  related: {entity: string, ident_cely: string}[] = [];

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
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       note = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
     if (this.inDocument) {
      this.checkRelations();
      this.state.documentProgress = 0;
      this.state.loading = true;
      this.getExtZdroj();
     }
  }

  checkRelations() {
    this.service.checkRelations(this.result.ident_cely).subscribe((res: any) => {
      this.result.az_dokument = res.az_dokument;
      this.result.akce_projekt = res.akce_projekt;
      this.relationsChecked = true;
      this.related = [];
      res.az_dokument.forEach((ident_cely: string) => {
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
      this.getFullId();
    }
  }

  getExtZdroj() {
    if (this.result.az_ext_zdroj) {
      for (let i = 0; i < this.result.az_ext_zdroj.length; i = i + 20) {
        const ids = this.result.az_ext_zdroj.slice(i, i + 20);
        this.service.getIdAsChild(ids, "ext_zdroj").subscribe((res: any) => {
          this.result.az_ext_zdroj = [];
          this.result.az_ext_odkaz.forEach(eo => {
            const ez = res.response.docs.find(ez => eo.ext_zdroj.id === ez.ident_cely);
            ez.ext_odkaz_paginace = eo.paginace;
            this.result.az_ext_zdroj.push(ez);
          });
          this.result.az_ext_zdroj.sort((ez1, ez2) => {
            let res = 0;
            res = ez1.ext_zdroj_autor[0].localeCompare(ez2.ext_zdroj_autor[0], 'cs');
            if (res === 0) {
              res = ez1.ext_zdroj_rok_vydani_vzniku = ez2.ext_zdroj_rok_vydani_vzniku;
            }
            if (res === 0) {
              res = ez1.ext_zdroj_nazev.localeCompare(ez2.ext_zdroj_nazev);
            }
            return res;
          });
        });
      }
    }
  }

  getFullId() {
    this.service.getId(this.result.ident_cely).subscribe((res: any) => {
      this.result = res.response.docs[0];
      // this.getDokuments();
      // this.getExtZdroj();
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
