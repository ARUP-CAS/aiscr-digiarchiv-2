import { Component, inject, OnInit, PLATFORM_ID } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { KontaktDialogComponent } from './kontakt-dialog/kontakt-dialog.component';
import { LicenceDialogComponent } from './licence-dialog/licence-dialog.component';
import { MatBottomSheet, MatBottomSheetRef } from '@angular/material/bottom-sheet';
import { isPlatformBrowser } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { AppState } from '../../app.state';
import { MatIconModule } from '@angular/material/icon';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { MatButton, MatIconButton } from '@angular/material/button';

@Component({
  imports: [
    TranslateModule, FlexLayoutModule,
    MatIconModule
],
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent implements OnInit {
  platformId;
  mustConsent = true;
  public currentYear = new Date().getFullYear();

  constructor(
    public state: AppState,
    private dialog: MatDialog,
    private _bottomSheet: MatBottomSheet
  ) {
    this.platformId = inject(PLATFORM_ID);
  }

  ngOnInit(): void {
    let expired = false;
    if (!isPlatformBrowser(this.platformId)) {
      return
    }
    if (localStorage.getItem("consentTime") !== null) {
      const EXPIRE_DATE = parseInt(localStorage.getItem("consentTime"));
      expired = Date.now() > EXPIRE_DATE;
    }
    this.mustConsent = (null === localStorage.getItem("consent")) || expired;
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
  imports: [
    TranslateModule,
    MatIconModule, MatButton, MatIconButton
],
  selector: 'footer-consent',
  templateUrl: 'consent.html'
})
export class ConsentSheet {
  constructor(private _bottomSheetRef: MatBottomSheetRef<ConsentSheet>,
    public state: AppState
  ) { }

  consent(event: MouseEvent): void {
    localStorage.setItem("consent", "granted");
    const TIMESTAMP = Date.now();
    const expiresOn = TIMESTAMP + 1000 * 60 * 60 * 24 * 365 // 1year in ms
    localStorage.setItem("consentTime", expiresOn.toString());
    this._bottomSheetRef.dismiss();
    event.preventDefault();
  }

  reject(event: MouseEvent): void {
    this._bottomSheetRef.dismiss();
    event.preventDefault();
    localStorage.setItem("consent", "denied");
    const TIMESTAMP = Date.now();
    const expiresOn = TIMESTAMP + 1000 * 60 * 60 * 24 * 365 // 1year in ms
    localStorage.setItem("consentTime", expiresOn.toString());
  }

  close(event: MouseEvent): void {
    this._bottomSheetRef.dismiss();
    event.preventDefault();
  }
}
