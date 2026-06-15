import { Component, Inject, signal } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { AppService } from '../../app.service';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { TranslateModule } from '@ngx-translate/core';
import { CdkDrag, CdkDragHandle } from '@angular/cdk/drag-drop';

export interface Predmet {
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
}

@Component({
  selector: 'app-museion-predmety-dialog',
  imports: [TranslateModule, MatDialogModule, MatButtonModule, MatTooltipModule, MatTableModule,
    CdkDrag, CdkDragHandle
  ],
  templateUrl: './museion-predmety-dialog.html',
  styleUrl: './museion-predmety-dialog.scss',
})
export class MuseionPredmetyDialog {

  predmety = signal<PredmetyDleAmcr>(null);

  columns = ['aktivita', 'areal', 'cislo', 'cisloCes', 'cisloEvidCes', 'dataceUrceni', 'dataceVzniku', 'datumNabyti',
  'datumNalezu', 'datumStav', 'datumZapisu', 'druhObjektu', 'fond', 'hloubka', 'kompletnost', 
  'komponenta', 'kontextObjekt', 'kontextPlocha', 'kontextStratigrafie', 'material', 'mnozstviSlovy', 'okolnosti', 'oznaceni',
  'pocetCasti', 'pocetKusu', 'podsbirka', 'popis', 'popisCasti', 'popisStav', 
  'poznamkaUrceni', 'prirustkoveCislo', 'rozmer', 'sbirka', 'stav', 'technika']

  constructor(
    public dialogRef: MatDialogRef<MuseionPredmetyDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { id: string, typ: string },
    private service: AppService) { }

  ngOnInit(): void {
    this.service.museionPredmety(this.data.id, this.data.typ).subscribe((res: any) => {
      if (res.hasError) {
        alert(this.service.getTranslation('dialog.alert.feedback_failed') + ": " + res.error);
      } else {
      this.predmety.set(res.predmetyDleAmcrId);

      }
    });
  }

}
