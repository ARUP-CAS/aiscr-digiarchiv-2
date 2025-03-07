
export interface Sort { label: string; field: string; dir: string; entity?: string[]};

export class Configuration {
  context: string;
  serverUrl: string;
  registrationUrl: string;
  restorePassword: string;
  helpUrl: string;
  accountUrl: string;
  amcr: string;
  amcr_server: string;
  isIndexing: boolean;
  defaultLang: string;
  facets: string[];
  noPoradiFacets: {[key: string]: boolean};
  // dateFacets: string[];
  // numberFacets: string[];

  entities: string[];
  reCaptchaScore: number;

  uiVars: {[key: string]: any};

  home: {
    boxes: {
      'label': string,
      'index': string,
      'field': string,
      'ico': string,
      count: number,
      typy: any[]
    }[]
  };

  mapOptions: {
    docsForMarker: number,
    docsForCluster: number,
    maxZoom: number,
    zoom: number,
    hitZoomLevel: number,
    centerX: number,
    centerY: number,
    heatmapOptions: {
      radius: number,
      maxOpacity: number,
      minOpacity: number,
      scaleRadius: boolean,
      useLocalExtrema: boolean,
      latField: string,
      lngField: string,
      valueField: string,
      gradient: { [key: string]: string };
    }
    shape: {
      color: string,
      fillColor: string,
      weight: number,
      fillOpacity: number
    },
    selectionInitPad: number;
  };
  hideMenuWidth: number;

  sorts: Sort[];
  currentSort: Sort;

  selRows: number[];
  defaultRows: number;
  exportRowsLimit: number;
  exportFields: {[entity: string]: {name: string, label?: string, heslar?: string, secured?: boolean, byPath?: boolean, type: string}[]};
  urlFields: string[];
  filterFields : {field: string, type: string}[];
  entityIcons: {[entity: string]: string};

  choiceApi: {label: string, metadataPrefix: string, url: string, useParent: boolean}[];
  feedBackMaxLength: number;
}
