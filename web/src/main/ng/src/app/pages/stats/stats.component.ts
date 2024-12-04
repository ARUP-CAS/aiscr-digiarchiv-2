import { HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-stats',
  templateUrl: './stats.component.html',
  styleUrls: ['./stats.component.scss']
})
export class StatsComponent implements OnInit {

  typesAll = ['id', 'viewer', 'file'];
  ident_cely: string;
  type: string;
  ip: string;
  user: string;
  ids: { name: string, type: string, value: number }[];
  types: { name: string, type: string, value: number }[];
  ips: { name: string, type: string, value: number }[];
  users: { name: string, type: string, value: number }[];
  subs: any[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private titleService: Title,
    private service: AppService) { }

  ngOnInit(): void {
    this.setTitle();
    this.service.currentLang.subscribe(res => {
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

  setIdentCely() {
    const params: any = {};
    params.ident_cely = this.ident_cely;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  filter(field: string, value: string) {
    const params: any = {};
    params[field] = value;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  removeFilter(field: string) {
    const params: any = {};
    params[field] = undefined;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  search(params: Params) {
    this.ident_cely = params['ident_cely'];
    this.type = params['type'];
    this.ip = params['ip'];
    this.user = params['user'];

    const p: any = Object.assign({}, params);
    // const p: any = {};
    // if (this.ident_cely) {
    //   p.ident_cely = this.ident_cely;
    // }
    // if (params['type']) {
    //   p.type = params['type'];
    // }

    p.page = 0;
    this.service.searchStats(p as HttpParams).subscribe((resp: any) => {
      this.types = resp.facet_counts.facet_fields.type;
      this.ips = resp.facet_counts.facet_fields.ip;
      this.ids = resp.facet_counts.facet_fields.ident_cely;
      const prefix = 'rest/AMCR/record/';
      this.ids.forEach(id => {
        if (id.name.startsWith(prefix)) {
          id.name = id.name.substring(prefix.length)
        }
      })
      this.users = resp.facet_counts.facet_fields.user;
    });
  }

}
