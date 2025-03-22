
import { Component, OnInit, NgZone, Renderer2, Inject, PLATFORM_ID, OnDestroy, Input, ChangeDetectorRef, Output, EventEmitter } from '@angular/core';

import { HttpParams } from '@angular/common/http';
import { Router, ActivatedRoute } from '@angular/router';

import 'src/assets/js/locationfilter';
import 'leaflet.polylinemeasure';

import { AppService } from 'src/app/app.service';
import { SolrResponse } from 'src/app/shared/solr-response';
import { AppState } from 'src/app/app.state';
import { AppConfiguration } from 'src/app/app-configuration';

import 'node_modules/leaflet.fullscreen/Control.FullScreen.js';
import { Control, geoJSON, LatLngBounds, Map, Marker, marker } from 'leaflet';
import { isPlatformBrowser } from '@angular/common';

declare var L;
import 'leaflet.markercluster';
declare var HeatmapOverlay;

import * as Wkt from 'wicket';
import { SolrDocument } from 'src/app/shared/solr-document';
// import { Wkt } from 'src/typings';

export class AppMarkerOptions {
  id: string;
  isPian: boolean;
  lat: string;
  lng: string;
  presnost: string;
  typ: string;
  doc: any;
  pian_chranene_udaje: any;
  mrk?: Marker;
}


@Component({
  selector: 'app-mapa',
  templateUrl: './mapa.component.html',
  styleUrls: ['./mapa.component.scss']
})
export class MapaComponent implements OnInit, OnDestroy {

  @Input() isResults = true;
  @Input() showResults = false;
  @Output() loadingFinished = new EventEmitter();

  isBrowser: boolean;
  data = {
    data: []
  };

  heatmapLayer;

  locationFilter;

  zoomOptions = {
    zoomInTitle: this.service.getTranslation('map.desc.zoom in'),
    zoomOutTitle: this.service.getTranslation('map.desc.zoom out'),
    position: 'topright'
  };

  options = {
    layers: [],
    zoom: 4,
    zoomControl: false,
    wheelDebounceTime: 200,
    zoomSnap: 0,
    center: L.latLng(49.803, 15.496),
    preferCanvas: true
  };

  icon = L.divIcon({className: 'map-pin', iconSize: null });
  hitIcon = L.divIcon({className: 'map-pin-hit', iconSize: null });
  iconPoint = L.divIcon({className: 'map-pin-point', iconSize: null });
  hitIconPoint = L.divIcon({className: 'map-pin-hit-point', iconSize: null });

  // icon = L.icon({
  //   iconSize: [12, 20],
  //   iconAnchor: [6, 20],
  //   iconUrl: 'assets/img/pin.png',
  //   //shadowUrl: 'assets/img/marker-shadow.png'
  // });

  // hitIcon = L.icon({
  //   iconSize: [12, 20],
  //   iconAnchor: [6, 20],
  //   iconUrl: 'assets/img/pin-hit.png',
  //   //shadowUrl: 'assets/img/marker-shadow.png'
  // });

  // iconPoint = L.icon({
  //   iconSize: [16, 26],
  //   iconAnchor: [8, 26],
  //   iconUrl: 'assets/img/pin-point.png',
  //   //shadowUrl: 'assets/img/marker-shadow.png'
  // });

  // hitIconPoint = L.icon({
  //   iconSize: [16, 26],
  //   iconAnchor: [8, 26],
  //   iconUrl: 'assets/img/pin-hit-point.png',
  //   //shadowUrl: 'assets/img/marker-shadow.png'
  // });

  params: HttpParams;

  maxNumMarkers = 0;
  markerZoomLevel = 16;
  currentZoom: number;
  zoomingOnMarker = false;
  processingParams = false;
  showType = 'marker'; // 'heat', 'cluster', 'marker'

  map: L.Map;
  //idmarkers = new L.featureGroup();
  markers = new L.featureGroup();
  clusters = new L.markerClusterGroup();

  piansList: { id: string, presnost: string, typ: string, docIds: string[] }[] = [];
  markersList: any[] = [];
  selectedMarker: any[] = [];
  selectedResultId: string;
  distErr: number;

  showHeat: boolean;
  mapReady = false;
  subs: any[] = [];
  isInit = true;
  firstChange = true;
  firstZoom = true;

  layersControl = { baseLayers: {}, overlays: {} };
  osmInfo = '<span aria-hidden="true"> | </span>Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a>. ';

