import { DatePipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormControl, FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatDatepicker } from '@angular/material/datepicker';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Moment } from 'moment';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { Crumb } from '../../shared/crumb';
import {MatSelectModule} from '@angular/material/select';
import {MatDatepickerModule} from '@angular/material/datepicker';
import {MatRadioModule} from '@angular/material/radio';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule, FormsModule, MatFormFieldModule,
    MatCardModule, MatIconModule, MatExpansionModule, MatMenuModule,
    MatProgressBarModule, MatTooltipModule, MatListModule, MatSelectModule,
    MatDatepickerModule, MatRadioModule, MatButtonModule
  ],
  selector: 'app-facets-dynamic',
  templateUrl: './facets-dynamic.component.html',
  styleUrls: ['./facets-dynamic.component.scss']
})
export class FacetsDynamicComponent implements OnInit {
  rokoddate = new FormControl(new Date());
  rokod: number;
  rokdodate = new FormControl(new Date());
  rokdo: number;
  datumod: Date;
  datumdo: Date;
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
      // Zaciname letoskem
      // this.rokod = this.state.stats['dokument_rok_vzniku'].min;
      // this.rokdo = this.state.stats['dokument_rok_vzniku'].max;
      this.rokod = new Date().getFullYear();
      this.rokdo = new Date().getFullYear();
      this.rokoddate.setValue(new Date(this.rokoddate.value.setFullYear(this.rokod)));
      this.rokdodate.setValue(new Date(this.rokdodate.value.setFullYear(this.rokdo)));
    }
  }

  addFilter() {
    this.state.isFacetsCollapsed = true;
    document.getElementById('content-scroller').scrollTo(0,0);
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
