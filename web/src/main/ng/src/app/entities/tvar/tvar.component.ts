import { Component, OnInit, Input, input } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule,
],
  selector: 'app-tvar',
  templateUrl: './tvar.component.html',
  styleUrls: ['./tvar.component.scss']
})
export class TvarComponent implements OnInit {
  
  tvar = input<string>();
  poznamka = input<string>();

  constructor() { }

  ngOnInit(): void {
  }

}
