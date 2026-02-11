import { DatePipe } from '@angular/common';
import { HttpParams } from '@angular/common/http';
import { Component, Inject, OnInit, signal } from '@angular/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, Params, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatSelectModule } from '@angular/material/select';
import { provideMomentDateAdapter } from '@angular/material-moment-adapter';
import { MatButtonModule } from '@angular/material/button';

import * as echarts from 'echarts/core';
import { EChartsOption } from 'echarts'; 
import { NgxEchartsDirective, provideEchartsCore } from 'ngx-echarts';
import { LineChart } from 'echarts/charts';
import { TooltipComponent, GridComponent, TitleComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { LabelLayout } from "echarts/features";
import { MAT_DATE_LOCALE, MAT_DATE_FORMATS } from '@angular/material/core';
echarts.use([CanvasRenderer, LineChart, TooltipComponent, GridComponent, TitleComponent, LabelLayout]);
import 'moment/locale/cs';

export class MultiDateFormat {
  value = '';
  constructor() { }
  get display() {
    switch (this.value) {
      case 'mm.yyyy':
        return {
          dateInput: 'MM.YYYY',
          monthYearLabel: 'MM YYYY',
          dateA11yLabel: 'MM.YYYY',
          monthYearA11yLabel: 'MM YYYY',
        };
      case 'yyyy':
        return {
          dateInput: 'YYYY',
          monthYearLabel: 'MM YYYY',
          dateA11yLabel: 'MM.YYYY',
          monthYearA11yLabel: 'MM YYYY',
        };
      default:
        return {
          dateInput: 'DD.MM.YYYY',
          monthYearLabel: 'MMM YYYY',
          dateA11yLabel: 'LL',
          monthYearA11yLabel: 'MMMM YYYY',
        }
    }

  }
  get parse() {
    switch (this.value) {
      case 'mm.yyyy':
        return {
          dateInput: 'MM.YYYY'
        };
      case 'yyyy':
        return {
          dateInput: 'YYYY'
        };
      default:
        return {
          dateInput: 'DD.MM.YYYY'
        }
    }

  }
}

@Component({
  imports: [
    TranslateModule,
    RouterModule,
    FlexLayoutModule,
    FormsModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatDatepickerModule,
    MatIconModule,
    MatInputModule,
    MatCardModule,
    MatListModule,
    MatSelectModule,
    MatButtonModule,
    NgxEchartsDirective
],
  providers: [
    provideEchartsCore({ echarts }),
    provideMomentDateAdapter(), 
    
    { provide: MAT_DATE_LOCALE, useValue: 'cs-CZ' },
    // {
    //   provide: DateAdapter,
    //   useClass: MomentDateAdapter,
    //   deps: [MAT_DATE_LOCALE, MAT_MOMENT_DATE_ADAPTER_OPTIONS],
    // },
    { provide: MAT_DATE_FORMATS, useClass: MultiDateFormat }
  ],
  selector: 'app-stats',
  templateUrl: './stats.component.html',
  styleUrls: ['./stats.component.scss']
})
export class StatsComponent implements OnInit {

  totalIds: number;
  typesAll = ['id', 'viewer', 'file'];
  ident_cely: string;
  type: string;
  ip: string;
  user: string;
  entity: string;
  interval: string;
  ids: { name: string, type: string, value: number }[];
  types: { name: string, type: string, value: number }[];
  ips: { name: string, type: string, value: number }[];
  users: { name: string, type: string, value: number }[];
  entities: { name: string, type: string, value: number }[];
  subs: any[] = [];

  datumod: Date;
  datumdo: Date;

  loading = signal(false);

  // insoType: string = 'O';
  // extType: string;
  series: any = [];
  legend: any = [];

  chartOptions: EChartsOption = {
    tooltip: {},
    xAxis: {
    },
    yAxis: {},
    series: [],
    legend: {
      data: [],
      bottom: 0,
    },
    color: ['rgb(0, 153, 168)', '#fac858'],
  };


  constructor(
    // @Inject(MAT_DATE_FORMATS) private dateFormatConfig: MultiDateFormat,
    private datePipe: DatePipe,
    private route: ActivatedRoute,
    private router: Router,
    private titleService: Title,
    public state: AppState,
    private service: AppService) { }

  ngOnInit(): void {
    this.setTitle();

    // Init this.datumod NOW/YEAR - 1YEAR
    // this.datumod = new Date(new Date().getFullYear() - 1, 0, 1);
    this.datumod = new Date(new Date().setFullYear(new Date().getFullYear() - 1))

    this.service.currentLang.subscribe(res => {
      this.search(this.route.snapshot.queryParams);
      this.setTitle();
    });

    this.subs.push(this.route.queryParams.subscribe(val => {
      this.search(val);
    }));
  }

  ngOnDestroy() {
    this.subs.forEach(s => s.unsubscribe());
  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('navbar.desc.logo_desc') + ' | Stats');
  }

  setInterval(interval: string) {

    const params: any = {};
    params.interval = interval;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  setIdentCely() {
    const params: any = {};
    params.ident_cely = this.ident_cely;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  addDateFilter() {
    const params: any = {};
    params['date'] = this.datePipe.transform(this.datumod, 'yyyy-MM-dd') + ',' + this.datePipe.transform(this.datumdo, 'yyyy-MM-dd');
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  filter(field: string, value: string) {
    const params: any = {};
    params[field] = value ? value : undefined;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  removeFilter(field: string, event: any) {
    const params: any = {};
    params[field] = undefined;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
    event.stopPropagation()
  }

  search(params: Params) {
    this.loading.set(true);
    this.ident_cely = params['ident_cely'];
    this.type = params['type'];
    this.ip = params['ip'];
    this.user = params['user'];
    this.interval = params['interval'] ? params['interval'] : 'WEEK';
    this.entity = params['entity'];

    if (params['date']) {
      const dates = params['date'].split(',');
      if ('null' !== dates[0]) {
        this.datumod = dates[0];
      }
      if ('null' !== dates[1]) {
        this.datumdo = dates[1];
      }
    }

    const p: any = Object.assign({}, params);
    p.page = 0;
    this.service.searchStats(p as HttpParams).subscribe((resp: any) => {
      this.types = resp.facet_counts.facet_fields.type;
      this.ips = resp.facet_counts.facet_fields.ip;
      this.ids = resp.facet_counts.facet_fields.ident_cely;

      this.users = resp.facet_counts.facet_fields.user;
      this.entities = resp.facet_counts.facet_fields.entity;
      this.setGraphData(resp.facet_counts.facet_ranges.indextime.counts);

      this.totalIds = resp.stats.stats_fields.ident_cely.countDistinct;
      this.loading.set(false);
    });
  }

  prefix = 'rest/AMCR/record/';
  formatId(id: string) {

    if (id.startsWith(this.prefix)) {
      return id.substring(this.prefix.length)
    }
    return id;
  }

  setGraphData(counts: { name: string, type: string, value: number }[]) {
    this.series = [];
    const xAxisData: string[] = [];
    const values: any[] = [];
    let maxY = 0;
    counts.forEach(element => {
      values.push(element.value);
      xAxisData.push(this.datePipe.transform(element.name, 'dd.MM.yyyy'));
      maxY = Math.max(element.value, maxY);
    });
    this.series.push({
      // source: 'source.name',
      name: this.service.getTranslation('stats.graphName'),
      field: 'indextime',
      maxY: maxY,
      minY: 0,
      dataType: 'integer',
      type: 'line',
      lineStyle: {
        width: 4,
        shadowColor: 'rgba(0, 0, 0, 0.5)',
        shadowBlur: 10,
        shadowOffsetX: 5,
        shadowOffsetY: 5
      },
      data: values,

    });
    this.legend.push(this.service.getTranslation('stats.graphLegend'));

    this.chartOptions.xAxis = {
      data: xAxisData,
      silent: false,
      splitLine: {
        show: true,
      },
    };
    this.chartOptions.series = this.series;
    this.chartOptions.legend = { data: this.legend, bottom: 0 };

    let title = this.service.getTranslation('stats.graphTitle.' + (this.interval ? this.interval : 'WEEK'));
    this.chartOptions.title = {
      text: title,
      left: 'center'
    };

  }

}

