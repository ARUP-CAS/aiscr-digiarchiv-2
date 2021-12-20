export class SolrDocument {
  id: string;
  ident_cely: string;
  pristupnost: string;
  pian: {
    pristupnost: string,
    type: string,
    parent: string, 
    ident_cely: string, 
    centroid_e: string, 
    presnost: string, 
    typ: string, 
    parent_doctype: string, 
    centroid_n: string
  }[];
  [prop: string]: any;
}

