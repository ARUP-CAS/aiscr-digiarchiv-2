import { Component, OnInit } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { ActivatedRoute, Router } from '@angular/router';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from 'src/app/shared/config';
import { AppConfiguration } from 'src/app/app-configuration';

@Component({
  selector: 'app-paginator',
  templateUrl: './paginator.component.html',
  styleUrls: ['./paginator.component.scss']
})
export class PaginatorComponent implements OnInit {

  pageIndex: number;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    public state: AppState,
    public config: AppConfiguration) { }

  ngOnInit(): void {
    this.pageIndex = this.state.page + 1;


    if (this.state.ui?.rows) {
      this.state.rows = this.state.ui.rows;
    }

    if (this.state.ui?.sort?.[this.state.entity]) {
      this.state.sort = this.state.sorts_by_entity.find(s => (s.field) === this.state.ui.sort[this.state.entity]);
      if (!this.state.sort) {
        this.state.sort = this.state.sorts_by_entity[0];
      }
    }

  }

  pageChanged(e: PageEvent) {
    const params: any = {};
    params.rows = e.pageSize;
    params.page = e.pageIndex;
    this.pageIndex = e.pageIndex + 1;
    this.state.pageChanged = true;
    // document.getElementById('scroll-wrapper').scrollTop = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  setPage() {
    const params: any = {};
    params.page = this.pageIndex - 1;
    this.state.page = this.pageIndex - 1;
    this.state.pageChanged = true;
    // document.getElementById('scroll-wrapper').scrollTop = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  sortBy(sort: Sort) {
    this.state.sort = sort;

    if (this.state.ui) {
      this.state.ui.rows = this.state.rows;
      this.state.ui.sort[this.state.entity] = sort.field;
    }

    this.router.navigate([], { queryParams: { sort: sort.field }, queryParamsHandling: 'merge' });
  }

}
