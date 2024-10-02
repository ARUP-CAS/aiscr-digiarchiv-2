import { Component, OnInit, Input } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { Utils } from 'src/app/shared/utils';
import { AppState } from 'src/app/app.state';
import { AppConfiguration } from 'src/app/app-configuration';

@Component({
  selector: 'app-komponenta',
  templateUrl: './komponenta.component.html',
  styleUrls: ['./komponenta.component.scss']
})
export class KomponentaComponent implements OnInit {


  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() inDocument = false;
  opened = false;
  idShort: string;

  // nalez: Nalez[] = [];
  aktivity: string[] = []; 

  constructor(
    private service: AppService,
    public state: AppState,
    public config: AppConfiguration
  ) { }

  ngOnInit(): void {
    if (this.result.nalez && !this.result.nalez.hasOwnProperty('length')) {
      this.result.nalez = [this.result.nalez];
    }
    // this.fillAktivity();
  }

  hasAktivita(field: string) {
    return this.result[field] && this.result[field][0] !== '0';
  }

  hasValue(field: string): boolean {
    return Utils.hasValue(this.result, field);
  }

}
