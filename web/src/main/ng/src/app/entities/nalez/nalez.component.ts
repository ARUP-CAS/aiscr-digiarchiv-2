
import { Component, OnInit, input } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { InlineFilterComponent } from "../../components/inline-filter/inline-filter.component";

@Component({
  imports: [
    TranslateModule,
    RouterModule,
    
    InlineFilterComponent
],
  selector: 'app-nalez',
  templateUrl: './nalez.component.html',
  styleUrls: ['./nalez.component.scss']
})
export class NalezComponent implements OnInit {

  readonly result = input<any>();

  constructor() { }

  ngOnInit(): void {
  }

}
