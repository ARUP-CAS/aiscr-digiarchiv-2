export class User {
  jmeno: string;
  prijmeni: string;
  pristupnost: string;
  email: string;
  organizaceNazev: string;
  cteni_dokumentu: boolean;
  skupina: {lang: string, value:string}[];
  organizace: {id: string, value:string};
  ui: { sort: {[entity: string]:string}, rows?:number} = {sort:{}};
}

