import { Component, OnInit, Input } from '@angular/core';
import { Komponenta } from 'src/app/shared/komponenta';
import { Nalez } from 'src/app/shared/nalez';
import { AppService } from 'src/app/app.service';
import { Utils } from 'src/app/shared/utils';

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

  nalez: Nalez[] = [];
  aktivity: string[] = []; 

  constructor(
    private service: AppService
  ) { }

  ngOnInit(): void {
    // this.idShort = this.data.ident_cely.substring(this.data.ident_cely.lastIndexOf('-') + 1);
    // this.service.getNalez(this.data.ident_cely).subscribe(res => {
    //   this.nalez = res;
    // });
    this.fillAktivity();
  }

  fillAktivity() {
    this.hasAktivita('aktivita_sidlistni') ? this.aktivity.push('sidlistni') : '';
    this.hasAktivita('aktivita_tezebni') ? this.aktivity.push('tezebni') : '';
    this.hasAktivita('aktivita_vyrobni') ? this.aktivity.push('vyrobni') : '';
    this.hasAktivita('aktivita_komunikace') ? this.aktivity.push('komunikace') : '';
    this.hasAktivita('aktivita_boj') ? this.aktivity.push('boj') : '';
    this.hasAktivita('aktivita_kultovni') ? this.aktivity.push('kultovni') : '';
    this.hasAktivita('aktivita_pohrebni') ? this.aktivity.push('pohrebni') : '';
    this.hasAktivita('aktivita_deponovani') ? this.aktivity.push('deponovani') : '';
    this.hasAktivita('aktivita_intruze') ? this.aktivity.push('intruze') : '';
    this.hasAktivita('aktivita_jina') ? this.aktivity.push('jina') : '';
  }

  hasAnyAktivita() {
    return this.hasAktivita('aktivita_sidlistni') ||
      this.hasAktivita('aktivita_tezebni') ||
      this.hasAktivita('aktivita_vyrobni') ||
      this.hasAktivita('aktivita_komunikace') ||
      this.hasAktivita('aktivita_boj') ||
      this.hasAktivita('aktivita_kultovni') ||
      this.hasAktivita('aktivita_pohrebni') ||
      this.hasAktivita('aktivita_deponovani') ||
      this.hasAktivita('aktivita_intruze') ||
      this.hasAktivita('aktivita_jina');
  }

  hasAktivita(field: string) {
    return this.result[field] && this.result[field][0] !== '0';
  }

  hasValue(field: string): boolean {
    return Utils.hasValue(this.result, field);
  }

}
