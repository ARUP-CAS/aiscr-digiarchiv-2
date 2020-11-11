import { Component, OnInit } from '@angular/core';
import { SolrDocument } from 'src/app/shared/solr-document';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppState } from 'src/app/app.state';
import { AppService } from 'src/app/app.service';
import { HttpParams } from '@angular/common/http';
import { SolrResponse } from 'src/app/shared/solr-response';

@Component({
  selector: 'app-favorites',
  templateUrl: './favorites.component.html',
  styleUrls: ['./favorites.component.scss']
})
export class FavoritesComponent implements OnInit {

  opened: boolean = true;
  loading = false;
  docs: SolrDocument[] = [];
  isChartBarCollapsed = true;
  exportUrl: string;

  constructor(
    private titleService: Title,
    private route: ActivatedRoute,
    private router: Router,
    private config: AppConfiguration,
    public state: AppState,
    private service: AppService
  ) {
    this.state.bodyClass = 'app-page-results';
  }

  ngOnInit(): void {
    this.service.currentLang.subscribe(res => {
      this.setTitle();
    });
    this.route.queryParams.subscribe(val => {
      this.search(val);
      const parts = this.router.url.split('?');
      const str = parts.length > 1 ? parts[1] : '' + '&lang=' + this.state.currentLang;
      this.exportUrl = 'export?' + str;
    });
  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('logo_desc') + ' | Results');
  }

  search(params: Params) {
    if (this.state.isMapaCollapsed) {
      this.loading = true;
    }
    const p = Object.assign({}, params);
    p.mapa = !this.state.isMapaCollapsed;
    p.inFavorites = true;
    this.service.search(p as HttpParams).subscribe((resp: SolrResponse) => {
      this.state.setSearchResponse(resp);
      this.docs = resp.response.docs;
      this.loading = false;
    });

  }

  isActiveFacet() {
    if (this.state.breadcrumbs?.length === 0) {
      return false;
    } else {
      return true;
    }
  }

  showChartBar() {
    this.isChartBarCollapsed = !this.isChartBarCollapsed;
  }

  setItemView(view: string) {
    this.state.itemView = view;
  }

}
