import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Component, computed, effect, Inject, input, PLATFORM_ID, signal, Signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { Utils } from '../../shared/utils';
import { FileViewerComponent } from '../../components/file-viewer/file-viewer.component';

@Component({
  selector: 'app-entity',
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule
  ],
  templateUrl: './entity.html',
  styleUrl: './entity.scss'
})
export class Entity {

  viewType = input<any>();

  protected _result: any;
  result = input<any>();

  inDocument = input<boolean>(false);
  detailExpanded = input<boolean>(false);
  _detailExpanded: boolean;
  isChild = input<boolean>(false);
  mapDetail = input<boolean>(false);
  isDocumentDialogOpen = input<boolean>(false);
  hasDetail: boolean;

  hasMapRights: boolean;

  imgSrc: string;

  bibTex: string;

  relationsChecked = false;
  related = signal<{ entity: string, ident_cely: string }[]>([]);

  okresy: string[] = [];

  constructor(
    @Inject(PLATFORM_ID) protected platformId: any,
    protected dialog: MatDialog,
    protected router: Router,
    protected datePipe: DatePipe,
    public config: AppConfiguration,
    public state: AppState,
    public service: AppService
  ) {
    effect(() => {
      this._detailExpanded = this.detailExpanded();
      this._result = this.result();
      
      if (this._result) {

        if (this._result.loc_rpt) {
          const coords = this._result.loc_rpt[0].split(',');
          this._result.centroid_e = coords[0];
          this._result.centroid_n = coords[1];
        }
        this.setBibTex();
        this.setImg();
        this.checkRelations();
        this.hasDetail = false;
        this.detailExpanded = this.inDocument;// && !this.mapDetail;
        if (this.mapDetail) {
          this.getFullId();
        }
        this.okres();
      }
    });
  }

  ngOnInit(): void {
    if (!this._result) {
      return;
    }
    this.service.currentLang.subscribe(l => {
      this.setBibTex();
    });
    if (this.inDocument) {
      this.state.documentProgress = 0;
      this.state.loading.set(false);;
    }
  }

  isArray(obj: any) {
    return Array.isArray(obj)
  }

  checkRelations() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    if (!this._result.ident_cely || this.isChild() || (!this.state.isMapaCollapsed && !this.mapDetail())) {
      return;
    }

  }

  imageLoaded() {
    this.state.imagesLoaded++;
    // this.state.imagesLoading =  this.state.imagesLoaded < this.state.numImages;
  }

  setBibTex() {

  }

  toggleDetail() {
    this._detailExpanded = !this._detailExpanded;
    if (!this.hasDetail && !this.inDocument()) {
      this.getFullId();
    }
  }

  getFullId() {
    this.service.getId(this._result.ident_cely).subscribe((res: any) => {
      this.result = res.response.docs[0];
      this.hasDetail = true;
    });
  }

  setImg() {
    if (this._result.soubor_filepath?.length > 0) {
      this._result.soubor.sort((a: any, b: any) => {
        return a.nazev.localeCompare(b.nazev);
      });
      this.imgSrc = this.config.context + '/api/img/thumb?id=' + this._result.soubor[0].id;
    }

  }

  hasValue(field: string): boolean {
    return Utils.hasValue(this._result, field);
  }

  okres() {
    if (this._result.location_info) {
      this._result.location_info.forEach((li: { okres: string; }) => {
        if (!this.okresy.includes(li.okres)) {
          this.okresy.push(li.okres);
        }
      });
    }
  }

  popisObsahu(): string {
    const s: string = this._result.popis;
    return s.replace(/\[new_line\]/gi, '<br/>');
  }

  toggleFav() {
    if (this._result.isFav) {
      this.service.removeFav(this._result.ident_cely).subscribe(res => {
        this._result.isFav = false;
      });
    } else {
      this.service.addFav(this._result.ident_cely).subscribe(res => {
        this._result.isFav = true;
      });
    }
  }

  gotoDoc() {
    this.state.itemView = 'default';
    if (this.state.dialogRef) {
      this.state.dialogRef.close();
    }
    this.router.navigate(['/id', this._result.ident_cely]);
  }

  print() {
    if (this.inDocument) {
      this.service.print();
    } else {
      this.state.printing.set(true);
      this.router.navigate(['/id', this._result.ident_cely]);
    }
  }

  viewFiles() {
    if (this.isChild) {
      this.gotoDoc();
      return;
    }
    const canView = this.state.hasRights(this._result.pristupnost, this._result.dokument_organizace);
    // const canView = true;
    if (canView) {
      this.state.dialogRef = this.dialog.open(FileViewerComponent, {
        panelClass: 'app-file-viewer',
        width: '1000px',
        height: '900px',
        data: this._result
      });
    } else {
      const msg = this.service.getTranslation('alert.insuficient rights');
      alert(msg);
    }
  }
}
