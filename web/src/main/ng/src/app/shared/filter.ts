export class FilterValue{
  displayValue: string;
  queryValue: string;
};

export class Filter {
  display: boolean;
  field: string;
  isMultiple: boolean;
  values: FilterValue[] = [];
  displayValue: string;
  queryValue: string;
}
