import { AppState } from 'src/app/app.state';
import { Router } from '@angular/router';
import { AppConfiguration } from './../../app-configuration';
import { AppService } from './../../app.service';
import { Component, OnInit, ViewChild, Output, EventEmitter, AfterViewInit } from '@angular/core';

@Component({
  selector: 'app-timeline',
  templateUrl: './timeline.component.html',
  styleUrls: ['./timeline.component.scss']
})
export class TimelineComponent implements OnInit, AfterViewInit {


  @ViewChild('container') container;
  @ViewChild('handleRight') handleRight;
  @ViewChild('handleLeft') handleLeft;
  @ViewChild('dst') dst;


  @Output() timelineChanged = new EventEmitter();

  public leftPos = 0;
  public leftPosStr: string;
  public rightPos = 0;
  public rightPosStr: string;
  button_is_down_: boolean;


  private deltaLeft: number;
  private deltaRight: number;
  private movingLeft: boolean;
  private containerWidth = 0;
  private handleWidth = 0;

  private pxToObdobi: number;

  obdobi: any = null;
  obdobiCount: number;

  titleOd: string;
  titleDo: string;

  idxOd: number;
  idxDo: number;

  currentObdobi: any;

  subs: any;


  constructor(
    private router: Router,
    private config: AppConfiguration,
    public service: AppService,
    public state: AppState) {
    this.leftPosStr = this.leftPos + 'px';
    this.rightPosStr = this.rightPos + 'px';
  }

  ngOnInit() {
    setTimeout(() => {
      this.setObdobi();
      this.subs = this.state.facetsChanged.subscribe(val => {
        if (this.state.obdobi) {
          this.currentObdobi = this.state.obdobi;
        } else {
          this.currentObdobi = '0,' + (this.obdobi.length - 1);
        }
        this.setPos();
      });
    }, 100);

  }

  ngAfterViewInit() {
    if (this.container.nativeElement.offsetWidth) {
      this.containerWidth = this.container.nativeElement.offsetWidth;
      this.handleWidth = this.handleLeft.nativeElement.offsetWidth;
    }
  }

  setObdobi() {
    this.obdobi = this.config.obdobi;
    this.obdobiCount = this.config.obdobiStats.count;
    this.currentObdobi = '0,' + (this.obdobi.length - 1);
    this.setWidths();
    // this.interval = new Interval(this.poradiStats['min'], this.poradiStats['max']);
    this.titleOd = this.obdobi[0].nazev;
    this.titleDo = this.obdobi[this.obdobi.length - 1].nazev;

    this.setPos();

  }

  setWidths() {
    if (this.container && this.container.nativeElement.offsetWidth) {
      this.containerWidth = this.container.nativeElement.offsetWidth;
      this.handleWidth = this.handleLeft.nativeElement.offsetWidth;
      this.pxToObdobi = (this.obdobiCount) / this.containerWidth;
      this.leftPos = 0;
      this.rightPos = 0;
      this.leftPosStr = this.leftPos + 'px';
      this.rightPosStr = this.rightPos + 'px';

    }
  }

  setPos() {

    const idx1 = parseInt(this.currentObdobi.split(',')[0], 10);
    const idx2 = parseInt(this.currentObdobi.split(',')[1], 10);
    this.leftPos = idx1 / this.pxToObdobi;
    this.rightPos = Math.floor(this.containerWidth - (idx2 / this.pxToObdobi));
    this.leftPosStr = this.leftPos + 'px';
    this.rightPosStr = this.rightPos + 'px';
    this.posChanged();

  }

  posChanged() {
    this.idxOd = Math.floor(this.leftPos * this.pxToObdobi);
    this.titleOd = this.obdobi[this.idxOd].nazev;
    this.idxDo = Math.min(Math.floor((this.containerWidth - this.rightPos) * this.pxToObdobi), this.obdobi.length - 1);
    this.titleDo = this.obdobi[this.idxDo].nazev;

  }

  addFilter() {
    const val = this.idxOd + ',' + this.idxDo  + ',' + this.config.obdobi[this.idxOd].poradi  + ',' + this.config.obdobi[this.idxDo].poradi;
    this.router.navigate([], { queryParams: { obdobi_poradi: val }, queryParamsHandling: 'merge' });
  }

  open() {
    if (this.obdobi === null) {
      this.setObdobi();
    }
    if (this.containerWidth === 0) {

      if (this.container && this.container.nativeElement.offsetWidth) {
        this.containerWidth = this.container.nativeElement.offsetWidth;
        this.handleWidth = this.handleLeft.nativeElement.offsetWidth;
        this.pxToObdobi = (this.obdobiCount) / this.containerWidth;
        this.leftPos = 0;
        this.rightPos = 0;
        this.leftPosStr = this.leftPos + 'px';
        this.rightPosStr = this.rightPos + 'px';
        this.posChanged();

      } else {
        setTimeout(() => {
          this.open();
        }, 10);
      }
    }

  }

  onMousedown(evt: any, isLeft: boolean) {
    evt.preventDefault();
    this.button_is_down_ = true;
    this.movingLeft = isLeft;
    if (this.movingLeft) {
      this.deltaLeft = evt.clientX - this.leftPos;
    } else {
      this.deltaRight = evt.clientX + this.rightPos;
    }
  }

  //
  // this function can only be called when button_is_down_ is true
  // as we have used a special div with *ngIf
  // <div *ngIf="button_is_down_"  (window:mousemove)="onMousemove($event)" ..
  //
  onMousemove(evt: any) {

    if (this.movingLeft) {
      if (evt.clientX - this.deltaLeft < 0) {
        this.leftPos = 0;
      } else {
        const newMargin = evt.clientX - this.deltaLeft;
        const offsetRight = this.containerWidth - this.rightPos - this.handleWidth;
        if (newMargin + this.handleWidth < offsetRight) {
          // jen pokud neni u praveho
          this.leftPos = newMargin;
        }
      }
      this.leftPosStr = this.leftPos + 'px';

    } else {
      if (this.deltaRight - evt.clientX > 0) {
        const nm = this.deltaRight - evt.clientX;
        const offsetLeft = this.leftPos + this.handleWidth;
        const offsetRight = this.containerWidth - nm - this.handleWidth;
        if (offsetRight > offsetLeft) {
          // jen pokud neni u leveho
          this.rightPos = nm;
        }
      } else {
        this.rightPos = 0;
      }
      this.rightPosStr = this.rightPos + 'px';
    }
    this.posChanged();
  }

  onMouseup(evt: any) {
    if (this.button_is_down_) {
      this.addFilter();
    }
    this.button_is_down_ = false;
  }

}
