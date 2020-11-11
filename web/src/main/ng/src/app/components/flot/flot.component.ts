import { isPlatformBrowser } from '@angular/common';
import {
  Component, ElementRef, Input, Output, OnInit, EventEmitter,
  HostListener, OnChanges, SimpleChanges, ChangeDetectionStrategy, Inject, PLATFORM_ID
} from '@angular/core';
import { AppWindowRef } from 'src/app/app.window-ref';

declare var $: any;

@Component({
  selector: 'app-flot',
  templateUrl: './flot.component.html',
  styleUrls: ['./flot.component.scss']
})

export class FlotComponent implements OnChanges, OnInit {
  // @Input() dataset: any;
  public data = [];
  @Input() onselection: any;
  @Input() options: any;
  @Input() width: string | number = '100%';
  @Input() height: string | number = 220;

  @Output() selectionChanged: EventEmitter<any> = new EventEmitter();
  @Output() clicked: EventEmitter<any> = new EventEmitter();


  initialized = false;

  plotArea: any;
  plot: any;

  selecting = false;

  constructor(
    @Inject(PLATFORM_ID) private platformId: any,
    private windowRef: AppWindowRef,
    private el: ElementRef) { }


  public ngOnChanges(changes: SimpleChanges): void {
    if (changes.option && this.initialized) {
      this.draw();
    }
  }

  draw() {
    if (this.initialized) {
      this.plot = $.plot(this.plotArea, this.data, this.options);
    }
  }

  public setOptions(options) {
    this.options = options;
    this.draw();

  }

  public setData(data) {
    this.data = data;
    this.draw();

  }

  public setSelection(sel) {
    if (this.initialized) {
      this.plot.setSelection(sel);
      if (isPlatformBrowser(this.platformId)) {
        if (this.windowRef.nativeWindow.event) {
          this.windowRef.nativeWindow.event.preventDefault();
        }
      }
      
    }
  }

  private dataAtPos(pos) {
    let item = null;

    const series = this.plot.getData();
    // for (let i = series.length - 1; i >= 0; --i) {
    const s = series[0],
      points = s.datapoints.points,

    mx = pos.x;
    let ps = s.datapoints.pointsize;
    let barLeft, barRight;

    switch (s.bars.align) {
      case 'left':
        barLeft = 0;
        break;
      case 'right':
        barLeft = -s.bars.barWidth;
        break;
      default:
        barLeft = -s.bars.barWidth / 2;
    }

    barRight = barLeft + s.bars.barWidth;

    for (let j = 0; j < points.length; j += ps) {
      const x = points[j], y = points[j + 1], b = points[j + 2];
      // console.log(mx, x,y,b);
      if (x == null) {
        continue;
      }

      // for a bar graph, the cursor must be inside the bar
      if (series[0].bars.horizontal ?
        (mx <= Math.max(b, x) && mx >= Math.min(b, x)) :
        (mx >= x + barLeft && mx <= x + barRight)) {
        item = [0, j / ps];
      }
    }
    // }

    let ret = null;
    if (item) {
      const i = item[0];
      const j = item[1];
      ps = series[0].datapoints.pointsize;

      ret = {
        datapoint: series[0].datapoints.points.slice(j * ps, (j + 1) * ps),
        dataIndex: j,
        series: series[0],
        seriesIndex: i
      };
    }
    return ret;
  }

  public ngOnInit(): void {
    if (!this.initialized) {
      this.plotArea = $(this.el.nativeElement).find('div').empty();
      // this.plotArea = $('#flotCanvas');
      this.plotArea.css({
        width: this.width,
        height: this.height
      });

      let _this = this;
      this.plotArea.bind('plotselected', function(event, ranges) {
        _this.selecting = true;
        _this.selectionChanged.emit(ranges);
      });

      this.plotArea.bind('plotunselected', function(event, ranges) {
        _this.selecting = false;
      });

      this.plotArea.bind('plotclick', function(event, pos, item) {

        if (_this.selecting) {
          _this.selecting = false;
          return;
        }
        if (item) {
          _this.clicked.emit(item);
        }
        if (!item) {
          const titem = _this.dataAtPos(pos);
          if (titem) {
            _this.clicked.emit(titem);
          }
        }
        _this.selecting = false;
      });
      this.initialized = true;
      this.draw();
    }
  }
}
