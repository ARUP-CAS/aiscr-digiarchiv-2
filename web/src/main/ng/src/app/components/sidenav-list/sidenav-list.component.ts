import { Component, OnInit, Output, EventEmitter, ViewContainerRef, Inject, TemplateRef } from '@angular/core';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { MatDialog } from '@angular/material/dialog';
import { LoginDialogComponent } from '../login-dialog/login-dialog.component';
import { DOCUMENT } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { RouterModule } from '@angular/router';
import { AppConfiguration } from '../../app-configuration';

@Component({
  imports: [
    TranslateModule,
    RouterModule,
    MatIconModule,
    MatListModule
],
  selector: 'app-sidenav-list',
  templateUrl: './sidenav-list.component.html',
  styleUrls: ['./sidenav-list.component.scss']
})
export class SidenavListComponent implements OnInit {
  @Output() sidenavClose = new EventEmitter();

  public onSidenavClose = () => {
    this.sidenavClose.emit();
  }

  private overlayRef: OverlayRef;
  user: string;
  pwd: string;
  loginError: boolean = false;
  isLoginVisible = false;

  // sidenav
  @Output() public sidenavToggle = new EventEmitter();

  constructor(
    private overlay: Overlay,
    private viewContainerRef: ViewContainerRef,
    private dialog: MatDialog,
    public state: AppState,
    public config: AppConfiguration,
    private service: AppService,
    @Inject(DOCUMENT) private document: Document
  ) { }
  
  ngOnInit(): void {
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
      width: '700px',
      data: null
    });
  }

  login() {
    this.service.login(this.user, this.pwd).subscribe(res => {
      if (res.error) {
        // console.log(res['error']);
        this.loginError = true;
        this.state.logged = false;
      } else {
        this.state.logged = true;
        this.loginError = false;
        this.state.user = null;
        this.user = '';
        this.pwd = '';
        for (const first in res) {
          if (res[first]) {
            this.state.user = res[first];
            break;
          }
        }
      }
    });
  }

  showFav() {
    alert('TODO');
  }

  closePop() {
    this.closeInfoOverlay();
    this.isLoginVisible = false;
  }

  openInfoOverlay(relative: any, template: TemplateRef<any>) {
    this.closeInfoOverlay();

    this.isLoginVisible = true;
    this.overlayRef = this.overlay.create({
      positionStrategy: this.overlay.position().flexibleConnectedTo(relative).withPositions([{
        overlayX: 'end',
        overlayY: 'top',
        originX: 'center',
        originY: 'bottom'
      }]).withPush().withViewportMargin(10).withDefaultOffsetX(17).withDefaultOffsetY(20),
      scrollStrategy: this.overlay.scrollStrategies.close(),
      hasBackdrop: false,
      backdropClass: 'popover-backdrop'
    });
    this.overlayRef.backdropClick().subscribe(() => this.closeInfoOverlay());

    const portal = new TemplatePortal(template, this.viewContainerRef);
    this.overlayRef.attach(portal);
  }

  closeInfoOverlay() {
    if (this.overlayRef) {
      this.overlayRef.detach();
      this.overlayRef.dispose();
      this.overlayRef = null;
    }
  }


}
