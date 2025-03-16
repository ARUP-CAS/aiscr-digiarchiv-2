import { MediaMatcher } from '@angular/cdk/layout';
import { ChangeDetectorRef, Component, Inject, NgZone, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';

declare var L;
import 'leaflet.markercluster';
import 'src/assets/js/locationfilter';
import 'leaflet.polylinemeasure';
declare var HeatmapOverlay: any;
import * as Wkt from 'wicket';
import { HttpParams } from '@angular/common/http';
import 'node_modules/leaflet.fullscreen/Control.FullScreen.js';
import { Control, geoJSON, LatLngBounds, Map, Marker, marker } from 'leaflet';

@Component({
  selector: 'app-map-view',
  templateUrl: './map-view.component.html',
  styleUrls: ['./map-view.component.scss']
})
export class MapViewComponent {

  isBrowser: boolean;

  // Control mobile view
  opened = false;
  matcher: MediaQueryList;
  sideNavMapBreakPoint: string = "(min-width: 1280px)";

  loading = false;
  itemSize = 70;
  vsSize = 0;

  subs: any[] = [];
  math = Math;

  docs: SolrDocument[] = [];

  // Map config

  zoomOptions = {
    zoomInTitle: this.service.getTranslation('map.desc.zoom in'),
    zoomOutTitle: this.service.getTranslation('map.desc.zoom out'),
    position: 'topright'
  };

  options = {
    layers: [],
    maxZoom: 18,
    zoom: 4,
    zoomControl: false,
    wheelDebounceTime: 200,
    zoomSnap: 0,
    center: L.latLng(49.803, 15.496),
    preferCanvas: true
  };

  layersControl = { baseLayers: {}, overlays: {} };
  osmInfo = '<span aria-hidden="true"> | </span>Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a>. ';

  osm = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: this.config.mapOptions.maxZoom,
    maxNativeZoom: 19,
    name: 'osm'
  });

  info: string;
  activeBaseLayerOSM: boolean = true;

  cuzkOrt = L.tileLayer('https://ags.cuzk.cz/arcgis1/rest/services/ORTOFOTO_WM/MapServer/tile/{z}/{y}/{x}?blankTile=false', { layers: 'ortofoto_wm', maxZoom: 25, maxNativeZoom: 19, minZoom: 6 });
  cuzkEL = L.tileLayer.wms('http://ags.cuzk.cz/arcgis2/services/dmr5g/ImageServer/WMSServer?', { layers: 'dmr5g:GrayscaleHillshade', maxZoom: 25, maxNativeZoom: 20, minZoom: 6 });
  cuzkZM = L.tileLayer('https://ags.cuzk.cz/arcgis1/rest/services/ZTM_WM/MapServer/tile/{z}/{y}/{x}?blankTile=false', { layers: 'zmwm', maxZoom: 25, maxNativeZoom: 19, minZoom: 6 });
  baseLayers: any = {
    "ČÚZK - Základní mapy ČR": this.cuzkZM,
    "ČÚZK - Ortofotomapa": this.cuzkOrt,
    "ČÚZK - Stínovaný reliéf 5G": this.cuzkEL,
    "OpenStreetMap": this.osm,
  };

  cuzkWMS = L.tileLayer.wms('http://services.cuzk.cz/wms/wms.asp?', { layers: 'KN', maxZoom: 25, maxNativeZoom: 20, minZoom: 17, opacity: 0.5 });
  cuzkWMS2 = L.tileLayer.wms('http://services.cuzk.cz/wms/wms.asp?', { layers: 'prehledka_kat_uz', maxZoom: 25, maxNativeZoom: 20, minZoom: 12, opacity: 0.5 });
  overlays: any = {
    "ČÚZK - Katastrální mapa": this.cuzkWMS,
    "ČÚZK - Katastrální území": this.cuzkWMS2,
  };
  dataLayerName: string = 'data';

  lfAttribution = '<span aria-hidden="true"> | </span><a href="https://leafletjs.com" title="A JavaScript library for interactive maps"><svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="12" height="8" viewBox="0 0 12 8" class="leaflet-attribution-flag"><path fill="#4C7BE1" d="M0 0h12v4H0z"></path><path fill="#FFD500" d="M0 4h12v3H0z"></path><path fill="#E0BC00" d="M0 7h12v1H0z"></path></svg> Leaflet</a>';



  icon = L.divIcon({ className: 'map-pin', iconSize: null });
  hitIcon = L.divIcon({ className: 'map-pin-hit', iconSize: null });
  iconPoint = L.divIcon({ className: 'map-pin-point', iconSize: null });
  hitIconPoint = L.divIcon({ className: 'map-pin-hit-point', iconSize: null });

  maxNumMarkers = 0;
  markerZoomLevel = 16;

  // Map state
  map: L.Map;
  mapReady = false;
  markers = new L.featureGroup();
  clusters = new L.markerClusterGroup();
  heatmapLayer: any;
  locationFilter: any;
  usingMeasure = false;
  markersActive = true;
  isDocumentHandle = false;
  loadingMarkers = false;
  settingsBounds = false;
  currentMapId: string = null;
  currentZoom: number;
  currentLocBounds: string;
  showType = 'marker'; // 'heat', 'cluster', 'marker'
  shouldPad = true;
  facetsChanged = false;

  constructor(
    @Inject(PLATFORM_ID) platformId: any,
    private titleService: Title,
    private route: ActivatedRoute,
    private router: Router,
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService,
    public mediaMatcher: MediaMatcher,
    private zone: NgZone,
    private cd: ChangeDetectorRef
  ) {
    this.state.bodyClass = 'app-page-results';
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    this.setTitle();
    // this.initLayers();
    this.options.center = L.latLng(this.config.mapOptions.centerX, this.config.mapOptions.centerY);
    this.subs.push(this.service.currentLang.subscribe(res => {
      this.setTitle();
      if (this.mapReady) {
        this.setAttribution();
        this.initLayers();
        this.translateControls();
      }
    }));

    this.subs.push(this.state.mapResultChanged.subscribe(res => {
      if (window.innerWidth < this.config.hideMenuWidth) {
        this.opened = false;
      }
    }));

    this.subs.push(this.route.queryParams.subscribe(val => {
      if (this.mapReady) {
        this.paramsChanged();
      }
    }));

    this.subs.push(this.state.facetsChanged.subscribe(res => {
      const start = new Date();
      if (this.mapReady) {
        if (res === 'direct') {
          this.clearData();
          this.facetsChanged = true;
        }
      }
    }));


    // set side nav map breakpoint
    this.matcher = this.mediaMatcher.matchMedia(this.sideNavMapBreakPoint);
    this.matcher.addEventListener('change', (e) => {
      this.myListener(e);
    });
  }

  ngOnDestroy() {
    this.matcher.removeEventListener('change', (e) => {
      this.myListener(e);
    });

    this.subs.forEach(s => s.unsubscribe());
  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('navbar.desc.logo_desc')
      + ' | ' + this.service.getTranslation('title.map')
      + ' - ' + this.service.getTranslation('entities.' + this.state.entity + '.title'));
  }

  // set opened for sidenav
  myListener(event: any) {
    this.zone.run(() => {
      this.opened = event.matches;
    });
  }

  paramsChanged() {
    if (this.facetsChanged) {
      this.getDataByVisibleArea();
      this.facetsChanged = false;
      return;
    }
    const mapIdChanged = this.currentMapId !== this.route.snapshot.queryParamMap.get('mapId')
    this.currentMapId = this.route.snapshot.queryParamMap.get('mapId');

    // const locrptChanged = this.currentLocBounds !== this.route.snapshot.queryParamMap.get('loc_rpt');
    let locrptChanged = false;
    this.currentLocBounds = this.route.snapshot.queryParamMap.get('loc_rpt');

    if (this.route.snapshot.queryParamMap.has('loc_rpt')) {
      const loc_rpt = this.route.snapshot.queryParamMap.get('loc_rpt').split(',');
      const southWest = L.latLng(loc_rpt[0], loc_rpt[1]);
      const northEast = L.latLng(loc_rpt[2], loc_rpt[3]);
      const bounds: LatLngBounds = L.latLngBounds(southWest, northEast);
        if (loc_rpt[0] === loc_rpt[2]) {
          // this.setToMarkerZoom = true;
        }
      locrptChanged = !this.map.getBounds().equals(bounds);
      if (locrptChanged) {
        if (this.shouldPad) {
          this.fitBounds(bounds, { paddingTopLeft: [21, 21], paddingBottomRight: [21, 21] });
        } else {
          this.fitBounds(bounds, null);
        }
        this.shouldPad = false;
      }
    }



  }

  clearData() {
    this.markers.clearLayers();
    this.clusters.clearLayers();
  }

  onMapReady(map: L.Map) {
    this.map = map;

    this.locationFilter = new L.LocationFilter({
      bounds: this.state.locationFilterBounds,
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
    }).addTo(map);

    map.on('polylinemeasure:toggle', (e: any) => {
      this.usingMeasure = e.sttus
    });

    map.addControl(L.control.zoom(this.zoomOptions));
    L.control.scale({ position: 'bottomleft', imperial: false }).addTo(this.map);


    this.markers.clearLayers();
    this.clusters.clearLayers();
    // map.addLayer(this.markers);

    map.on('enterFullscreen', () => map.invalidateSize());
    map.on('exitFullscreen', () => map.invalidateSize());

    map.on('baselayerchange', (e) => {
      this.activeBaseLayerOSM = e.layer.options['name'] === 'osm';
      this.setAttribution();
    });

    map.on('overlayadd', (e) => {
      if (e.name === this.dataLayerName) {
        
        this.markersActive = true;
        this.getDataByVisibleArea();
      }
    });

    map.on('overlayremove', (e) => {
      if (e.name === this.dataLayerName) {
        this.markersActive = false;
        this.clusters.clearLayers();
      }
    });

    map.on('zoomend', (e) => {
      console.log('zoomend')
      this.state.mapBounds = this.map.getBounds();
      this.doZoom();
    });

    map.on('dragend', () => {
      //console.log('dragend')
      this.updateBounds();
    });

    map.on('fullscreenchange', () => {
      //console.log('fullscreenchange')
      this.updateBounds();
    });

    map.on('resize', () => {
      //console.log('resize')
      this.updateBounds();
    });

    this.locationFilter.on('change', (e) => {
      if (JSON.stringify(this.state.locationFilterBounds) !== JSON.stringify(this.locationFilter.getBounds())) {
        this.updateVyber();
      }
    });

    this.locationFilter.on('enabled', () => {
      if (!this.state.locationFilterEnabled) {
        this.state.locationFilterEnabled = true;
        this.locationFilter.setBounds(this.map.getBounds().pad(this.config.mapOptions.selectionInitPad));
        this.updateVyber();
      }
    });

    this.locationFilter.on('disabled', () => {
      this.state.locationFilterEnabled = false;
      this.state.locationFilterBounds = null;
      this.updateVyber();
    });

    this.mapReady = true;
    setTimeout(() => {
    this.paramsChanged();
    this.setAttribution();
    }, 1);

  }

  markersSubs: any;
  stopLoadingMarkers() {
    this.loading = false;
    this.loadingMarkers = false;

    if (this.markersSubs) {
      this.markersSubs.unsubscribe();
    }
    setTimeout(() => {
      this.cd.detectChanges();
    }, 100)
  }

  zoomingCount = 0;
  doZoom() {
    if (this.settingsBounds) {
      console.log(this.map.getBounds())
      // maps bounds changed by code. Ignore event
      this.getDataByVisibleArea();
      this.settingsBounds = false;
      return;
    }
    this.zoomingCount++;
    setTimeout(() => {
      this.zoomingCount--;
      if (this.zoomingCount < 1) {
        this.updateBounds();
      }
    }, 100)
  }

  updateVyber() {
    if (this.isDocumentHandle) {
      // Jsme v documentu, nepotrebujeme znovu nacist data
      return;
    }
    this.stopLoadingMarkers();


    if (this.locationFilter.isEnabled()) {
      this.state.locationFilterBounds = this.locationFilter.getBounds();
    }
    const queryParams: any = { page: 0 };
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

  updateBounds() {
    if (this.isDocumentHandle) {
      // Jsme v documentu, nepotrebujeme znovu nacist data
      return;
    }
    if (this.settingsBounds) {
      // maps bounds changed by code. Ignore event
      this.settingsBounds = false;
      return;
    }
    this.stopLoadingMarkers();
    let bounds = this.map.getBounds();
    this.state.mapBounds = bounds;
    const value = bounds.getSouthWest().lat + ',' + bounds.getSouthWest().lng +
      ',' + bounds.getNorthEast().lat + ',' + bounds.getNorthEast().lng;

    // const bb = bounds.toBBoxString(); 

    const queryParams: any = { loc_rpt: value, page: 0 };
    this.zone.run(() => {
      this.router.navigate([], { queryParams, queryParamsHandling: 'merge' });
    });

  }

  fitBounds(bounds: LatLngBounds, options: any) {
    console.log('a')
    this.settingsBounds = true;
    this.map.fitBounds(bounds, options);
    console.log('b')

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

  getDataByVisibleArea() {

    // Nejdriv ziskame facets. A podle poctu vysledku nastavime mapType
    const p: any = Object.assign({}, this.route.snapshot.queryParams);

    const bounds = this.map.getBounds();
    const value = bounds.getSouthWest().lat + ',' + bounds.getSouthWest().lng +
      ',' + bounds.getNorthEast().lat + ',' + bounds.getNorthEast().lng;
    p.loc_rpt = value;
    p.rows = 0;
    p['mapa'] = 'true';
    p['noFacets'] = 'false';
    p['onlyFacets'] = 'true';
    if (!p.entity) {
      p.entity = 'dokument';
    }
    this.loading = true;
    this.service.search(p as HttpParams).subscribe((resp: any) => {
      this.loading = false;
      this.state.numFound = resp.response.numFound;
      this.state.setFacets(resp);
      this.processResponse(resp);
      this.state.facetsLoading = false;
    });
  }

  processResponse(resp: any) {
    this.setMapType(resp.response.numFound);
    const byLoc = this.state.entity === 'knihovna_3d' || this.state.entity === 'samostatny_nalez';
    switch (this.showType) {
      case 'cluster': {
        const p: any = Object.assign({}, this.route.snapshot.queryParams);

        const bounds = this.map.getBounds();
        const value = bounds.getSouthWest().lat + ',' + bounds.getSouthWest().lng +
          ',' + bounds.getNorthEast().lat + ',' + bounds.getNorthEast().lng;
        p.loc_rpt = value;
        p.rows = resp.response.numFound;
        p['noFacets'] = 'true';
        p['onlyFacets'] = 'false';
        this.service.getPians(p as HttpParams).subscribe((res: any) => {
          if (byLoc) {
            this.setClusterDataByLoc(res.response.docs);
          } else {
            this.setClusterDataByPian(res.response.docs);
          }
          setTimeout(() => {
            this.loading = false;
          }, 100)

        });
        break;
      }
      case 'marker': {
        this.getVisibleAreaMarkers(false);
        break;
      }
      case 'heat': {
        this.setHeatData(resp.responseHeader.params['facet.heatmap.distErr']);
        break;
      }
    }
  }

  setMapType(count: number) {
    const oldType = this.showType;
    this.showType = 'undefined';
    if (count > this.config.mapOptions.docsForCluster) {
      this.showType = 'heat';
      this.markers.clearLayers();
      this.clusters.clearLayers();
    } else if (count > this.maxNumMarkers) {
      this.showType = 'cluster';
      if (this.heatmapLayer) {
        this.map.removeLayer(this.heatmapLayer);
      }
      if (oldType !== this.showType) {
        this.clusters.clearLayers();
      }
    } else {
      this.showType = 'marker';
      if (this.heatmapLayer) {
        this.map.removeLayer(this.heatmapLayer);
      }
      if (oldType !== this.showType) {
        this.markers.clearLayers();
        this.clusters.clearLayers();
      }
    }

  }

  setHeatData(distErr: number) {
    if (this.state.mapResult) {
      return
    }
    this.loading = true;

    if (this.heatmapLayer) {
      this.map.removeLayer(this.heatmapLayer);
    }
    if (!this.state.heatMaps?.loc_rpt) {
      //this.state.loading = false;
      return;
    }
    // const markersToShow = Math.min(this.state.solrResponse.response.numFound, this.markersList.length);

    const heatData = { data: [] };
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

    let maxBounds: LatLngBounds = null;

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
              const bounds: LatLngBounds = new L.latLngBounds([
                [lat, lng],
                [lat - distY, lng + distX]
              ]);
              if (!this.locationFilter.isEnabled() || maxBounds.contains(bounds.getCenter())) {
                heatData.data.push({ lat: bounds.getCenter().lat, lng: bounds.getCenter().lng, count });
              }
            }
          }
        }
      }
    }
    this.config.mapOptions.heatmapOptions.radius = 1.9 * distErr;
    this.heatmapLayer = new HeatmapOverlay(this.config.mapOptions.heatmapOptions);
    this.heatmapLayer.setData(heatData);
    this.map.addLayer(this.heatmapLayer);
    setTimeout(() => {
      if (this.state.mapResult) {
        //this.hitMarker(this.state.mapResult);
      }
      this.loading = false;
      this.cd.detectChanges();
    }, 10);
  }

  clusterList: any[] = [];
  setClusterDataByPian(docs: any[]) {
    this.markers.clearLayers();
    this.clusters.clearLayers();
    this.clusterList = [];
    docs.forEach(doc => {
      const coords = doc.loc_rpt[0].split(',');
      const mrk = L.marker([coords[0], coords[1]], { id: doc.pian_id, doc: [doc.ident_cely], riseOnHover: true, icon: doc.typ === 'bod' ? this.iconPoint : this.icon });
      this.clusterList.push(mrk);
      // const mrk = this.addMarker({
      //   id: doc.pian_id,
      //   isPian: true,
      //   lat: coords[0],
      //   lng: coords[1],
      //   presnost: doc.pian_presnost,
      //   typ: doc.typ,
      //   doc: [doc.ident_cely],
      //   pian_chranene_udaje: doc.pian_chranene_udaje
      // });
    });
    this.clusters.addLayers(this.clusterList);
    this.currentZoom = this.map.getZoom();
    this.cd.detectChanges();
  }

  setClusterDataByLoc(docs: any[]) {
    this.markers.clearLayers();
    this.clusters.clearLayers();
    this.clusterList = [];
    docs.forEach(doc => {
      if (doc.loc_rpt) {
        if (this.state.hasRights(doc.pristupnost, doc.organizace)) {
          const coords = doc.loc_rpt[0].split(',');
          const mrk = L.marker([coords[0], coords[1]], { id: doc.ident_cely, doc: [doc.ident_cely], riseOnHover: true, icon: this.iconPoint });
          this.clusterList.push(mrk);
          mrk.addTo(this.clusters);
        }
      }
    });
    this.clusters.addLayers(this.clusterList);
    this.currentZoom = this.map.getZoom();
    this.cd.detectChanges();
  }

  getVisibleAreaMarkers(clean: boolean) {
  }

  clearPian() {
    this.router.navigate([], { queryParams: { pian_id: null, page: 0 }, queryParamsHandling: 'merge' });
  }
}
