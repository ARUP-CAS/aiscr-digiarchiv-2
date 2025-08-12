import { Component, OnInit, Input } from '@angular/core';
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
  
  @Input() tvar: string;
  @Input() poznamka: string = null;

  constructor() { }

  ngOnInit(): void {
  }

  hasPoznamka(): boolean{
    return this.poznamka !== null && this.poznamka !== '';
  }

}
