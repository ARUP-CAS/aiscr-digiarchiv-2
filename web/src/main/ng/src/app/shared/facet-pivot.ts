export class FacetPivot {
  [key: string]: {
    field: string;
    count: number;
    value: string;
    pivot: { field: string, count: number, value: string }[]
  }[]
}
