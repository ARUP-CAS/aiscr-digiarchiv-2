export class KomponentaDok {
  idx = -1;
  ident_cely = '';
  parent = '';
  poradi = '';

  poznamka = '';

  obdobi = '';
  obdobi_poradi = '';
  jistota = '';
  presna_datace = '';
  areal = '';
  aktivita_sidlistni = false;
  aktivita_tezebni = false;
  aktivita_vyrobni = false;
  aktivita_komunikace = false;
  aktivita_boj = false;
  aktivita_kultovni = false;
  aktivita_pohrebni = false;
  aktivita_deponovani = false;
  aktivita_intruze = false;
  aktivita_jina = false;

  setFieldFromDokument(dk: any, idx: string) {

    const keys = Object.keys(this);
    keys.forEach(k => {
      const sk = 'komponenta_dokumentu_' + k;
      if (dk[sk]) {
        this[k] = dk[sk][idx];
      }
    });

    // this.ident_cely = dk['komponenta_dokumentu_ident_cely'][idx];
    // this.parent = dk['komponenta_dokumentu_parent'][idx];
    // this.obdobi = dk['komponenta_dokumentu_obdobi'][idx];
    // this.poradi = dk['komponenta_dokumentu_poradi'][idx];
    // this.poznamka = dk['komponenta_dokumentu_poznamka'][idx];

    // this.jistota = dk['komponenta_dokumentu_jistota'][idx];
    // this.presna_datace = dk['komponenta_dokumentu_presna_datace'][idx];
    // this.areal = dk['komponenta_dokumentu_areal'][idx];
    // this.aktivita_sidlistni = dk['komponenta_dokumentu_aktivita_sidlistni'][idx] === "1";
    // this.aktivita_tezebni = dk['komponenta_dokumentu_aktivita_tezebni'][idx] === "1";
    // this.aktivita_vyrobni = dk['komponenta_dokumentu_aktivita_vyrobni'][idx] === "1";
    // this.aktivita_komunikace = dk['komponenta_dokumentu_aktivita_komunikace'][idx] === "1";
    // this.aktivita_boj = dk['komponenta_dokumentu_aktivita_boj'][idx] === "1";
    // this.aktivita_kultovni = dk['komponenta_dokumentu_aktivita_kultovni'][idx] === "1";
    // this.aktivita_pohrebni = dk['komponenta_dokumentu_aktivita_pohrebni'][idx] === "1";
    // this.aktivita_deponovani = dk['komponenta_dokumentu_aktivita_deponovani'][idx] === "1";
    // this.aktivita_intruze = dk['komponenta_dokumentu_aktivita_intruze'][idx] === "1";
    // this.aktivita_jina = dk['komponenta_dokumentu_aktivita_jina'][idx] === "1";

  }
}
