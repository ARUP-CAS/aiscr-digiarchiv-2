import { AppState } from 'src/app/app.state';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { Router } from '@angular/router';
import { Crumb } from 'src/app/shared/crumb';
import { SolrResponse } from 'src/app/shared/solr-response';
import { FlatTreeControl } from '@angular/cdk/tree';
import { MatTreeFlatDataSource, MatTreeFlattener } from '@angular/material/tree';
import { DatePipe } from '@angular/common';
import { MatDatepickerInputEvent, MatDatepicker } from '@angular/material/datepicker';
import { Moment } from 'moment';
import { DateAdapter } from '@angular/material/core';
import { FormControl } from '@angular/forms';

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
  selector: 'app-facets',
  templateUrl: './facets.component.html',
  styleUrls: ['./facets.component.scss']
})
export class FacetsComponent implements OnInit, AfterViewInit {

  @ViewChild('tree') tree;

  loading: boolean;

  activeOP: boolean; // _temp pro test, pak vymazat

  treeData: FacetNode[] = [];
  treeControl: FlatTreeControl<ExampleFlatNode>;
  treeFlattener;
  dataSource: MatTreeFlatDataSource<unknown, ExampleFlatNode>;
  changedFacets: Crumb[] = [];
  math = Math;

  private flattener = (node: FacetNode, level: number) => {
    return {
      expandable: !!node.children && node.children.length > 0,
      field: node.field,
      count: node.count,
      used: node.used,
      level
    };
  }

  constructor(
    private router: Router,
    private datePipe: DatePipe,
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService,
  ) {

    this.treeControl = new FlatTreeControl<ExampleFlatNode>(node => node.level, node => node.expandable);
    this.treeFlattener = new MatTreeFlattener(this.flattener, node => node.level, node => node.expandable, node => node.children);

    this.dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);
    this.dataSource.data = this.treeData;
  }

  hasChild = (_: number, node: ExampleFlatNode) => node.expandable; // demo

  ngOnInit(): void {
    this.state.stateChanged.subscribe(() => {
      this.setTreeData();
    });

  }

  ngAfterViewInit() {
    // setTimeout(() => {
    //   if (this.tree) {
    //     this.tree.treeControl.expandAll();
    //   }
    // }, 1000);
  }

  setTreeData() {
    this.treeData = [];
    if (this.state.facetPivots.length === 0) {
      return;
    }
    const kategorie = this.state.facetPivots[0];
    kategorie.values.forEach(fp => {
      const used = this.state.breadcrumbs.findIndex(c => c.field === 'kategorie' && c.value === fp.value) > -1;
      const fnode: FacetNode = {
        field: fp.value,
        count: fp.count,
        used,
        children: []
      };
      fp.pivot.forEach(val => {
        const used2 = this.state.breadcrumbs.findIndex(c => c.field === val.field && c.value === val.value) > -1;
        fnode.children.push({
          field: val.value,
          count: val.count,
          used: used2
        });
      });
      this.treeData.push(fnode);

    });
    this.dataSource.data = this.treeData;
  }

  changeShowWithoutThumbs() {
    const val = this.state.hideWithoutThumbs ? true : null;
    this.router.navigate([], { queryParams: { hideWithoutThumbs: val, page: 0 }, queryParamsHandling: 'merge' });
  }

  setEntity(entity) {
    this.router.navigate([], { queryParams: { entity, page: 0 }, queryParamsHandling: 'merge' });
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

  clickFacet(field, facet) {
    if (facet.operator && facet.operator !== 'delete') {
      this.addFilter(field, facet, 'delete');
    } else {
      this.addFilter(field, facet, 'or');
    }
  }

  addFilter(field, facet, op: string) {
    if (!facet.operator || this.changedFacets.length === 0) {
      this.changedFacets.push(new Crumb(field,  facet.name,  facet.name, op));
    } else {
      let found = false;
      this.changedFacets.forEach((c: Crumb) => {
        if (c.field === field && c.value === facet.name) {
          c.operator = op;
          found = true;
        }
      });
      if (!found) {
        this.changedFacets.push(new Crumb(field,  facet.name,  facet.name, op));
      }
    }
    facet.operator = op;
  }

  applyFilters() {
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
          params[c.field].forEach(p => {
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
          params[c.field] = params[c.field].filter(p => (c.value !== p.split(':')[0]));
        }
        if (params[c.field].length === 0) {
          params[c.field] = null;
        }
      }
    });
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }
}
