import { Component, OnInit, ViewContainerRef, Inject, Output, EventEmitter } from '@angular/core';
import { Overlay } from '@angular/cdk/overlay';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { LoginDialogComponent } from '../login-dialog/login-dialog.component';
import { DOCUMENT } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TranslateModule } from '@ngx-translate/core';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import {MatToolbarModule} from '@angular/material/toolbar';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';

@Component({
  imports: [
    TranslateModule,
    FormsModule,
    RouterModule,
    FlexLayoutModule,
    MatIconModule,
    MatListModule,
    MatProgressBarModule,
    MatButtonModule,
    MatFormFieldModule,
    MatDialogModule,
    MatToolbarModule
],
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements OnInit {

  // sidenav
  @Output() public sidenavToggle = new EventEmitter();
  loggedChecker: ReturnType<typeof setInterval>;
  intervalMilis = 30000;

  constructor(
    public config: AppConfiguration,
    private dialog: MatDialog,
    public state: AppState,
    private service: AppService,
    @Inject(DOCUMENT) private document: Document
  ) { }

  ngOnInit(): void {

    this.service.getLogged(true).subscribe((res: any) => {
      this.state.setLogged(res);
      if (this.state.logged && !this.loggedChecker) {
        this.loggedChecker = setInterval(() => {
          this.checkLogged();
        }, this.intervalMilis);
      }
    });

    this.state.loggedChanged.subscribe(val => {
      if (this.state.logged && !this.loggedChecker) {
        this.loggedChecker = setInterval(() => {
          this.checkLogged();
        }, this.intervalMilis);
      }
    });

  }

  checkLogged() {
    if (this.state.logged) {
      this.service.getLogged(false).subscribe((res: any) => {
        if (res.error) {
          this.state.setLogged(res);
          clearInterval(this.loggedChecker);
          alert(this.service.getTranslation('alert.sessionTimeout'));
        }
      });
    } else {
      clearInterval(this.loggedChecker);
    }
  }

  logout() {

  }

  // sidenav fuction
  public onToggleSidenav = () => {
    this.sidenavToggle.emit();
  }

  changeLang(lang: string) {
    this.service.changeLang(lang);
  }

  logoClicked() {
    this.state.isMapaCollapsed = true;
    this.document.body.classList.remove('app-view-map');
  }

  showLogin() {
    this.state.dialogRef = this.dialog.open(LoginDialogComponent, {
      width: '450px',
      panelClass: 'app-login-dialog',
      data: null
    });
  }


}
