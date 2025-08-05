
import { Component, OnInit, Input } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { InlineFilterComponent } from "../../components/inline-filter/inline-filter.component";

@Component({
  imports: [
    TranslateModule,
    RouterModule,
    FlexLayoutModule,
    InlineFilterComponent
],
  selector: 'app-nalez',
  templateUrl: './nalez.component.html',
  styleUrls: ['./nalez.component.scss']
})
export class NalezComponent implements OnInit {

  @Input() result: any;

  constructor() { }

  ngOnInit(): void {
  }

}
