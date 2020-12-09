import { Component, Input, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { DocumentDialogComponent } from 'src/app/components/document-dialog/document-dialog.component';

@Component({
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
    private dialog: MatDialog
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
      this.state.printing = true;
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
  }

}
