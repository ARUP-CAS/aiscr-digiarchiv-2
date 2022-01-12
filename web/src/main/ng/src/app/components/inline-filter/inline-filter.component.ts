import { Component, OnInit, Input } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { AppConfiguration } from 'src/app/app-configuration';
import { Router } from '@angular/router';

@Component({
  selector: 'app-inline-filter',
  templateUrl: './inline-filter.component.html',
  styleUrls: ['./inline-filter.component.scss']
})
export class InlineFilterComponent implements OnInit {

  @Input() field: string;
  @Input() value: string;
  @Input() heslar: string;
  @Input() isChild: boolean;
  @Input() isDate = false;
  @Input() isYear= false;
  isDocument: boolean;

  constructor(
    private router: Router,
    private config: AppConfiguration,
    private service: AppService) { }

  ngOnInit(): void {
    this.isDocument = this.router.isActive('id', false) || this.router.isActive('print', false);
  }

  addFilter() {
    let v = this.value + '';
    const filter = this.config.filterFields.find(ff => ff.field === this.field);
    if (filter && filter.type === 'date') {
    // if (this.config.dateFacets.includes(this.field)) {
      v += ',' + v;
    }
    this.service.addFilter(this.field, v, 'and');
  }

}
