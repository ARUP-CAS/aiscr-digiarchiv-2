import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit, TemplateRef, ViewContainerRef } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { TranslateModule } from '@ngx-translate/core';
import {MatTabsModule} from '@angular/material/tabs';
import { OverlayRef, Overlay } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { Title } from '@angular/platform-browser';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { RouterModule } from '@angular/router';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatTooltipModule} from '@angular/material/tooltip';
import { FreeTextComponent } from "../../components/free-text/free-text.component";
import { FlexLayoutModule } from 'ngx-flexible-layout';

export class Box {
  label: string;
  index: string;
  field: string;
  ico: string;
  count: number;
  typy: any[]
}

@Component({
  selector: 'app-home',
  imports: [
    TranslateModule, CommonModule, RouterModule, FlexLayoutModule,
    MatCardModule, MatIconModule, MatSidenavModule, MatTabsModule,
    MatProgressBarModule, MatTooltipModule,
    FreeTextComponent
],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home implements OnInit {

  opened: boolean = false;
  private overlayRef: OverlayRef;
  currentBox: Box;

  inPop = false;

  loading: boolean = false;
  totals: any = {};
  kategories: string[];

  showingCat: boolean;

  constructor(
    private overlay: Overlay,
    private viewContainerRef: ViewContainerRef,
    private titleService: Title,
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService) {
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    this.closePop();
  }

  ngOnInit(): void {
    this.state.bodyClass = 'app-page-home';
    this.setTitle();
    this.state.hasError = false;
    this.service.currentLang.subscribe(res => {
      this.setTitle();
    });
    this.service.getHome().subscribe((resp: any) => {
      // this.fillBoxes(resp);
      if (resp.error) {
        this.state.loading = false;
        this.loading = false;
        return;
      }
      this.totals = resp;
      this.kategories = Object.keys(resp.kategorie).filter(s => s !== 'ddata3d');

      this.state.stats = resp.stats.stats_fields;
      this.state.numFound = resp.response.numFound;

    });

    if (this.state.user) {
      // Get sort from ui
      this.service.getLogged(true).subscribe((resp: any) => {
        if (resp.ui) {
          this.state.ui = resp.ui;
          this
        }
      });
    }
  }

  getSortByEntity(entity: string): string {
    if (this.state.ui?.sort?.[entity]) {
      return this.state.ui.sort[entity];
    } else {
      this.state.sort = this.state.sorts_by_entity.find(s => (s.field) === this.state.ui.sort[this.state.entity]);
      if (!this.state.sort) {
        this.state.sort = this.state.sorts_by_entity[0];
      }
      return this.state.sort.field;
    }
  }

  searchEntity() {

  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('navbar.desc.logo_desc'));
  }

  showPop(box: Box, relative: any, template: TemplateRef<any>) {
    this.closeInfoOverlay();
    if (box.typy.length > 1) {
      setTimeout(() => {
        this.inPop = true;
        this.currentBox = box;
        this.openInfoOverlay(relative._elementRef, template);
      }, 10);
    }

  }

  enterPop() {
    this.inPop = true;
  }

  enterHome() {
    this.inPop = false;
    setTimeout(() => {

      if (!this.inPop) {
        this.closePop();
      }
    }, 1000);
  }

  closePop() {
    this.closeInfoOverlay();
  }

  openInfoOverlay(relative: any, template: TemplateRef<any>) {
    this.closeInfoOverlay();

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
