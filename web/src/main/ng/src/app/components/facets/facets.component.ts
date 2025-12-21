
import { Component, OnInit, ViewChild, AfterViewInit, OnChanges, SimpleChanges, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FlatTreeControl } from '@angular/cdk/tree';
import { MatTreeFlatDataSource, MatTreeFlattener } from '@angular/material/tree';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { Crumb } from '../../shared/crumb';
import {MatExpansionModule} from '@angular/material/expansion';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import {ScrollingModule} from '@angular/cdk/scrolling';
import { FacetsDynamicComponent } from "../facets-dynamic/facets-dynamic.component";
import { FacetsSearchComponent } from "./facets-search/facets-search.component";
import { MatButtonModule } from '@angular/material/button';

interface FacetNode {
  field: string;
  count: number;
  used: boolean;
  children?: FacetNode[];
}

interface Field {
  value: string;
  viewValue: string;
}

interface ExampleFlatNode {
  expandable: boolean;
  name: string;
  level: number;
}

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule,
    MatCardModule, MatIconModule, MatExpansionModule, MatMenuModule,
    MatProgressBarModule, MatTooltipModule, MatListModule, ScrollingModule,
    FacetsDynamicComponent, MatButtonModule,
    FacetsSearchComponent
],
  selector: 'app-facets',
  templateUrl: './facets.component.html',
  styleUrls: ['./facets.component.scss']
})
export class FacetsComponent implements OnInit {

  @ViewChild('tree') tree: any;

  loading: boolean;
  facetHeight: string = "40px";

  activeOP: boolean; // _temp pro test, pak vymazat

  
  changedFacets: Crumb[] = [];
  math = Math;

  facetsSorted = signal<{ field: string, values: { name: string, type: string, value: number, operator: string }[] }[]>([]);
  expandedFacets: {[field: string]: boolean} = {};

  constructor(
    private router: Router,
    private datePipe: DatePipe,
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService,
  ) { }

  hasChild = (_: number, node: ExampleFlatNode) => node.expandable; // demo

  ngOnInit(): void {
    this.state.facetsChanged.subscribe(() => {
      this.orderFacets();
    });
  }

  orderFacets() {
    if (!this.state.facetsFiltered) {
      return;
    }
    const facetsSorted: { field: string, values: { name: string, type: string, value: number, operator: string, poradi?: number }[] }[] =[];
    this.state.facetsFiltered.forEach(f => {
        const ff: { name: string, type: string, value: number, operator: string, poradi?: number }[] = f.values;
        
        if ('poradi' === this.state.facetSort[f.field]) {
          ff.forEach(v => {
            v.poradi = this.config.thesauri[v.name];
          });
        }

        if ('name' === this.state.facetSort[f.field]) {
          ff.sort((v1, v2) => {
            const n1 = this.service.getHeslarTranslation(v1.name, f.field).toLocaleUpperCase('cs');
            const n2 = this.service.getHeslarTranslation(v2.name, f.field).toLocaleUpperCase('cs');
            // facet.name | translateHeslar : facetField.field
            return n1.localeCompare(n2, 'cs');
          });
        } else if ('poradi' === this.state.facetSort[f.field]) {
          if (f.field === 'pristupnost') {
            ff.sort((v1, v2) => {
              return v1.name.localeCompare(v2.name, 'cs');
            });
            
          } else {
            ff.sort((v1, v2) => {
              return v1.poradi - v2.poradi
            });
          }
        } else {
          ff.sort((v1, v2) => {
            return v2.value - v1.value
          });
        }
        facetsSorted.push({ field: f.field, values: ff });
    });

    this.facetsSorted.update(f => [...facetsSorted]);

  }

  changeShowWithoutThumbs() {
    const val = this.state.hideWithoutThumbs ? true : null;
    this.router.navigate([], { queryParams: { hideWithoutThumbs: val, page: 0 }, queryParamsHandling: 'merge' });
  }

