export class Komponenta {
  idx: number;
  ident_cely: string;
  parent: string;
  poradi:string;
  
  poznamka:string;

  obdobi:string;
  obdobi_poradi:string;
  jistota:string;
  presna_datace:string;
  areal:string;
  aktivita_sidlistni:boolean;
  aktivita_tezebni:boolean;
  aktivita_vyrobni:boolean;
  aktivita_komunikace:boolean;
  aktivita_boj:boolean;
  aktivita_kultovni:boolean;
  aktivita_pohrebni:boolean;
  aktivita_deponovani:boolean;
  aktivita_intruze:boolean;
  aktivita_jina:boolean;
  
  setFieldFromDokument(dk: any, idx: string){
    
    this.ident_cely = dk['komponenta_dokumentu_ident_cely'][idx];
    this.parent = dk['komponenta_dokumentu_parent'][idx];
    this.poradi = dk['komponenta_dokumentu_poradi'][idx];
    this.poznamka = dk['komponenta_dokumentu_poznamka'][idx];
    this.obdobi = dk['komponenta_dokumentu_obdobi'][idx];
    this.obdobi_poradi = dk['komponenta_dokumentu_obdobi_poradi'][idx];
    this.jistota = dk['komponenta_dokumentu_jistota'][idx];
    this.presna_datace = dk['komponenta_dokumentu_presna_datace'][idx];
    this.areal = dk['komponenta_dokumentu_areal'][idx];
    this.aktivita_sidlistni = dk['komponenta_dokumentu_aktivita_sidlistni'][idx] === "1";
    this.aktivita_tezebni = dk['komponenta_dokumentu_aktivita_tezebni'][idx] === "1";
    this.aktivita_vyrobni = dk['komponenta_dokumentu_aktivita_vyrobni'][idx] === "1";
    this.aktivita_komunikace = dk['komponenta_dokumentu_aktivita_komunikace'][idx] === "1";
    this.aktivita_boj = dk['komponenta_dokumentu_aktivita_boj'][idx] === "1";
    this.aktivita_kultovni = dk['komponenta_dokumentu_aktivita_kultovni'][idx] === "1";
    this.aktivita_pohrebni = dk['komponenta_dokumentu_aktivita_pohrebni'][idx] === "1";
    this.aktivita_deponovani = dk['komponenta_dokumentu_aktivita_deponovani'][idx] === "1";
    this.aktivita_intruze = dk['komponenta_dokumentu_aktivita_intruze'][idx] === "1";
    this.aktivita_jina = dk['komponenta_dokumentu_aktivita_jina'][idx] === "1";
  
  }
}
