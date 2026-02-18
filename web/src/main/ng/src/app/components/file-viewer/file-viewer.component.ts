import { Component, OnInit, Inject, ViewChild, PLATFORM_ID, signal } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
//import { NguCarousel, NguCarouselConfig } from '@ngu/carousel';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { AppWindowRef } from '../../app.window-ref';
import { SolrDocument } from '../../shared/solr-document';
import { File } from '../../shared/file';
import {
  NguCarousel, 
  NguCarouselConfig, 
  NguCarouselDefDirective,
  // NguCarouselNextDirective,
  // NguCarouselPrevDirective,
  // NguItemComponent
} from '@ngu/carousel';
import { LicenseDialogComponent } from '../license-dialog/license-dialog.component';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';


@Component({
  imports: [
    TranslateModule, RouterModule,  FormsModule, MatDialogModule,
    MatCardModule, MatIconModule,  MatButtonModule, MatFormFieldModule, MatInputModule,
    MatProgressBarModule, MatTooltipModule, MatListModule, MatSelectModule,
    NguCarousel,
    NguCarouselDefDirective
  ],
  selector: 'app-file-viewer',
  templateUrl: './file-viewer.component.html',
  styleUrls: ['./file-viewer.component.scss']
})
export class FileViewerComponent implements OnInit {

  showing = signal(false);
  // rolling = false;
  // result: any;

  files = signal<File[]>([]);
  selectedFile = signal<File>(null); 

  currentPage: number = 1;
  currentPageDisplayed = signal(1);
  fileid = 0;
  carouselItems: any[] = [];

  @ViewChild('carousel') carousel: NguCarousel<any>;

  carouselConfig: NguCarouselConfig = {
    grid: { xs: 1, sm: 1, md: 1, lg: 1, all: 0 },
    load: 5,
    // interval: {timing: 4000, initialDelay: 1000},
    loop: false,
    touch: true,
    velocity: 0.2,
    point: {
      visible: false,
      hideOnSingleSlide: false
    }
  }

  constructor(
    @Inject(PLATFORM_ID) private platformId: any,
    private windowRef: AppWindowRef,
    private dialog: MatDialog,
    public dialogRef: MatDialogRef<FileViewerComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SolrDocument,
    private config: AppConfiguration,
    public state: AppState,
    private service: AppService) { }


  ngOnInit(): void {
    this.setData();
    this.service.logViewer(this.data.ident_cely, this.data['entity']).subscribe(() => {});
  }

  selectFile(file: File, idx: number) {
    // this.carousel.dataSource = [];
    this.selectedFile.set(file);
    //setTimeout(() => {
      this.currentPage = 1;
      this.currentPageDisplayed.set(this.currentPage);
      this.setPage();
      this.fileid = idx + new Date().getTime();
    //}, 10);
  }

  public carouselItemsLoad(j: number) {
    this.carouselItems = [];
    const len = this.selectedFile().pages.length;
    const max = Math.min(len, this.currentPage + 4 );
      for (let i = 0; i < max; i++) {
        this.carouselItems.push(
          i
          //this.selectedFile().pages[i]
        );
      }
  }

  downloadUrl() {
    return this.config.context + '/api/img/full?id=' + this.selectedFile().id;
  }

  imgPoint(doc: any, size: string) {
    if (doc.hasOwnProperty('mimetype')) {
      if (doc.mimetype.indexOf('pdf') > 0) {
        return this.config.context + '/api/pdf';
      } else {
        return this.config.context + '/api/img/' + size;
      }
    } else {
      return this.config.context + '/api/img/' + size;
    }
  }

  confirmDownload() {
    const d = this.dialog.open(LicenseDialogComponent, {
      width: '450px',
      panelClass: 'app-login-dialog',
      data: { result: this.data }
    });

    d.afterClosed().subscribe(result => {

      if (result) {
        if (isPlatformBrowser(this.platformId)) {
          // this.windowRef.nativeWindow.open(this.downloadUrl());

          const link = this.windowRef.nativeWindow.document.createElement('a');
          link.href = this.downloadUrl();
          link.download = this.selectedFile().nazev;
          link.click();
          this.service.showInfoDialog(this.service.getTranslation('dialog.desc.download_started'), 2000);
        }

      }
    });
  }

  nextPage() {
    if (this.currentPage < this.selectedFile().rozsah) {
      this.currentPage++;
      this.currentPageDisplayed.set(this.currentPage);
      this.carousel.moveTo(this.currentPageDisplayed() - 1, false);
    }
  }

  prevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.currentPageDisplayed.set(this.currentPage);
      this.carousel.moveTo(this.currentPageDisplayed() - 1, false);
    }
  }

  gotoPage() {
    
    this.currentPage = parseInt(this.currentPage+'');
    this.currentPageDisplayed.set(this.currentPage);
    if (this.currentPage > 0 || this.currentPage < this.selectedFile().rozsah) {
      this.carousel.moveTo(this.currentPageDisplayed() - 1, false);
    }
  }

  setPage() {
    const regex = /[0-9]/;
    const matches = regex.exec(this.currentPage+'');
    if (!matches) {
      alert(this.service.getTranslation('dialog.invalid_page'));
      return;
    }
    this.currentPage = parseInt(this.currentPage+'');
    if (this.currentPage < 1 || this.currentPage > this.selectedFile().rozsah) {
      alert(this.service.getTranslation('dialog.invalid_page'));
      return;
    }
    this.currentPageDisplayed.set(this.currentPage);
    this.carouselItemsLoad(this.currentPage);
    if (!this.carousel) {
      return;
    }
    setTimeout(() => {
        this.carousel.moveTo(this.currentPageDisplayed() - 1, false);
    }, 100);
  }

  setData() {
    this.selectedFile.set(null);
    const files: File[] = [];
    this.showing.set(false);

    setTimeout(() => {
      // this.rolling = false;
      // const fs: any[] = JSON.parse(this.data.soubor);

      // this.rolling = false;
      // const fs: any[] = JSON.parse(this.data.soubor);
      this.data['soubor'].forEach((f: any) => {
        const file = new File();
        file.id = f.id;
        file.nazev = f.nazev;
        file.mimetype = f.mimetype;
        const rozsah = f.rozsah;
        // const rozsah = '20';
        file.rozsah = (rozsah != null) ? parseInt(rozsah, 10) : 1;
        file.size_mb = f.size_mb;
        file.pages = new Array(file.rozsah);
        file.filepath = f.filepath;
        // file.setSize(true);
        files.push(file);
        files.sort((a, b) => {
          return a.nazev.localeCompare(b.nazev);
        });
      });
      this.files.set(files);
      this.fileid = new Date().getTime();
      this.selectFile(this.files()[0], 0);
      // this.selectedFile = this.files[0];
      // this.currentPage = 1;
      this.showing.set(true);
    }, 10);

  }

  mimetype() {
    const s = this.selectedFile().mimetype;
    if (s.indexOf('/') > 0) {
      return s.split('/')[1].toUpperCase();
    } else {
      return s;
    }

  }

  close() {
    this.dialogRef.close();
  }
}