  changeEntity(entity: string) {
    if (this.state.user) {
      // Get sort from ui
      this.service.getLogged(true).subscribe((resp: any) => {
        if (resp.ui) {
          this.state.ui = resp.ui;
        }
        this.setEntity(entity)
      });
    } else {
      this.setEntity(entity)
    }

  }

  setEntity(entity: string) {
    this.state.isFacetsCollapsed = true;
    this.state.setFacetChanged();
    document.getElementById('content-scroller').scrollTo(0,0);
    // Validate sort param sort
    const sortParam = this.state.ui?.sort?.[entity] ? this.state.ui.sort[entity] : null;

    let sort = this.config.sorts.find(s => ((s.field) === sortParam) && (!s.entity || s.entity.length === 0 || s.entity.includes(entity)));

    if (!sort) {
      sort = this.config.sorts.find(s => !s.entity || s.entity.length === 0 || s.entity.includes(entity));
    }
    this.router.navigate([], { queryParams: { entity, page: 0, sort: sort.field }, queryParamsHandling: 'merge' });
  }

  clickPivot(field: string, value: string, used: boolean) {

    const params: any = {};
    params[field] = [];
    this.state.breadcrumbs.forEach((c: Crumb) => {
      if (c.field === field && c.value !== value) {
        params[field].push(c.value);
      }
    });
    if (!used) {
      params[field].push(value);
    }
    params.page = 0;

    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  clickFacet(field: string, facet: any) {
    if (facet.operator && facet.operator !== 'delete') {
      this.addFilter(field, facet, 'delete');
    } else {
      this.addFilter(field, facet, 'or');
    }
  }

  addFilter(field: string, facet: any, op: string) {
    if (!facet.operator || this.changedFacets.length === 0) {
      this.changedFacets.push(new Crumb(field, facet.name, facet.name, op));
    } else {
      let found = false;
      this.changedFacets.forEach((c: Crumb) => {
        if (c.field === field && c.value === facet.name) {
          c.operator = op;
          found = true;
        }
      });
      if (!found) {
        this.changedFacets.push(new Crumb(field, facet.name, facet.name, op));
      }
    }
    facet.operator = op;
  }

  clickCommonFacet(cf: {name: string, value: string}) {
    this.state.isFacetsCollapsed = true;
    this.state.setFacetChanged();
    document.getElementById('content-scroller').scrollTo(0,0);
    const params: any = {};
    params[cf.name] = true;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  applyFilters() {
    this.state.isFacetsCollapsed = true;
    this.state.setFacetChanged();
    document.getElementById('content-scroller').scrollTo(0,0);
    const params: any = {};
    this.changedFacets.forEach((c: Crumb) => {
      if (!params[c.field]) {
        // First time for this field. Push all from breadcrumbs
        params[c.field] = [];
        this.state.breadcrumbs.forEach((b: Crumb) => {
          if (c.field === b.field && c.value !== b.value) {
            params[c.field].push(b.value + ':' + b.operator);
          }
        });
        if (c.operator !== 'delete') {
          params[c.field].push(c.value + ':' + c.operator);
        }
        if (params[c.field].length === 0) {
          params[c.field] = null;
        }
      } else {
        if (c.operator !== 'delete') {
          // Change operators
          let found = false;
          params[c.field].forEach((p: string) => {
            if (c.value === p.split(':')[0]) {
              p = c.value + ':' + c.operator;
              found = true;
            }
          });
          if (!found) {
            params[c.field].push(c.value + ':' + c.operator);
          }
        } else {
          // remove value
          params[c.field] = params[c.field].filter((p: string) => (c.value !== p.split(':')[0]));
        }
        if (params[c.field].length === 0) {
          params[c.field] = null;
        }
      }
    });
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  setFacetSort(facet: string, sort: string) {
    this.state.facetSort[facet] = sort;
    this.orderFacets();
  }
}
