import { Crumb } from 'src/app/shared/crumb';
import { AppService } from 'src/app/app.service';
import { Router, ActivatedRoute } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppState } from 'src/app/app.state';
import { Sort } from 'src/app/shared/config';

@Component({
  selector: 'app-facets-used',
  templateUrl: './facets-used.component.html',
  styleUrls: ['./facets-used.component.scss']
})
export class FacetsUsedComponent implements OnInit {

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService) { }

  ngOnInit(): void {
    this.service.currentLang.subscribe(() => {
      this.service.setCrumbs(this.route.snapshot.queryParamMap);
    });
  }

  removeFacet(crumb: Crumb) {
    this.state.isFacetsCollapsed = true;
    const params: any = {};
    const field = crumb.field;
    params[field] = [];
    this.state.breadcrumbs.forEach((c: Crumb) => {
      if (c.field === field && c.value !== crumb.value) {
        params[field].push(c.value + ':' + c.operator);
      }
    });
    if (params[field].length === 0) {
      params[field] = null;
    }
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  clean() {
    const q: any = {};
    this.state.breadcrumbs.forEach((c: Crumb) => {
      q[c.field] = null;
    });
    q.page = 0;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }
}
