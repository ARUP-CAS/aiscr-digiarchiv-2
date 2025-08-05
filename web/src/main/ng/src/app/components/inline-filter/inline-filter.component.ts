import { CommonModule } from '@angular/common';
import { Component, OnInit, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { MatButtonModule } from '@angular/material/button';

@Component({
  imports: [
    TranslateModule, CommonModule, FormsModule, FlexLayoutModule,
    MatIconModule, MatButtonModule
  ],
  selector: 'app-inline-filter',
  templateUrl: './inline-filter.component.html',
  styleUrls: ['./inline-filter.component.scss']
})
export class InlineFilterComponent implements OnInit {

  @Input() field: string;
  @Input() value: any;
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

  addFilter(event: any) {
    let v = this.value + '';
    const filter = this.config.filterFields.find(ff => ff.field === this.field);
    if (filter && filter.type === 'date') {
    // if (this.config.dateFacets.includes(this.field)) {
      v += ',' + v;
    }
    this.service.addFilter(this.field, v, 'and');
    event.stopPropagation();
  }

}
