export class NeidentAkce {
  ident_cely = '';
  okres = '';
  katastr = '';
  lokalizace = '';
  vedouci = '';
  rok_zahajeni = '';
  rok_ukonceni = '';
  pian = '';
  popis = '';
  poznamka = '';

  setFieldFromDokument(dk: any, idx: string) {
    const keys = Object.keys(this);
    keys.forEach(k => {
      const sk = 'neident_akce_' + k;
      if (dk[sk]) {
        this[k] = dk[sk][idx];
      }
    });

    // this.okres = dk.neident_akce_okres[idx];
    // this.katastr = dk.neident_akce_katastr[idx];
    // this.lokalizace = dk.neident_akce_lokalizace[idx];
    // this.vedouci = dk.neident_akce_vedouci[idx];
    // this.rok_zahajeni = dk.neident_akce_rok_zahajeni[idx];
    // this.rok_ukonceni = dk.neident_akce_rok_ukonceni[idx];
    // if (dk.hasOwnProperty('neident_akce_pian')) {
    //   this.pian = dk.neident_akce_pian[idx];
    // }

    // if (dk.neident_akce_popis) {
    //   this.popis = dk.neident_akce_popis[idx];
    // }

    // if (dk.neident_akce_poznamka) {
    //   this.popis = dk.neident_akce_poznamka[idx];
    // }
  }

}
