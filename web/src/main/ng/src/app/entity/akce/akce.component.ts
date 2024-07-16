import { AppService } from 'src/app/app.service';
import { Component, OnInit, Input, OnChanges, Inject, PLATFORM_ID } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { DocumentDialogComponent } from 'src/app/components/document-dialog/document-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { DatePipe, isPlatformBrowser } from '@angular/common';
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
  @Input() mapDetail: boolean = false;
  @Input() isDocumentDialogOpen: boolean;
  @Input() inDocument = false;
  hasDetail: boolean;
  bibTex: string;

  math = Math;
  itemSize = 133;
  vsSize = 0;
  numChildren = 0;
  dokLoaded = 0;

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
    const sd = new Date(this.result.specifikace_data);
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex =
      `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
       author = {AMČR},
       title = {Záznam ${this.result.ident_cely}},
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       note = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
    this.result.dokumentTemp = [];
    if (this.inDocument) {
      this.setVsize();
      this.state.documentProgress = 0;
      if (isPlatformBrowser(this.platformId)) {
        setTimeout(() => {
          this.state.loading = true;
          this.state.imagesLoading = true;
          this.result.valid_dokument = [];
          this.result.valid_projekt = [];
          this.getDokuments();
          this.getProjekts();
        }, 100);
      }
    }  
  }

  ngOnChanges(c) {
    if (c.result) {
      this.hasDetail = false;
      this.result.dokumentTemp = [];
      this.detailExpanded = this.inDocument;
    }
    if (this.mapDetail) {
      this.getFullId();
    }
  }

  setVsize() {
      if (this.result.az_dokument) {
        this.numChildren += this.result.az_dokument.length;
        this.state.numImages = this.result.az_dokument.length;
      }
      if (this.result.akce_projekt) {
        this.numChildren += 1;
      }
      this.state.numChildren = this.numChildren;
      this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
  }

  checkLoading() {
    this.state.loading = (this.dokLoaded + this.result.valid_projekt.length) < this.numChildren;
    if (!this.state.loading) {
      this.result.valid_dokument = this.result.dokumentTemp.concat([]);
    }
  }


  getDokuments() {
    if (this.result.az_dokument) {
      for (let i = 0; i < this.result.az_dokument.length; i++) {
        this.result.dokumentTemp.push({});
      }
      for (let i = 0; i < this.result.az_dokument.length; i = i + 20) {
        const ids = this.result.az_dokument.slice(i, i + 20);
        this.service.getIdAsChild(ids, "dokument").subscribe((res: any) => {
          // Odpoved ma jine serazeni.
          const sorted: any[] = [];
          ids.forEach(id => {
            const doc = res.response.docs.find(d => d.ident_cely === id);
            // sorted.push(doc);
            const idx = this.result.az_dokument.findIndex(d => d === id);
            this.result.dokumentTemp[idx] = doc;
            this.dokLoaded++;
          });
          this.state.documentProgress = this.dokLoaded / this.numChildren * 100;
          this.checkLoading();

        });
      }
    }
    this.state.loading = false;
  }

  getProjekts() {
    if (this.result.akce_projekt) {
        this.service.getIdAsChild([this.result.akce_projekt], "projekt").subscribe((res: any) => {
          this.result.valid_projekt = res.response.docs;
          this.checkLoading();
        });
    }
  }

  getFullId() {
    //this.service.getId(this.result.ident_cely).subscribe((res: any) => {
      //this.result = res.response.docs[0];
      this.setVsize();
      this.result.valid_dokument = [];
      this.result.dokumentTemp = [];
      this.result.valid_projekt = [];
      this.getDokuments();
      this.getProjekts();
      // this.result.akce = res.response.docs[0].akce;
      // this.result.lokalita = res.response.docs[0].lokalita;
      this.hasDetail = true;
    //});
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
