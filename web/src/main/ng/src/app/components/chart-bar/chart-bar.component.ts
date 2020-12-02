import { Component, OnInit, ViewChild, Input, Output, EventEmitter } from '@angular/core';
import { FlotComponent } from '../flot/flot.component';
import { Subscription } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { AppConfiguration } from 'src/app/app-configuration';
import { Router } from '@angular/router';

@Component({
  selector: 'app-chart-bar',
  templateUrl: './chart-bar.component.html',
  styleUrls: ['./chart-bar.component.scss']
})
export class ChartBarComponent implements OnInit {

  @ViewChild('chart') chart: FlotComponent;
  @Input() height: string;
  @Input() width: string;

  @Output() selChanged: EventEmitter<any> = new EventEmitter();

  public data: any = {};
  public options = {};

  ranges = [0, 0];

  constructor(
    private router: Router,
    private config: AppConfiguration,
    private service: AppService,
    private state: AppState) {
  }

  ngOnInit() {
    this.options = {
      series: {
        bars: {
          show: true,
          lineWidth: 1,
          barWidth: 100,
          order: 2
        },
        hoverable: true
      },
      xaxis: {
        show: true,
        tickFormatter: (val, axis) => {
          // console.log(val, axis);
          return this.poradiToObdobi(val);
        }
      },
      grid: {
        hoverable: true,
        borderWidth: 0,
        color: '#546e7a',
        clickable: true,
        mouseActiveRadius: 1000

      },
      selection: {
        mode: 'x'
      },
      tooltip: {
        show: true, 
        bars: true,
        content: (label, xval, yval, flotItem) => {
          const ob1 = this.config.obdobi.find(o => (o.poradi + '') === (xval + ''));
          const ob2 = this.config.obdobi.find(o => (o.poradi + '') === ((+xval + 100) + ''));
          // console.log(ob1);
          if (ob1 && ob2) {
            return ob1.nazev + ' - ' + ob2.nazev + ' (' + yval + ')';
          } else {
            return xval + ' - ' + (+xval + 100) + ' (' + yval + ')';      // "%s | X: %x | Y: %y"
          }
        }
      },
      colors: ['#ffab40', '#ffab40', '#ffab40']
    };
  }

  poradiToObdobi(poradi: number) {
    const ob1 = this.config.obdobi.find(o => (o.poradi + '') === (poradi + ''));
    // console.log(ob1);
    if (ob1) {
      return ob1.nazev;
    } else {
      return '';
    }
  }

  ngAfterViewInit() {
    this.state.facetsChanged.subscribe(
      (resp) => {
        this.setData();
      }
    );
  }

  setData() {
    // console.log(this.state.facetRanges);
    if (this.state.facetRanges && this.state.facetRanges.obdobi_poradi) {
      this.chart.setData(this.state.facetRanges.obdobi_poradi.counts);
      const counts: {name: string, type: string, value: number}[] = this.state.facetRanges.obdobi_poradi.counts;
      const d = [];
      counts.forEach(count => {
        d.push([count.name, count.value]);
      });
      this.data = [{ data: d }];
      this.chart.setData(this.data);

      // this.chart.setSelection({ xaxis: { from: '300', to: '800' } });
      // let c: any[] = this.state.facetRanges.obdobi_poradi['counts'];
      // c = c.filter(val => {
      //   return val[0] !== '0';
      // });
      // if (c.length > 0) {
      //   this.data = [{ data: c }];
      //   this.ranges = [
      //     Math.max(+c[0][0], this.state.currentObdobiOd),
      //     Math.min(this.state.currentObdobiDo, +c[c.length - 1][0] + 10)
      //   ];
      //   //                this.ranges = [c[0][0], +c[c.length-1][0]+10];
      //   this.chart.setData(this.data);
      //   this.chart.setSelection({ xaxis: { from: this.ranges[0], to: this.ranges[1] } });
      // }

    } else {
      this.data = [{ data: [] }];
      this.chart.setData(this.data);
    }
  }

  onSelection(ranges) {
    this.ranges = [
      this.findNearestPoradi(Math.floor(ranges.xaxis.from)),
      this.findNearestPoradi(Math.ceil(ranges.xaxis.to))];
    console.log(this.ranges);
    const params = { obdobi_poradi: this.ranges[0] + ',' + this.ranges[1] };
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  findNearestPoradi(poradi: number) {
    const ob1 = this.config.obdobi.find(o => (o.poradi) === (poradi));
    if (ob1) {
      return poradi;
    } else {
      let diff = 10000;
      let nearest = 0;
      this.config.obdobi.forEach(o => {
        if (Math.abs(poradi - o.poradi) < diff) {
          diff = Math.abs(poradi - o.poradi);
          nearest = o.poradi;
        }
      });
      return nearest;
    }
  }

  onClick(item) {
    console.log(item);
    const params = { obdobi_poradi: item.datapoint[0] + ',' + item.datapoint[0] };
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }


  clear() {
    // this.state.removeRokFilter();
    // this.service.goToResults();
  }


}
