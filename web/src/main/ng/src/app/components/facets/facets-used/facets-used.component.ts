
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';

import { AppConfiguration } from '../../../app-configuration';
import { AppService } from '../../../app.service';
import { AppState } from '../../../app.state';
import { Crumb } from '../../../shared/crumb';
import { MatListModule } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';

@Component({
  imports: [
    TranslateModule, RouterModule,  
    MatCardModule, MatIconModule, MatTooltipModule, MatListModule,
    MatButtonModule
],
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
    
      this.service.setCrumbs(this.route.snapshot.queryParamMap);
    this.service.currentLang.subscribe(() => {
      this.service.setCrumbs(this.route.snapshot.queryParamMap);
    });
  }

  removeInfavorites() {
    
    const params: any = {};
    params['inFavorites'] = null;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  removeCommonFacet(name: string) {
    const params: any = {};
    params[name] = null;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  removeFacet(crumb: Crumb) {
    this.state.isFacetsCollapsed = true;
    document.getElementById('content-scroller').scrollTo(0,0);
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
    this.state.isFacetsCollapsed = true;
    document.getElementById('content-scroller').scrollTo(0,0);
    const q: any = {};
    this.state.breadcrumbs.forEach((c: Crumb) => {
      q[c.field] = null;
    });

    this.config.commonFacets.forEach(cf => {
      q[cf.name] = null;
    });
    q.page = 0;
    q['inFavorites'] = null;
    this.state.breadcrumbs = [];
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }
}
