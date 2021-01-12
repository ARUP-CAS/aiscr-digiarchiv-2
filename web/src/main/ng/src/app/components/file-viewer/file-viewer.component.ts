import { AppState } from 'src/app/app.state';
import { AppWindowRef } from 'src/app/app.window-ref';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { Component, OnInit, Inject, ViewChild, PLATFORM_ID } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';
import { SolrDocument } from 'src/app/shared/solr-document';
import { File } from 'src/app/shared/file';
import { NguCarousel, NguCarouselConfig } from '@ngu/carousel';
import { LicenseDialogComponent } from '../license-dialog/license-dialog.component';
import { isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-file-viewer',
  templateUrl: './file-viewer.component.html',
  styleUrls: ['./file-viewer.component.scss']
})
export class FileViewerComponent implements OnInit {

  showing = false;
  // rolling = false;
  // result: any;

  files: File[] = [];
  selectedFile: File = null;

  currentPage = 1;
  currentPageDisplayed = 1;
  fileid = 0;

  @ViewChild('carousel') carousel: NguCarousel<any>;

  carouselConfig: NguCarouselConfig = {
    grid: { xs: 1, sm: 1, md: 1, lg: 1, all: 0 },
    load: 10,
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
  }

  selectFile(file: File, idx: number) {

    this.selectedFile = file;
    setTimeout(() => {

      this.currentPage = 1;
      this.setPage();
      this.fileid = idx + new Date().getTime();
    }, 10);
  }

  downloadUrl() {
    return this.imgPoint(this.selectedFile) + '?full=true&id=' + this.selectedFile.filepath;
  }

  imgPoint(doc: any) {
    if (doc.hasOwnProperty('mimetype')) {
      if (doc.mimetype.indexOf('pdf') > 0) {
        return this.config.context + '/api/pdf';
      } else {
        return this.config.context + '/api/img';
      }
    } else {
      return this.config.context + '/api/img';
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
          link.download = this.selectedFile.nazev;
          link.click();
        }

      }
    });
  }

  nextPage() {
    this.currentPage++;
    this.carousel.moveTo(this.currentPage - 1, false);

  }

  prevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.carousel.moveTo(this.currentPage - 1, false);
    }
  }

  setPage() {
    if (this.currentPage > 0 || this.currentPage < this.selectedFile.rozsah) {
      this.carousel.moveTo(this.currentPage - 1, false);
    }
  }

  setData() {

    this.selectedFile = null;
    this.files = [];
    this.showing = false;

    setTimeout(() => {
      // this.rolling = false;
      // const fs: any[] = JSON.parse(this.data.soubor);

      this.data.soubor.forEach(f => {
        const file = new File();
        file.nazev = f.nazev;
        file.mimetype = f.mimetype;
        const rozsah = f.rozsah;
        // const rozsah = '20';
        file.rozsah = (rozsah != null) ? parseInt(rozsah, 10) : 1;
        file.size_bytes = parseInt(f.size_bytes, 10);
        file.pages = new Array(file.rozsah);
        file.filepath = f.filepath;
        file.setSize(true);
        this.files.push(file);
        this.files.sort((a, b) => {
          return a.nazev.localeCompare(b.nazev);
        });
      });
      this.fileid = new Date().getTime();
      this.selectedFile = this.files[0];
      this.currentPage = 1;
      this.showing = true;
    }, 10);

  }

  mimetype() {
    const s = this.selectedFile.mimetype;
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
