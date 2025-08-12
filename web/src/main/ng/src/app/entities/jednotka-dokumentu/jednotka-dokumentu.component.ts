import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
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
import { DocumentDialogComponent } from '../../components/document-dialog/document-dialog.component';
import { FeedbackDialogComponent } from '../../components/feedback-dialog/feedback-dialog.component';
import { InlineFilterComponent } from '../../components/inline-filter/inline-filter.component';
import { NalezComponent } from "../nalez/nalez.component";
import { MatMenuModule } from "@angular/material/menu";
import { DokumentComponent } from "../dokument/dokument.component";

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    InlineFilterComponent, MatButtonModule,
    MatMenuModule, MatDialogModule,
    forwardRef(() => NalezComponent),
    forwardRef(() => DokumentComponent)
    
],
  selector: 'app-jednotka-dokumentu',
  templateUrl: './jednotka-dokumentu.component.html',
  styleUrls: ['./jednotka-dokumentu.component.scss']
})
export class JednotkaDokumentuComponent implements OnInit {

  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() mapDetail: boolean;
  @Input() isDocumentDialogOpen: boolean;
  @Input() inDocument = false;
  hasRights: boolean;

  constructor(
    private router: Router,
    public service: AppService,
    public state: AppState,
    private dialog: MatDialog,
    public config: AppConfiguration
  ) { }
  
  ngOnInit(): void {
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
      this.state.printing.set(true);
      this.router.navigate(['/id', this.result.ident_cely]);
    }
  }
  

  openDocument() {
    this.state.dialogRef = this.dialog.open(DocumentDialogComponent, {
      width: '900px',
      data: this.result,
      panelClass: 'app-document-dialog'
    });
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
    return false;
  }

  openFeedback() {
    this.state.dialogRef = this.dialog.open(FeedbackDialogComponent, {
      width: '900px',
      data: this.result.ident_cely,
      panelClass: 'app-feedback-dialog'
    });
  }
}
