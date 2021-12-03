import { Component, OnInit } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { MatDialog } from '@angular/material/dialog';
import { KontaktDialogComponent } from './kontakt-dialog/kontakt-dialog.component';
import { LicenceDialogComponent } from './licence-dialog/licence-dialog.component';

@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent implements OnInit {
  public currentYear = new Date().getFullYear();

  constructor(
    public state: AppState,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
  }

  showKontakt() {
    this.state.dialogRef = this.dialog.open(KontaktDialogComponent, {
      width: '700px',
      data: null
    });
  }

  showLicence() {
    this.state.dialogRef = this.dialog.open(LicenceDialogComponent, {
      width: '700px',
      data: null
    });
  }

}
