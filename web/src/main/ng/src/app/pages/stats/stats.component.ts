import { DatePipe } from '@angular/common';
import { HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { EChartsOption } from 'echarts';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

@Component({
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

  loading = false;

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
    private datePipe: DatePipe,
    private route: ActivatedRoute,
    private router: Router,
    private titleService: Title,
    public state: AppState,
    private service: AppService) { }

  ngOnInit(): void {
    this.setTitle();
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

  removeFilter(field: string, $event) {
    const params: any = {};
    params[field] = undefined;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
    $event.stopPropagation()
  }

  search(params: Params) {
    this.loading = true;
    this.ident_cely = params['ident_cely'];
    this.type = params['type'];
    this.ip = params['ip'];
    this.user = params['user'];
    this.interval = params['interval'] ? params['interval'] : 'DAY';
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
      this.loading = false;
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

    let title = this.service.getTranslation('stats.graphTitle.' + (this.interval ? this.interval : 'DAY'));
    this.chartOptions.title = {
      text: title,
      left: 'center'
    };

  }

}
