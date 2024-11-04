export class User {
  jmeno: string;
  prijmeni: string;
  pristupnost: string;
  organizaceNazev: string;
  skupina: {lang: string, value:string}[];
  organizace: {id: string, value:string};
}

