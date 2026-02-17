import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { MatButtonModule } from '@angular/material/button';

@Component({
  imports: [
    TranslateModule, CommonModule, FormsModule, 
    MatIconModule, MatButtonModule
  ],
  selector: 'app-inline-filter',
  templateUrl: './inline-filter.component.html',
  styleUrls: ['./inline-filter.component.scss'],
  preserveWhitespaces: false
})
export class InlineFilterComponent implements OnInit {

  readonly field = input<string>();
  readonly value = input<any>();
  public strVal = computed<string>(() => 
    (this.value()?.id ? this.value().id : this.value()) + ''
  );
  readonly heslar = input<string>();
  readonly isChild = input<boolean>();
  readonly isDate = input(false);
  readonly isYear = input(false);
  isDocument: boolean;

  constructor(
    private router: Router,
    private config: AppConfiguration,
    private service: AppService) { }

  ngOnInit(): void {
    this.isDocument = this.router.isActive('id', false) || this.router.isActive('print', false);
  }

  addFilter(event: any) {
    const val = this.value();
    console.log(this.field(), val, val.id)
    let v = (val.id ? val.id : val) + '';
    const filter = this.config.filterFields.find(ff => ff.field === this.field());
    if (filter && filter.type === 'date') {
    // if (this.config.dateFacets.includes(this.field)) {
      v += ',' + v;
    }
    this.service.addFilter(this.field(), v, 'and');
    event.stopPropagation();
  }

}
