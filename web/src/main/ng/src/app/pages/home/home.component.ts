import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { Component, OnInit, ViewContainerRef, TemplateRef, ViewChild, ElementRef, OnDestroy, HostListener } from '@angular/core';
import { OverlayRef, Overlay } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { AppState } from 'src/app/app.state';
import { Title } from '@angular/platform-browser';
import { SolrResponse } from 'src/app/shared/solr-response';

class Box {
  label: string;
  index: string;
  field: string;
  ico: string;
  count: number;
  typy: any[]
}

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {

  opened: boolean = false;
  private overlayRef: OverlayRef;
  currentBox: Box;

  inPop = false;

  loading: boolean;
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
    this.state.bodyClass = 'app-page-home';
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    this.closePop();
  }

  ngOnInit(): void {
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
        }
      });
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
