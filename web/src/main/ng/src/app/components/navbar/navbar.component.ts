import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { Component, OnInit, ViewContainerRef, TemplateRef, HostListener, Inject, Output, EventEmitter } from '@angular/core';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { MatDialog } from '@angular/material/dialog';
import { LoginDialogComponent } from '../login-dialog/login-dialog.component';
import { DOCUMENT } from '@angular/common';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements OnInit {

  // sidenav
  @Output() public sidenavToggle = new EventEmitter();
  loggedChecker;
  intervalMilis = 30000;

  constructor(
    private overlay: Overlay,
    private viewContainerRef: ViewContainerRef,
    private dialog: MatDialog,
    public state: AppState,
    private service: AppService,
    @Inject(DOCUMENT) private document: Document
  ) { }

  ngOnInit(): void {

    this.service.getLogged(true).subscribe((res: any) => {
      this.state.setLogged(res);
      if (this.state.logged) {
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

  focusp(e, el) {
    el.focus();
  }

  showLogin() {
    this.state.dialogRef = this.dialog.open(LoginDialogComponent, {
      width: '450px',
      panelClass: 'app-login-dialog',
      data: null
    });
  }

  // login() {
  //   this.service.login(this.user, this.pwd).subscribe((res: any) => {
  //     this.state.setLogged(res);
  //     if (res.error) {
  //       this.loginError = true;
  //     } else {
  //       this.loginError = false;
  //       this.user = '';
  //       this.pwd = '';
  //     }
  //   });
  // }


}
