import { DatePipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDatepicker } from '@angular/material/datepicker';
import { Router } from '@angular/router';
import { Moment } from 'moment';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { Crumb } from 'src/app/shared/crumb';

@Component({
  selector: 'app-facets-dynamic',
  templateUrl: './facets-dynamic.component.html',
  styleUrls: ['./facets-dynamic.component.scss']
})
export class FacetsDynamicComponent implements OnInit {
  rokoddate = new FormControl(new Date());
  rokod: number;
  rokdodate = new FormControl(new Date());
  rokdo: number;
  datumod = Date;
  datumdo = Date;
  numberod: number;
  numberdo: number;
  filterField: { type: string, field: string };
  filterFieldValue: string;
  filterOperator: string;
  boolValue: string = '0';

  facetHeight: string = "40px";

  constructor(
    private router: Router,
    private datePipe: DatePipe,
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService
  ) { }

  ngOnInit(): void {

    if (this.state.stats) {
      this.rokod = this.state.stats['rok_vzniku'].min;
      this.rokdo = this.state.stats['rok_vzniku'].max;
      this.rokoddate.setValue(new Date(this.rokoddate.value.setFullYear(this.rokod)));
      this.rokdodate.setValue(new Date(this.rokdodate.value.setFullYear(this.rokdo)));
    }
  }

  addFilter() {
    const params: any = {};
    let val = this.filterFieldValue;
    switch (this.filterField.type) {
      case 'date':
        val = this.datePipe.transform(this.datumod, 'yyyy-MM-dd') +
          ',' + this.datePipe.transform(this.datumdo, 'yyyy-MM-dd');
        break;
      case 'number':
        val = this.numberod + ',' + this.numberdo;
        break;
      case 'rok':
        val = this.rokod + ',' + this.rokdo;
        break;
      case 'boolean':
        val = this.boolValue;
        break;
      default:

    }
    params[this.filterField.field] = [val + ':' + (this.filterOperator ? this.filterOperator : 'or')];
    this.state.breadcrumbs.forEach((c: Crumb) => {
      if (c.field === this.filterField.field) {
        params[this.filterField.field].push(c.value + ':' + c.operator);
      }
    });
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  chosenYearHandler(normalizedYear: Moment, datepicker: MatDatepicker<Moment>, field: string) {
    if (field === 'from') {
      this.rokod = normalizedYear.year();
    } else {
      this.rokdo = normalizedYear.year();
    }

    this.rokoddate.setValue(new Date(this.rokoddate.value.setFullYear(this.rokod)));
    this.rokdodate.setValue(new Date(this.rokdodate.value.setFullYear(this.rokdo)));
    datepicker.close();
  }

  clickRokFacet() {
    const params: any = {};
    params['rok_vzniku'] = this.rokod + ',' + this.rokdo;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

}