  osm = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: this.config.mapOptions.maxZoom,
    maxNativeZoom: 19,
    name: 'osm'
  });

  info: string;
  activeBaseLayerOSM: boolean = true;

  // osmColor = L.tileLayer('http://tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: 'OSM map', maxZoom: 25, maxNativeZoom: 19, minZoom: 6 });

  // https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/253
  cuzkWMS = L.tileLayer.wms('http://services.cuzk.cz/wms/wms.asp?', { layers: 'KN', maxZoom: 25, maxNativeZoom: 20, minZoom: 17, opacity: 0.5 });
  cuzkWMS2 = L.tileLayer.wms('http://services.cuzk.cz/wms/wms.asp?', { layers: 'prehledka_kat_uz', maxZoom: 25, maxNativeZoom: 20, minZoom: 12, opacity: 0.5 });
  // cuzkOrt = L.tileLayer('http://ags.cuzk.cz/arcgis/rest/services/ortofoto_wm/MapServer/tile/{z}/{y}/{x}?blankTile=false', { layers: 'ortofoto_wm', maxZoom: 25, maxNativeZoom: 19, minZoom: 6 });
  cuzkOrt = L.tileLayer('https://ags.cuzk.cz/arcgis1/rest/services/ORTOFOTO_WM/MapServer/tile/{z}/{y}/{x}?blankTile=false', { layers: 'ortofoto_wm', maxZoom: 25, maxNativeZoom: 19, minZoom: 6 });
  cuzkEL = L.tileLayer.wms('http://ags.cuzk.cz/arcgis2/services/dmr5g/ImageServer/WMSServer?', { layers: 'dmr5g:GrayscaleHillshade', maxZoom: 25, maxNativeZoom: 20, minZoom: 6 });

  // cuzkZM = L.tileLayer('http://ags.cuzk.cz/arcgis/rest/services/zmwm/MapServer/tile/{z}/{y}/{x}?blankTile=false', { layers: 'zmwm', maxZoom: 25, maxNativeZoom: 19, minZoom: 6 });
  cuzkZM = L.tileLayer('https://ags.cuzk.cz/arcgis1/rest/services/ZTM_WM/MapServer/tile/{z}/{y}/{x}?blankTile=false', { layers: 'zmwm', maxZoom: 25, maxNativeZoom: 19, minZoom: 6 });

  baseLayers: any = {
    "ČÚZK - Základní mapy ČR": this.cuzkZM,
    "ČÚZK - Ortofotomapa": this.cuzkOrt,
    "ČÚZK - Stínovaný reliéf 5G": this.cuzkEL,
    "OpenStreetMap": this.osm,
  };
  overlays: any = {
    "ČÚZK - Katastrální mapa": this.cuzkWMS,
    "ČÚZK - Katastrální území": this.cuzkWMS2,
  };

  lfAttribution = '<span aria-hidden="true"> | </span><a href="https://leafletjs.com" title="A JavaScript library for interactive maps"><svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="12" height="8" viewBox="0 0 12 8" class="leaflet-attribution-flag"><path fill="#4C7BE1" d="M0 0h12v4H0z"></path><path fill="#FFD500" d="M0 4h12v3H0z"></path><path fill="#E0BC00" d="M0 7h12v1H0z"></path></svg> Leaflet</a>';

  dataLayerName: string = 'data';
  showDetail = false;
  currentMapId: string;
  currentLocBounds: any;
  shouldZoomOnMarker = false;
  setToMarkerZoom = false;
  markersActive = true;
  usingMeasure = false;

  constructor(
    @Inject(PLATFORM_ID) private platformId: any,
    private cd: ChangeDetectorRef,
    private renderer: Renderer2,
    private router: Router,
    private route: ActivatedRoute,
    private zone: NgZone,
    private config: AppConfiguration,
    public state: AppState,
    private service: AppService) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => {
      s.unsubscribe();
    });
  }

  ngOnInit(): void {
    this.info = this.service.getTranslation('map.desc.info');
    this.showDetail = false;
    this.params = this.route.snapshot.queryParams as HttpParams;
    this.maxNumMarkers = this.config.mapOptions.docsForMarker;
    this.options.zoom = this.config.mapOptions.zoom;
    // if (this.state.mapResult) {
    //   this.options.zoom = this.config.mapOptions.hitZoomLevel;
    // }
    this.initLayers();
    this.options.center = L.latLng(this.config.mapOptions.centerX, this.config.mapOptions.centerY);
    // L.control.zoom(this.zoomOptions);

    this.subs.push(this.service.currentLang.subscribe(res => {
      if (this.mapReady) {
        this.setAttribution();
        this.initLayers();
        this.translateControls();
      }
    }));

    this.subs.push(this.route.queryParams.subscribe(val => {
      if (this.mapReady && !this.state.isMapaCollapsed) {
        this.paramsChanged();
      }
    }));

    this.subs.push(this.state.facetsChanged.subscribe(res => {
      const start = new Date();
      if (this.mapReady) {
        this.setHeatData();
        if (res === 'direct') {
          this.markersList = [];
          this.piansList = [];
          this.markers.clearLayers();
          //this.idmarkers.clearLayers();
          this.clusters.clearLayers();
        }
      }
    }));

  }

  setAttribution() {
    this.map.attributionControl.removeAttribution(this.info);
    this.info = this.service.getTranslation('map.desc.info') + (this.activeBaseLayerOSM ? this.osmInfo : '') + this.lfAttribution;
    this.map.attributionControl.addAttribution(this.info);
    this.map.attributionControl.setPrefix(false);
  }

  translateControls() {
    L.setOptions(this.locationFilter, {
      enableButton: {
        enableText: this.service.getTranslation('map.desc.select area'),
        disableText: this.service.getTranslation('map.desc.remove selection')
      }
    });
    this.locationFilter.updateText();
  }

  initLayers() {
    this.baseLayers = {};
    this.baseLayers[this.service.getTranslation('map.layer.cuzk_zakladni')] = this.cuzkZM;
    this.baseLayers[this.service.getTranslation('map.layer.cuzk_orto')] = this.cuzkOrt;
    this.baseLayers[this.service.getTranslation('map.layer.cuzk_stin')] = this.cuzkEL;
    this.baseLayers[this.service.getTranslation('map.layer.openstreet')] = this.osm;
    this.overlays = {};
    this.overlays[this.service.getTranslation('map.layer.cuzk_katastr_mapa')] = this.cuzkWMS;
    this.overlays[this.service.getTranslation('map.layer.cuzk_katastr_uzemi')] = this.cuzkWMS2;
    this.dataLayerName = this.service.getTranslation('map.layer.data');
    this.overlays[this.dataLayerName] = this.markers;
    this.options.layers = [this.osm];

    this.layersControl = {
      baseLayers: this.baseLayers,
      overlays: this.overlays
    }
  }

  popUpHtml(id: string, presnost: string, ids: string[]) {
    const t = this.service.getTranslation('entities.' + this.state.entity + '.title');
    const p = ids.length > 1 ? ids.length : ids[0];
    // const p = ids.join(', ');
    return id + ' (' + this.service.getHeslarTranslation(presnost, 'presnost') + ') (' + t + ': ' + p + ')';
  }

  onMapReady(map: L.Map) {
    this.map = map;
    if (this.route.snapshot.queryParamMap.has('vyber')) {
      const loc_rpt = this.route.snapshot.queryParamMap.get('vyber').split(',');
      const southWest = L.latLng(loc_rpt[0], loc_rpt[1]);
      const northEast = L.latLng(loc_rpt[2], loc_rpt[3]);

      this.state.locationFilterBounds = L.latLngBounds(southWest, northEast);
    }

    this.locationFilter = new L.LocationFilter({
      bounds: this.state.locationFilterBounds,
      enable: this.state.locationFilterBounds !== null,
      adjustButton: false,
      buttonPosition: 'topright',
      enableButton: {
        enableText: this.service.getTranslation('map.desc.select area'),
        disableText: this.service.getTranslation('map.desc.remove selection')
      }
    });

    map.addLayer(this.locationFilter);

    L.control.polylineMeasure({
      position: 'topright',
      measureControlTitleOn: this.service.getTranslation('map.desc.measureOn'), // 'Turn on PolylineMeasure' Title for the control going to be switched on
      measureControlTitleOff: this.service.getTranslation('map.desc.measureOff'), //  'Turn off PolylineMeasure'Title for the control going to be switched off
    }).addTo (map);

    map.on('polylinemeasure:toggle', (e: any) => { 
      this.usingMeasure = e.sttus
    });

    map.addControl(L.control.zoom(this.zoomOptions));
    L.control.scale({ position: 'bottomleft', imperial: false }).addTo(this.map);


    this.markers.clearLayers();
    this.clusters.clearLayers();
    map.addLayer(this.markers);

    map.on('enterFullscreen', () => map.invalidateSize());
    map.on('exitFullscreen', () => map.invalidateSize());


    map.on('baselayerchange', (e) => {
      this.activeBaseLayerOSM = e.layer.options['name'] === 'osm';
      this.setAttribution();
    });

    
    map.on('overlayadd', (e) => {
      if (e.name === this.dataLayerName) {
        this.markersActive = true;
        this.paramsChanged();
      }
    });

    
    map.on('overlayremove', (e) => {
      if (e.name === this.dataLayerName) {
        this.markersActive = false;
        this.clusters.clearLayers();
      }
    });

    map.on('zoomend', (e) => {
      //console.log('zoomend', this.processingParams, this.zoomingOnMarker)

      this.state.mapBounds = this.map.getBounds();
      if (!this.isResults) {
        // Jsme v documentu, nepotrebujeme znovu nacist data
        return;
      }
      if (!this.processingParams && !this.zoomingOnMarker) {
        this.stopLoadingMarkers();
        this.doZoom();
      } else if (this.zoomingOnMarker || this.processingParams) {
        this.stopLoadingMarkers();
        if (this.markersSubs) {
          this.markersSubs.unsubscribe();
        }
        this.getDataByVisibleArea();
      }
      this.firstZoom = false;
      this.zoomingOnMarker = false;
      this.processingParams = false;
    });
    map.on('dragend', () => {
      //console.log('dragend')
      if (!this.processingParams) {
        if (this.markersSubs) {
          this.markersSubs.unsubscribe();
        }
        this.updateBounds(map.getBounds(), false, 'mapDragEnd');
      }

    });
    map.on('fullscreenchange', () => {
      //console.log('fullscreenchange')
      this.updateBounds(map.getBounds(), false, 'mapFull');
    });
    map.on('resize', () => {
      //console.log('resize')
      this.updateBounds(map.getBounds(), false, 'mapResize');
    });

    this.locationFilter.on('change', (e) => {
      if (JSON.stringify(this.state.locationFilterBounds) !== JSON.stringify(this.locationFilter.getBounds())) {
        this.updateBounds(null, true, 'locChange');
      }
      this.firstChange = false;

    });

    this.locationFilter.on('enabled', () => {
      if (!this.state.locationFilterEnabled) {
        this.state.locationFilterEnabled = true;
        this.locationFilter.setBounds(this.map.getBounds().pad(this.config.mapOptions.selectionInitPad));
        this.updateBounds(null, true, 'locEnabled');
      }
    });

    this.locationFilter.on('disabled', () => {
      if (this.state.isMapaCollapsed) {
        return;
      }
      this.state.locationFilterEnabled = false;
      this.state.locationFilterBounds = null;
      this.updateBounds(null, false, 'locDisabled');
    });

    this.paramsChanged();
    this.setAttribution();
    this.mapReady = true;
    // this.updateBounds(this.map.getBounds(), false, 'mapReady');
  }

  zoomingCount = 0;
  doZoom() {
    this.zoomingCount++;
    setTimeout(() => {
      this.zoomingCount--;
      if (this.zoomingCount < 1) {
        this.updateBounds(this.map.getBounds(), false, 'zoom');
      }
    }, 100)
  }

  isEqualsBounds(bounds: LatLngBounds) {
    const b = this.map.getBounds();
    return bounds.getSouth() === b.getSouth() &&
      bounds.getNorth() === b.getNorth() &&
      bounds.getEast() === b.getEast() &&
      bounds.getWest() === b.getWest()
  }

  paramsChanged() {
    if (!this.markersActive) {
      return;
    }
    this.processingParams = true;
    let bounds;
    if (!this.isResults) {
      this.currentMapId = this.route.snapshot.params.id;
      this.shouldZoomOnMarker = true;
      this.getMarkerById(true);
      this.processingParams = false;
      return;
    }
    if (this.state.closingMapResult) {
      this.state.closingMapResult = false;
      this.processingParams = false;
      this.shouldZoomOnMarker = false;
      this.currentMapId = null;
      this.map.fitBounds(this.state.mapBounds);
      this.clearSelectedMarker();
      this.getDataByVisibleArea();
      this.processingParams = false;
      return;
    }
    if (this.route.snapshot.queryParamMap.has('mapId') &&
      this.currentMapId !== this.route.snapshot.queryParamMap.get('mapId') &&
      !this.route.snapshot.queryParamMap.has('loc_rpt')) {
      this.shouldZoomOnMarker = true;
    }
    this.currentMapId = this.route.snapshot.queryParamMap.get('mapId');
    if (!this.currentMapId) {
      this.state.mapResult = null;
      //this.idmarkers.clearLayers();
      this.clearSelectedMarker();
    }
    this.currentLocBounds = this.route.snapshot.queryParamMap.get('loc_rpt');

    if (!this.route.snapshot.queryParamMap.has('loc_rpt') && this.route.snapshot.queryParamMap.has('mapId')) {
      this.getMarkerById(false);
      this.processingParams = false;
      return;
    }

    if (this.route.snapshot.queryParamMap.has('mapId')) {
      this.getMarkerById(false);
    }

    // if (this.route.snapshot.queryParamMap.has('vyber')) {
    //   const loc_rpt = this.route.snapshot.queryParamMap.get('vyber').split(',');
    //   const southWest = L.latLng(loc_rpt[0], loc_rpt[1]);
    //   const northEast = L.latLng(loc_rpt[2], loc_rpt[3]);
    //   bounds = L.latLngBounds(southWest, northEast);

    //   this.state.locationFilterBounds = bounds;
    //   this.locationFilter.enable();
    //   this.locationFilter.setBounds(bounds);
    // }

    if (this.route.snapshot.queryParamMap.has('loc_rpt')) {
      const loc_rpt = this.route.snapshot.queryParamMap.get('loc_rpt').split(',');
      const southWest = L.latLng(loc_rpt[0], loc_rpt[1]);
      const northEast = L.latLng(loc_rpt[2], loc_rpt[3]);
      bounds = L.latLngBounds(southWest, northEast);
      if (loc_rpt[0] === loc_rpt[2]) {
        this.setToMarkerZoom = true;
      }
      if (!this.isEqualsBounds(bounds)) {
        const ob = this.map.getBounds();
        this.map.fitBounds(bounds, {paddingTopLeft: [21,21], paddingBottomRight: [21,21]});
        setTimeout(() => {
          if (this.isEqualsBounds(ob)) {
            this.getDataByVisibleArea();
            this.processingParams = false;
          }
        }, 10);
        return;
      } else {
        this.getDataByVisibleArea();
        this.processingParams = false;
        return;
      }


    } else if (this.state.stats?.lat && this.state.stats.lat.count > 0) {
      const lat = this.state.stats.lat;
      const lng = this.state.stats.lng;
      if (lat.max === lat.min) {
        lat.min = lat.min - 0.05;
        lat.max = lat.max + 0.05;
        lng.min = lng.min - 0.05;
        lng.max = lng.max + 0.05;
      }
      const southWest = L.latLng(lat.min, lng.min);
      const northEast = L.latLng(lat.max, lng.max);
      bounds = L.latLngBounds(southWest, northEast);
      this.map.fitBounds(bounds);
    } else {
      // hledame primo v mape

      const p = Object.assign({}, this.route.snapshot.queryParams);
      this.state.switchingMap = false;
      this.state.documentProgress = 0;
      this.state.facetsLoading = true;
      this.state.hasError = false;

      if (!p['entity']) {
        p['entity'] = 'dokument';
      }
      p['noFacets'] = 'false';
      p['onlyFacets'] = 'true';
      this.service.search(p as HttpParams).subscribe((resp: SolrResponse) => {
        if (resp.error) {
          this.state.loading = false;
          this.state.facetsLoading = false;
          this.state.hasError = true;
          this.service.showErrorDialog('dialog.alert.error', 'dialog.alert.search_error');
          return;
        }
        this.state.setSearchResponse(resp);

        const lat = this.state.stats.lat;
        const lng = this.state.stats.lng;
        if (lat.max === lat.min) {
          lat.min = lat.min - 0.05;
          lat.max = lat.max + 0.05;
          lng.min = lng.min - 0.05;
          lng.max = lng.max + 0.05;
        }
        const southWest = L.latLng(lat.min, lng.min);
        const northEast = L.latLng(lat.max, lng.max);
        bounds = L.latLngBounds(southWest, northEast);
        this.map.fitBounds(bounds);

      });
      return;
    }

    if (this.state.switchingMap) {
      this.setMapType(this.state.numFound);
      this.processResponse(this.state.solrResponse);
      // this.setMarkers(this.state.solrResponse.response.docs, false);
      if (this.showType === 'marker' && this.state.solrResponse.response.numFound > this.state.solrResponse.response.docs.length) {
        this.getDataByVisibleArea();
      }
    } else {
      // this.getDataByVisibleArea();
    }

    this.state.switchingMap = false;


  }

  updateBounds(mapBounds: any, isLocation: boolean, caller: string) {
    //console.log(caller)
    this.stopLoadingMarkers();
    if (!this.isResults) {
      // Jsme v documentu, nepotrebujeme znovu nacist data
      return;
    }
    let bounds = this.map.getBounds();

    if (mapBounds) {
      bounds = mapBounds;
    }

    if (this.locationFilter.isEnabled() && isLocation) {
      // bounds = this.locationFilter.getBounds();
      this.state.locationFilterBounds = this.locationFilter.getBounds();
    }
    this.state.mapBounds = bounds;
    const value = bounds.getSouthWest().lat + ',' + bounds.getSouthWest().lng +
      ',' + bounds.getNorthEast().lat + ',' + bounds.getNorthEast().lng;

    const queryParams: any = { loc_rpt: value, page: 0 };
    if (this.state.locationFilterEnabled) {
      queryParams.vyber = this.state.locationFilterBounds.getSouthWest().lat + ',' +
        this.state.locationFilterBounds.getSouthWest().lng + ',' +
        this.state.locationFilterBounds.getNorthEast().lat + ',' +
        this.state.locationFilterBounds.getNorthEast().lng;
    } else {
      queryParams.vyber = null;
    }

    this.zone.run(() => {
      this.router.navigate([], { queryParams, queryParamsHandling: 'merge' });
    });

  }

  setMapType(count: number) {
    const oldType = this.showType;
    this.showType = 'undefined';
    if (count > this.config.mapOptions.docsForCluster) {
      this.showType = 'heat';
      this.markersList = [];
      this.piansList = [];
      this.markers.clearLayers();
      this.clusters.clearLayers();
    } else if (count > this.maxNumMarkers) {
      this.showType = 'cluster';
      if (oldType !== this.showType) {
        this.clusters.clearLayers();
      }
    } else {
      this.showType = 'marker';
      if (oldType !== this.showType) {
        this.markers.clearLayers();
        this.clusters.clearLayers();
      }
    }

  }

  processResponse(resp: any) {
    this.setMapType(this.state.numFound);
    const byLoc = this.state.entity === 'knihovna_3d' || this.state.entity === 'samostatny_nalez';
    // this.markersList = [];
    // this.piansList = [];

    //console.log(this.showType)
    switch (this.showType) {
      case 'cluster': {
        const p: any = Object.assign({}, this.route.snapshot.queryParams);

        const bounds = this.map.getBounds();
        const value = bounds.getSouthWest().lat + ',' + bounds.getSouthWest().lng +
          ',' + bounds.getNorthEast().lat + ',' + bounds.getNorthEast().lng;
        p.loc_rpt = value;
        p.rows = this.state.numFound;
        p['noFacets'] = 'true';
        p['onlyFacets'] = 'false';
        this.service.getPians(p as HttpParams).subscribe((res: any) => {
          if (byLoc) {
            this.setClusterDataByLoc(res.response.docs);
          } else {
            this.setClusterDataByPian(res.response.docs);
          }
          setTimeout(() => {
            this.state.loading = false;
            this.loadingFinished.emit();
          }, 100)

        });
        break;
      }
      case 'marker': {
        this.getVisibleAreaMarkers(false);
        break;
      }
      case 'heat': {
        // setTimeout(() => {
        //   if (this.state.mapResult) {
        //     this.hitMarker(this.state.mapResult);
        //   }
        // }, 100);
        // break;
      }
    }
  }

  getDataByVisibleArea() {

    // Nejdriv ziskame facets. A podle poctu vysledku nastavime mapType
    const p: any = Object.assign({}, this.route.snapshot.queryParams);

    const bounds = this.map.getBounds();
    const value = bounds.getSouthWest().lat + ',' + bounds.getSouthWest().lng +
      ',' + bounds.getNorthEast().lat + ',' + bounds.getNorthEast().lng;
    p.loc_rpt = value;
    p.rows = 0;
    p['noFacets'] = 'false';
    p['onlyFacets'] = 'true';
    if (!p.entity) {
      p.entity = 'dokument';
    }
    this.state.loading = true;
    this.service.search(p as HttpParams).subscribe((resp: any) => {
      //this.state.loading = false;
      this.state.setSearchResponse(resp);
      this.state.numFound = resp.response.numFound;
      this.distErr = resp.responseHeader.params['facet.heatmap.distErr'];
      this.state.setFacets(resp);
      this.processResponse(resp);
      this.state.facetsLoading = false;
    });
  }

  setClusterDataByPian(docs: any[]) {
    this.markers.clearLayers();
    this.clusters.clearLayers();
    this.markersList = [];
    this.piansList = [];
    docs.forEach(doc => {
      //if (this.state.hasRights(pian.pristupnost, pian.organizace)) {
      const coords = doc.loc_rpt[0].split(',');
      const mrk = this.addMarker({
        id: doc.pian_id,
        isPian: true,
        lat: coords[0],
        lng: coords[1],
        presnost: doc.pian_presnost,
        typ: doc.typ,
        doc: [doc.ident_cely],
        pian_chranene_udaje: doc.pian_chranene_udaje
      });
      //}
    });
    this.clusters.addLayers(this.markersList);
    this.currentZoom = this.map.getZoom();
    this.cd.detectChanges();
  }

  setClusterDataByLoc(docs: any[]) {
    this.markers.clearLayers();
    this.clusters.clearLayers();
    this.markersList = [];
    this.piansList = [];
    docs.forEach(doc => {
      if (doc.loc_rpt) {
        //if (this.state.hasRights(doc.pristupnost, doc.organizace)) {
          const coords = doc.loc_rpt[0].split(',');
          // this.addMarker(doc.ident_cely, false, coords[0], coords[1], '', '', doc, null);
          const mrk = this.addMarker({
            id: doc.ident_cely,
            isPian: false,
            lat: coords[0],
            lng: coords[1],
            presnost: '',
            typ: 'bod',
            doc: [doc.ident_cely],
            pian_chranene_udaje: null
          });
          mrk.addTo(this.clusters);
        //}
      }
    });
    this.clusters.addLayers(this.markersList);
    this.currentZoom = this.map.getZoom();
    this.cd.detectChanges();
  }

  getMarkerById(setResponse: boolean) {
    //this.state.loading = true;
    const p: any = Object.assign({}, this.route.snapshot.queryParams);

    this.service.getId(this.currentMapId, false).subscribe((res: any) => {
      if (setResponse) {
        this.state.setSearchResponse(res, 'map');
      }
      const doc = res.response.docs.find(d => d.ident_cely === this.currentMapId);
      this.state.setMapResult(doc, false);
      this.setMarkers(res.response.docs, false, true);

      if (this.shouldZoomOnMarker) {
        this.zoomOnMapResult(doc);
      }

    });
  }

  markersSubs: any;
  getVisibleAreaMarkers(clean: boolean) {
    this.state.loading = true;
    const p: any = Object.assign({}, this.route.snapshot.queryParams);
    const bounds = this.map.getBounds();
    const value = bounds.getSouthWest().lat + ',' + bounds.getSouthWest().lng +
      ',' + bounds.getNorthEast().lat + ',' + bounds.getNorthEast().lng;
    p.loc_rpt = value;
    if (this.state.solrResponse?.response) {
      p.rows = this.state.solrResponse.response.numFound;
    }
    if (!p.entity) {
      p.entity = 'dokument';
    }
    this.state.solrResponse = null;
    this.markersSubs = this.service.search(p as HttpParams).subscribe((res: any) => {
      this.state.setSearchResponse(res, 'map');
      this.setMarkers(res.response.docs, clean, false);
      if (this.currentMapId) {
        const doc = res.response.docs.find(d => d.ident_cely === this.currentMapId);
        this.state.setMapResult(doc, false);
        if (this.shouldZoomOnMarker) {
          this.zoomOnMapResult(doc);
        }
      } else {
        if (this.setToMarkerZoom) {
          this.map.setView(bounds.getCenter(), this.config.mapOptions.hitZoomLevel);
          this.setToMarkerZoom = false;
        }
      }
      this.state.loading = false;
    });
  }

  findMarker(id: string) {
    return this.markersList.find(m => m.options.id === id);
  }

  addMarker(mr: AppMarkerOptions): any {
    //let appmrk = this.findMarker(mr.id);
    //if (!appmrk) {
      const mrk = L.marker([mr.lat, mr.lng], { id: mr.id, doc: mr.doc, riseOnHover: true, icon: mr.typ === 'bod' ? this.iconPoint : this.icon });
      if (mr.doc.includes(this.currentMapId)) {
        mrk.setIcon(this.hitIcon);
        // mrk.setIcon((mr.typ === 'bod' || mr.typ === 'HES-001135') ? this.hitIconPoint : this.hitIcon);
      }
      // mr.mrk = mrk;
      this.markersList.push(mrk);
      if (mr.isPian) {
        mrk.on('click', (e) => {
          this.setPianId(e.target.options.id, e.target.options.doc);
        });
        const pianInList = this.piansList.find(p => p.id === mr.id);
        if (pianInList) {
          mrk.bindTooltip(this.popUpHtml(mr.id, mr.presnost, pianInList.docIds));
        }
      } else {
        mrk.on('click', (e) => {
          const id = e.target.options.id;
          const doc = this.findMarker(id).doc;
          this.selectMarker(doc);
        });
        mrk.bindTooltip(mr.id);
      }
      return mrk;
    // } else if (mr.isPian) {
    //   // mrk.docId.push(doc.ident_cely);
    //   const pianInList = this.piansList.find(p => p.id === mr.id);
    //   if (pianInList) {
    //     appmrk.bindTooltip(this.popUpHtml(mr.id, mr.presnost, pianInList.docIds));
    //   }
    //   return appmrk;
    // } else {
    //   return appmrk;
    // }
  }

  selectMarker(res) {
    this.zone.run(() => {
      this.state.setMapResult(res, false);
    });
  }

  loadingMarkers = false;

  stopLoadingMarkers() {
    this.state.loading = false;
    this.loadingMarkers = false;
    this.loadingFinished.emit();
    setTimeout(() => {
      this.cd.detectChanges();
    }, 100)
  }

  processMarkersResp(resp: any[], ids: { id: string, docIds: string[] }[], isId: boolean) {
    resp.forEach(pian => {
      const coords = pian.loc_rpt[0].split(',');
      const docIds = ids.find(p => p.id === pian.ident_cely).docIds
      let pianInList = this.piansList.find(p => p.id === pian.ident_cely);
      if (!pianInList) {
        pianInList = { id: pian.ident_cely, presnost: null, typ: null, docIds: docIds }
        this.piansList.push(pianInList);
        // return;
      }
      pianInList.presnost = pian.pian_presnost;
      pianInList.typ = pian.typ;
      docIds.forEach(docId => {
        if (!pianInList.docIds.includes(docId)) {
          pianInList.docIds.push(docId);
        }
      });
      
      const mrk = this.addMarker({
        id: pian.ident_cely,
        isPian: true,
        lat: coords[0],
        lng: coords[1],
        presnost: pian.pian_presnost,
        typ: pian.pian_typ,
        doc: pianInList.docIds,
        pian_chranene_udaje: pian.pian_chranene_udaje
      });
      // if (isId) {
      //   mrk.addTo(this.idmarkers);
      // } else {
      //   mrk.addTo(this.markers);
      // }
        mrk.addTo(this.markers);
      this.addShapeLayer(pian.ident_cely, pian.pian_presnost, pian.pian_chranene_udaje?.geom_wkt.value);
    });
  }

  loadNextMarkers(ids: { id: string, docIds: string[] }[], entity: string, isId: boolean) {
    if (!this.loadingMarkers) {
      return;
    }
    if (ids.length === 0) {
      this.stopLoadingMarkers();
      return;
    }
    const idsSize = 20;
    const ids2 = ids.splice(0, idsSize);
    this.service.getIdAsChild(ids2.map(p => p.id), entity).subscribe((res: any) => {
      this.processMarkersResp(res.response.docs, ids2, isId);
      // if (res.response.docs.length < idsSize) {
      //   // To znamena konec

      //   this.stopLoadingMarkers();
      // } else {
        if (ids.length > 0) {
          this.state.loading = true;
          this.loadNextMarkers(ids, entity, isId)
        } else {
          this.stopLoadingMarkers();
        }
      // }
    });
  }

  setMarkersByPian(docs: SolrDocument[], isId: boolean) {
    const pianIds: { id: string, docIds: string[] }[] = [];
    docs.forEach(doc => {
      if (doc.pian_id && doc.pian_id.length > 0) {
        doc.pian_id.forEach(pian_id => {
          const pianInList = this.piansList.find(p => p.id === pian_id);
          if (!pianInList) {
            const p = pianIds.find(p => p.id === pian_id)
            if (!p) {
              pianIds.push({ id: pian_id, docIds: [doc.ident_cely] });
            } else {
              if (!p.docIds.includes(doc.ident_cely)) {
                p.docIds.push(doc.ident_cely);
              }
            }
          } else {
            if (!pianInList.docIds.includes(doc.ident_cely)) {
              pianInList.docIds.push(doc.ident_cely);
            }
            const p = pianIds.find(p => p.id === pian_id)
            if (p) {
                p.docIds = pianInList.docIds;
            }
          }
        });
      }
    });
    this.state.loading = true;
    this.loadingMarkers = true;
    this.loadNextMarkers(pianIds, 'pian', isId);
  }

  setMarkersByLoc(docs: SolrDocument[], isId: boolean) {
    const pianIds: { id: string, docId: string }[] = [];
    docs.forEach(doc => {

      if (!doc.pian_id) {
        doc.loc_rpt.forEach(loc_rpt => {
        const coords = loc_rpt.split(',');
        const mrk = this.addMarker({
          id: doc.ident_cely,
          isPian: false,
          lat: coords[0],
          lng: coords[1],
          presnost: '',
          typ: 'bod',
          doc: [doc.ident_cely],
          pian_chranene_udaje: null
        });
        // if (isId) {
        //   mrk.addTo(this.idmarkers);
        // } else {
        //   mrk.addTo(this.markers);
        // }
        mrk.addTo(this.markers);
      });
      }

    });
    console.log(this.markersList)
    this.loadingMarkers = false;
    setTimeout(() => {
      this.state.loading = false;
      this.loadingFinished.emit();
    }, 100)
  }



  setMarkers(docs: SolrDocument[], clean: boolean, isId: boolean) {
    if (clean) {
      this.markersList = [];
      this.piansList = [];
      this.markers.clearLayers();
      this.clusters.clearLayers();
    }
    if (this.markers._layers.length + docs.length > this.config.mapOptions.docsForMarker) {
      this.markersList = [];
      this.piansList = [];
      this.markers.clearLayers();
    }
    const byLoc = this.state.entity === 'knihovna_3d' || this.state.entity === 'samostatny_nalez';

    if (byLoc) {
      this.setMarkersByLoc(docs, isId)
    } else {
      if (this.state.entity === 'projekt') {
        // Projekt muze mit bez pian
        this.setMarkersByLoc(docs, isId)
      }
      this.setMarkersByPian(docs, isId);
    }
  }

  getVisibleCount(): number {

    let count = 0;
    if (this.state.solrResponse.facet_counts.facet_heatmaps.loc_rpt) {
      const f = this.state.solrResponse.facet_counts.facet_heatmaps.loc_rpt;
      const counts_ints2D = f.counts_ints2D;

      if (counts_ints2D !== null) {
        counts_ints2D.forEach(row => {
          if (row !== null && row !== 'null') {
            row.forEach((r: number) => {
              count += r;
            });
          }
        });
      }
    }
    return count;
  }

  setPianId(pian_id: string, docId: string[]) {
    this.zone.run(() => {
      if (this.usingMeasure) {
        return;
      }
      this.markersList = [];
      this.piansList = [];
      this.showDetail = true;
      this.state.setFacetChanged();
      const mapId = docId.length === 1 ? docId[0] : null;
      this.router.navigate([], { queryParams: { pian_id, mapId, page: 0 }, queryParamsHandling: 'merge' });
    });
  }

  clearPian() {
    this.router.navigate([], { queryParams: { pian_id: null, page: 0 }, queryParamsHandling: 'merge' });
  }

  clearSelectedMarker() {
    this.markersList.forEach(m => {
      m.setIcon((m.typ === 'bod' || m.typ === 'HES-001135') ? this.iconPoint : this.icon);
      m.setZIndexOffset(0);
      m.options.selected = false;
    });
    this.selectedMarker = [];
  }

  zoomOnMapResult(doc: any) {
    //console.log('zoomOnMapResult')
    this.zoomingOnMarker = true;
    const changed = this.selectedResultId !== doc.ident_cely;
    this.hitMarker(doc);

    const bounds = this.service.getBoundsByDoc(doc);
    this.map.setView(bounds.getCenter(), this.config.mapOptions.hitZoomLevel);
    if (!this.map.getBounds().contains(bounds)) {
      // Markers outside view in this zoom
      this.map.fitBounds(bounds, {paddingTopLeft: [21,21], paddingBottomRight: [21,21]});
    }
    this.selectedResultId = this.state.mapResult.ident_cely;
    
    this.state.mapBounds = this.map.getBounds();
    if (this.currentZoom === this.config.mapOptions.hitZoomLevel) {
      this.getDataByVisibleArea();
    }
    this.shouldZoomOnMarker = false;
  }

  hitMarker(res) {
    if (!res) {
      this.clearSelectedMarker();
      return true;
    }
    const docId = res.ident_cely;
    let changed = true;
    if ((this.selectedMarker.length > 0 && this.selectedMarker[0].options.doc.ident_cely.includes(docId))) {
      // the same or document
      changed = false;
    }
    let ms = this.markersList.filter(mrk => mrk.options.doc.includes(docId));
    //if (changed) {
    this.clearSelectedMarker();
    //}

    if (!ms || ms.length === 0) {
      // this.addMarkerByResult(res);
      return changed;
    }

    let latMax = 0;
    let latMin = 90;
    let lngMax = 0;
    let lngMin = 180;

    const bounds = []
    ms.forEach(m => {
      m.setIcon((m.typ === 'bod' || m.typ === 'HES-001135') ? this.hitIconPoint : this.hitIcon);
      m.setZIndexOffset(100);
      m.options.selected = true;
      const latlng = m.getLatLng();
      latMax = Math.max(latMax, latlng.lat);
      latMin = Math.min(latMin, latlng.lat);
      lngMax = Math.max(lngMax, latlng.lng);
      lngMin = Math.min(lngMin, latlng.lng);
      if (this.showType === 'heat') {
        m.addTo(this.markers);
        if (parseInt(m.presnost) < 4 && m.typ !== 'bod') {
          this.addShapeLayer(m.options.id, m.options.presnost, null);
        }
      }
    });

    if (changed && ms.length > 0) {
      this.zoomingOnMarker = true;
      // this.map.setView(ms[ms.length - 1].mrk.getLatLng(), this.config.mapOptions.hitZoomLevel);
    }
    return changed;
  }

  setHeatData() {
    if (this.state.mapResult) {
      return
    }
    this.state.loading = true;

    if (this.heatmapLayer) {
      this.map.removeLayer(this.heatmapLayer);
    }
    if (!this.state.heatMaps?.loc_rpt) {
      //this.state.loading = false;
      return;
    }
    // const markersToShow = Math.min(this.state.solrResponse.response.numFound, this.markersList.length);

    this.showHeat = this.state.numFound > this.config.mapOptions.docsForCluster
      && this.map.getZoom() < this.markerZoomLevel;
    // this.showHeat = false;
    if (!this.showHeat) {
      // this.state.loading = false;
      return;
    }
    this.data.data = [];
    const f = this.state.heatMaps.loc_rpt;
    const counts_ints2D = f.counts_ints2D;
    // const gridLevel = f.gridLevel;
    const columns = f.columns;
    const rows = f.rows;
    const minX = f.minX;
    const maxX = f.maxX;
    const minY = f.minY;
    const maxY = f.maxY;
    const distX = (maxX - minX) / columns;
    const distY = (maxY - minY) / rows;

    let maxBounds = null;

    if (this.locationFilter.isEnabled()) {
      const bounds = this.locationFilter.getBounds();
      maxBounds = new L.latLngBounds(bounds.getSouthWest(), bounds.getNorthEast());
    }

    if (counts_ints2D !== null) {
      for (let i = 0; i < counts_ints2D.length; i++) {
        if (counts_ints2D[i] !== null && counts_ints2D[i] !== 'null') {
          const row = counts_ints2D[i];
          const lat = maxY - i * distY;
          for (let j = 0; j < row.length; j++) {
            const count = row[j];
            if (count > 0) {
              const lng = minX + j * distX;
              const bounds = new L.latLngBounds([
                [lat, lng],
                [lat - distY, lng + distX]
              ]);
              if (!this.locationFilter.isEnabled() || maxBounds.contains(bounds.getCenter())) {
                this.data.data.push({ lat: bounds.getCenter().lat, lng: bounds.getCenter().lng, count });
              }
            }
          }
        }
      }
    }
    this.config.mapOptions.heatmapOptions.radius = 1.9 * this.distErr;
    this.heatmapLayer = new HeatmapOverlay(this.config.mapOptions.heatmapOptions);
    this.heatmapLayer.setData(this.data);
    this.map.addLayer(this.heatmapLayer);
    setTimeout(() => {
      if (this.state.mapResult) {
        this.hitMarker(this.state.mapResult);
      }
      this.state.loading = false;
      this.loadingFinished.emit();
    }, 10);
  }

  addShapeLayer(ident_cely: string, presnost: string, geom_wkt_c: string) {
    if (!geom_wkt_c) {
      return;
    }
    const wkt = new Wkt.Wkt();
    wkt.read(geom_wkt_c);

    if (wkt.toJson().type !== 'Point') {
      const layer = geoJSON((wkt.toJson() as any), {
        style: () => ({
          color: this.config.mapOptions.shape.color,
          weight: this.config.mapOptions.shape.weight,
          fillColor: this.config.mapOptions.shape.fillColor,
          fillOpacity: this.config.mapOptions.shape.fillOpacity
        })
      });
      const pianInList = this.piansList.find(p => p.id === ident_cely);
      // layer.pianId = ident_cely;
      layer.on('click', (e) => {
        this.setPianId(ident_cely, pianInList.docIds);
      });
      layer.bindTooltip(this.popUpHtml(ident_cely, presnost, pianInList.docIds));
      // layer.addTo(this.overlays);
      layer.addTo(this.markers);
    }
  }

}

