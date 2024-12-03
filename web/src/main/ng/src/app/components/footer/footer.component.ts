import { Component, OnInit } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { MatDialog } from '@angular/material/dialog';
import { KontaktDialogComponent } from './kontakt-dialog/kontakt-dialog.component';
import { LicenceDialogComponent } from './licence-dialog/licence-dialog.component';
import { MatBottomSheet, MatBottomSheetRef } from '@angular/material/bottom-sheet';

@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent implements OnInit {

  mustConsent = true;
  public currentYear = new Date().getFullYear();

  constructor(
    public state: AppState,
    private dialog: MatDialog,
    private _bottomSheet: MatBottomSheet
  ) {}

  ngOnInit(): void {
    this.mustConsent = 'true' !== localStorage.getItem("consent");
    if (this.mustConsent) {
      this._bottomSheet.open(ConsentSheet, {
        closeOnNavigation: false,
        disableClose: true
      });
    }
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

@Component({
  selector: 'footer-consent',
  templateUrl: 'consent.html'
})
export class ConsentSheet {
  constructor(private _bottomSheetRef: MatBottomSheetRef<ConsentSheet>) {}

  consent(event: MouseEvent): void {
    this._bottomSheetRef.dismiss();
    event.preventDefault();
    localStorage.setItem("consent", "true");
  }
  
  reject(event: MouseEvent): void {
    this._bottomSheetRef.dismiss();
    event.preventDefault();
    localStorage.setItem("consent", "false");
  }
}
