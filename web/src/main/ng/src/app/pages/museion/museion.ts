import { Component, computed, Inject, signal } from '@angular/core';
import { AppService } from '../../app.service';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { TranslateModule } from '@ngx-translate/core';
import { MatSelectModule } from "@angular/material/select";
import { FormsModule } from '@angular/forms';
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { ActivatedRoute } from '@angular/router';

export interface Predmet {
  pristup: string;
  aktivita: string,
  areal: string,
  cislo: string,
  cisloCes: string,
  cisloEvidCes: string,
  dataceUrceni: string,
  dataceVzniku: string,
  datumNabyti: Date,
  datumNalezu: Date,
  datumStav: string,
  datumZapisu: Date,
  druhObjektu: string,
  fond: string,
  hloubka: string,
  kompletnost: string,
  komponenta: string,
  kontextObjekt: string,
  kontextPlocha: string,
  kontextStratigrafie: string,
  material: string,
  mnozstviSlovy: string,
  okolnosti: string,
  oznaceni: string
  pocetCasti: string,
  pocetKusu: number,
  podsbirka: string,
  popis: string,
  popisCasti: string,
  popisStav: string,
  poznamkaUrceni: string,
  prirustkoveCislo: string,
  rozmer: string,
  sbirka: string,
  stav: string,
  technika: string
}

export interface PredmetyDleAmcr {
  organizaceId: string;
  pocetPom: string;
  pocetSys: string;
  predmetSys: Predmet[];
  predmetPom: Predmet[];
}

@Component({
  selector: 'app-museion',
  imports: [TranslateModule, MatButtonModule, MatTooltipModule, MatTableModule,
    MatSelectModule, FormsModule, MatProgressBarModule],
  templateUrl: './museion.html',
  styleUrl: './museion.scss',
})
export class Museion {


  loading = signal<boolean>(true);
  predmetyDleAmcrIdAll = signal<{ [organizaceId: string]: PredmetyDleAmcr }>(null);
  predmetyDleAmcrId = signal<PredmetyDleAmcr>(null);
  predmety = signal<Predmet[]>([]);
  organizaceIds = signal<string[]>([]);
  selectedOrganizace: string;

  columnsFull = ['cislo', 'prirustkoveCislo',
    'cisloCes',
    'cisloEvidCES',
    'fond',
    'sbirka',
    'podsbirka',
    'datumNalezu',
    'datumNabyti',
    'datumZapisu',
    'oznaceni',
    'popis',
    'material',
    'technika',
    'komponenta',
    'dataceVzniku',
    'areal',
    'aktivita',
    'druhObjektu',
    'okolnosti',
    'kontextPlocha',
    'kontextObjekt',
    'kontextStratigrafie',
    'hloubka',
    'mnozstviSlovy',
    'pocetKusu',
    'pocetCasti',
    'popisCasti',
    'rozmer',
    'kompletnost',
    'stav',
    'popisStav',
    'datumStav',
    'poznamkaUrceni',
    'dataceUrceni']

  columnsBasic = ['cislo'];

  pristup = 'FULL'; // FULL | BASIC
  columns = signal<string[]>([]);

  id: string;
  typ: string;

  constructor(
    private route: ActivatedRoute,
    private service: AppService) { }

  ngOnInit(): void {
    this.loading.set(true);
    this.route.queryParams.subscribe(val => {

      this.id = this.route.snapshot.params['id'];
      this.service.museionPredmety(this.id, this.typ).subscribe((res: any) => {
        if (res.hasError) {
          alert(this.service.getTranslation('dialog.alert.feedback_failed') + ": " + res.error);
        } else {
          this.organizaceIds.set(Object.keys(res.predmetyDleAmcrId));
          this.predmetyDleAmcrIdAll.set(res.predmetyDleAmcrId);
          this.selectedOrganizace = this.organizaceIds()[0];
          this.selectOrganizace();
        }
        this.loading.set(false);
      });

    });
    
  }

  selectOrganizace() {
    // this.selectedOrganizace.set(id);
    if (!this.selectedOrganizace) {
      return;
    }
    this.predmetyDleAmcrId.set({...this.predmetyDleAmcrIdAll()[this.selectedOrganizace]});
    //this.pristup = this.predmetyDleAmcrId().pristup;
    this.pristup = (this.predmetyDleAmcrId().predmetPom?.filter(pp => pp.pristup === 'FULL').length > 0 ||
                   this.predmetyDleAmcrId().predmetSys?.filter(pp => pp.pristup === 'FULL').length > 0) ?
                    'FULL' : 'BASIC';
    this.columns.set([...(this.pristup === 'FULL' ? this.columnsFull : this.columnsBasic)])
    this.predmety.set([
      ...(this.predmetyDleAmcrId().predmetSys ? this.predmetyDleAmcrId().predmetSys : []),
      ...(this.predmetyDleAmcrId().predmetPom ? this.predmetyDleAmcrId().predmetPom : [])
    ]);
  }
}
