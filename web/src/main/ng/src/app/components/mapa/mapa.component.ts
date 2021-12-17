
import { Component, OnInit, NgZone, Renderer2, Inject, PLATFORM_ID, OnDestroy, Input } from '@angular/core';

import { HttpParams } from '@angular/common/http';
import { Router, ActivatedRoute } from '@angular/router';

import 'src/assets/js/locationfilter';

import { AppService } from 'src/app/app.service';
import { SolrResponse } from 'src/app/shared/solr-response';
import { AppState } from 'src/app/app.state';
import { AppConfiguration } from 'src/app/app-configuration';

import 'node_modules/leaflet.fullscreen/Control.FullScreen.js';
import { geoJSON, marker } from 'leaflet';
import { isPlatformBrowser } from '@angular/common';

declare var L;
import 'leaflet.markercluster';
declare var HeatmapOverlay;

// declare var Wkt;
import * as Wkt from 'wicket';
// import { Wkt } from 'src/typings';


@Component({
  selector: 'app-mapa',
  templateUrl: './mapa.component.html',
  styleUrls: ['./mapa.component.scss']
})
export class MapaComponent implements OnInit, OnDestroy {

  @Input() isResults = true;

  isBrowser: boolean;
  data = {
    data: []
  };

  heatmapLayer;

  locationFilter;

  zoomOptions = {
    zoomInTitle: this.service.getTranslation('zoom in'),
    zoomOutTitle: this.service.getTranslation('zoom out'),
  };

  options = {
    layers: [],
    zoom: 4,
    zoomControl: false,
    zoomSnap: 0,
    center: L.latLng(49.803, 15.496),
    preferCanvas: true
  };

  icon = L.icon({
    iconSize: [25, 41],
    iconAnchor: [13, 41],
    iconUrl: 'assets/img/marker-icon.png',
    shadowUrl: 'assets/img/marker-shadow.png'
  });

  hitIcon = L.icon({
    iconSize: [25, 41],
    iconAnchor: [13, 41],
    iconUrl: 'assets/img/marker-icon-hit.png',
    shadowUrl: 'assets/img/marker-shadow.png'
  });

  params: HttpParams;

  maxNumMarkers = 200;
  markerZoomLevel = 16;
  currentZoom: number;
  zoomingOnMarker = true;

  map;
  // markers = new L.featureGroup();
  markers = new L.markerClusterGroup();

  markersList: any[] = [];
  selectedMarker = [];

  showHeat: boolean;
  mapReady = false;
  subs: any[] = [];

  layersControl = {};

  osm = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: this.config.mapOptions.maxZoom,
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> '
  });

  // osmColor = L.tileLayer('http://tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: 'OSM map', maxZoom: 25, maxNativeZoom: 19, minZoom: 6 });
  // cuzkWMS = L.tileLayer.wms('http://services.cuzk.cz/wms/wms.asp?', { layers: 'KN', maxZoom: 25, maxNativeZoom: 20, minZoom: 17, opacity: 0.5 });
  // cuzkWMS2 = L.tileLayer.wms('http://services.cuzk.cz/wms/wms.asp?', { layers: 'prehledka_kat_uz', maxZoom: 25, maxNativeZoom: 20, minZoom: 12, opacity: 0.5 });
  cuzkOrt = L.tileLayer('http://ags.cuzk.cz/arcgis/rest/services/ortofoto_wm/MapServer/tile/{z}/{y}/{x}?blankTile=false', { layers: 'ortofoto_wm', maxZoom: 25, maxNativeZoom: 19, minZoom: 6 });
  cuzkEL = L.tileLayer.wms('http://ags.cuzk.cz/arcgis2/services/dmr5g/ImageServer/WMSServer?', { layers: 'dmr5g:GrayscaleHillshade', maxZoom: 25, maxNativeZoom: 20, minZoom: 6 });
  cuzkZM = L.tileLayer('http://ags.cuzk.cz/arcgis/rest/services/zmwm/MapServer/tile/{z}/{y}/{x}?blankTile=false', { layers: 'zmwm', maxZoom: 25, maxNativeZoom: 19, minZoom: 6 });

  baseLayers = {
    "ČÚZK - Základní mapy ČR": this.cuzkZM,
    "ČÚZK - Ortofotomapa": this.cuzkOrt,
    "ČÚZK - Stínovaný reliéf 5G": this.cuzkEL,
    "OpenStreetMap": this.osm,
  };
  overlays = new L.featureGroup();

  constructor(
    @Inject(PLATFORM_ID) private platformId: any,
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
    this.subs.forEach(s => s.unsubscribe());
  }

  ngOnInit(): void {

    this.options.layers = [this.osm];

    this.layersControl = {
      baseLayers: this.baseLayers
    }


    this.options.zoom = this.config.mapOptions.zoom;
    if (this.state.mapResult) {
      this.options.zoom = this.config.mapOptions.hitZoomLevel;
    }
    this.options.center = L.latLng(this.config.mapOptions.centerX, this.config.mapOptions.centerY);

    L.control.zoom(this.zoomOptions);

    this.params = this.route.snapshot.queryParams as HttpParams;

    this.subs.push(this.state.mapResultChanged.subscribe((res: any) => {
      if (this.mapReady) {
        this.hitMarker(res);
      }
    }));

    this.subs.push(this.state.resultsChanged.subscribe(res => {
      if (this.mapReady) {
        this.setData();
      }
    }));

    this.subs.push(this.state.facetsChanged.subscribe(res => {
      const start = new Date();
      if (this.mapReady) {
        this.setHeatData();
      }
    }));

    this.subs.push(this.state.mapViewChanged.subscribe(res => {
      if (this.mapReady) {
        setTimeout(() => {
          this.map.invalidateSize({ pan: false });
        }, 500);
      }
    }));

    if (!this.isResults && this.mapReady) {
      this.setData();
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

  setData() {
    if (this.state.solrResponse) {
      const showCluster = this.state.solrResponse.response.numFound > this.maxNumMarkers && this.map.getZoom() < this.markerZoomLevel;
      if (showCluster) {
        this.state.loading = true;
        this.markers = new L.markerClusterGroup();
        this.markersList = [];
        const p = Object.assign({}, this.route.snapshot.queryParams);
        p.rows = this.state.solrResponse.response.numFound;
        this.service.getPians(p as HttpParams).subscribe((res: any) => {
          if (this.state.entity === 'knihovna_3d' || this.state.entity === 'samostatny_nalez') {
            this.setClusterDataByLoc(res.response.docs);
          } else {
            this.setClusterDataByPian(res.response.docs);
          }
          this.state.loading = false;
        });
      } else {
        this.setMarkersData();
        this.markersList.forEach(m => {
          if (m.pianPresnost < 4) {
            this.addShape(m.pianId, m.pianPresnost, m.docId.length);
          }
        });
        this.state.loading = false;
      }


      if (this.state.locationFilterEnabled) {
        this.locationFilter.enable();
      } else {
        this.locationFilter.disable();
      }
      setTimeout(() => {
        if (this.state.mapResult) {
          this.hitMarker(this.state.mapResult);
        } else {
          // this.fitOnMarkers();
        }

      }, 100);

      if (this.state.solrResponse.response.docs.length === 1) {
        this.state.setMapResult(this.state.solrResponse.response.docs[0], false)
      }
    }
  }

  fitOnMarkers() {
    // Find markers boundary and change map view

    if (this.state.stats?.lat && this.state.stats.lat.count > 0) {
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
      const bounds = L.latLngBounds(southWest, northEast);
      console.log(bounds);
      this.map.fitBounds(bounds.pad(.03));
      this.locationFilter.setBounds(this.map.getBounds().pad(-0.95));

    }
  }

  setClusterDataByPian(docs: any[]) {
    this.markers = new L.markerClusterGroup();
    docs.forEach(doc => {
      if (doc.pian && doc.pian.length > 0) {
        doc.pian.forEach(pian => {
          if (this.state.hasRights(pian.pristupnost, doc.organizace)) {
            const pianId = pian.ident_cely;
            const presnost = pian.presnost;
            let mrk = this.markerExists(pianId);
            if (!mrk) {
              mrk = L.marker([pian.centroid_n, pian.centroid_e], { pianId, icon: this.icon, docId: [], riseOnHover: true });
              this.markersList.push(mrk);
              mrk.pianId = pianId;
              mrk.pianPresnost = presnost;
              mrk.docId = [doc.ident_cely];
              mrk.on('click', (e) => {
                this.setPianId(e.target.pianId);
              });
              mrk.bindTooltip(this.popUpHtml(pianId, presnost, mrk.docId)).openTooltip();
              // mrk.addTo(this.markers);
            } else {
              mrk.docId.push(doc.ident_cely);
              mrk.bindTooltip(this.popUpHtml(pianId, presnost, mrk.docId)).openTooltip();
            }
          }
        });
      }
    });
    this.markers.addLayers(this.markersList);
    this.currentZoom = this.map.getZoom();
  }

  setClusterDataByLoc(docs: any[]) {
    this.markers = new L.markerClusterGroup();
    this.markersList = [];
    docs.forEach(doc => {
      if (doc.loc_rpt) {
        if (this.state.hasRights(doc.pristupnost, doc.organizace)) {
          let mrk = this.markerExists(doc.ident_cely);
          if (!mrk) {
            const coords = doc.loc_rpt[0].split(',');
            mrk = L.marker([coords[0], coords[1]], { icon: this.icon, docId: '', doc, riseOnHover: true });
            this.markersList.push(mrk);
            mrk.docId = doc.ident_cely;
            mrk.doc = doc;
            mrk.on('click', (e) => {
              this.setMarker(e.target.doc);
            });
            mrk.bindTooltip(doc.ident_cely).openTooltip();
            // mrk.addTo(this.markers);
          }
        }
      }
    });
    this.markers.addLayers(this.markersList);
    this.currentZoom = this.map.getZoom();
  }

  setMarkersData() {
    this.markersList = [];
    this.markers = new L.featureGroup();
    //this.markers = new L.markerClusterGroup();
    this.state.solrResponse.response.docs.forEach(doc => {
      if (doc.pian && doc.pian.length > 0) {
        doc.pian.forEach(pian => {
          if (this.state.hasRights(pian.pristupnost, doc.organizace)) {
            const pianId = pian.ident_cely;
            const presnost = pian.presnost;
            let mrk = this.markerExists(pianId);
            if (!mrk) {
              mrk = L.marker([pian.centroid_n, pian.centroid_e], { pianId, icon: this.icon, docId: [], riseOnHover: true });
              this.markersList.push(mrk);
              mrk.pianId = pianId;
              mrk.pianPresnost = presnost;
              mrk.docId = [doc.ident_cely];
              mrk.on('click', (e) => {
                this.setPianId(e.target.pianId);
              });
              mrk.bindTooltip(this.popUpHtml(pianId, presnost, mrk.docId.length)).openTooltip();
              mrk.addTo(this.markers);
            } else {
              mrk.docId.push(doc.ident_cely);
              mrk.bindTooltip(this.popUpHtml(pianId, presnost, mrk.docId.length)).openTooltip();
            }
          }
        });
      } else if (doc.loc_rpt) {
        if (this.state.hasRights(doc.pristupnost, doc.organizace)) {
          let mrk = this.markerExists(doc.ident_cely);
          if (!mrk) {
            const coords = doc.loc_rpt[0].split(',');
            mrk = L.marker([coords[0], coords[1]], { icon: this.icon, docId: '', doc, riseOnHover: true });
            this.markersList.push(mrk);
            mrk.docId = doc.ident_cely;
            mrk.doc = doc;
            mrk.on('click', (e) => {
              this.setMarker(e.target.doc);
            });
            mrk.bindTooltip(doc.ident_cely).openTooltip();
            mrk.addTo(this.markers);
          }
        }
      }

    });
    this.currentZoom = this.map.getZoom();
  }

  setMarker(res) {
    this.zone.run(() => {
      this.state.setMapResult(res, false);
    });
  }

  setPianId(pian_id: string) {
    this.zone.run(() => {
      this.router.navigate([], { queryParams: { pian_id, page: 0 }, queryParamsHandling: 'merge' });
    });
  }

  clearPian() {
    this.router.navigate([], { queryParams: { pian_id: null, page: 0 }, queryParamsHandling: 'merge' });
  }

  clearSelectedMarker() {

    this.selectedMarker.forEach(m => {
      m.setIcon(L.icon({
        iconUrl: 'assets/img/marker-icon.png'
      }));
      m.setZIndexOffset(0);
    });
    this.selectedMarker = [];
  }

  hitMarker(res) {
    if (!res) {
      this.clearSelectedMarker();
      // this.fitOnMarkers();
      return;
    }
    const docId = res.ident_cely;
    let changed = true;
    if ((this.selectedMarker.length > 0 && this.selectedMarker[0].docId.includes(docId))) {
      // the same or document
      changed = false;
    }
    const ms = this.markersList.filter(mrk => mrk.docId.includes(docId));
    this.clearSelectedMarker();
    ms.forEach(m => {
      // m.fire('click');
      m.setIcon(this.hitIcon);
      m.setZIndexOffset(100);
    });
    if (changed && ms.length > 0) {
      this.zoomingOnMarker = true;
      if (this.map.getZoom() < this.config.mapOptions.hitZoomLevel) {
        this.map.setView(ms[ms.length - 1].getLatLng(), this.config.mapOptions.hitZoomLevel);
      } else {
        this.map.setView(ms[ms.length - 1].getLatLng());
      }

      // setTimeout(() => {
      //   this.currentZoom = this.map.getZoom();
      //   // this.map.setZoom(this.hitZoomLevel);
      // }, 1000);
    }
    if (!ms || ms.length === 0) {
      this.state.mapResult = null;
      //this.fitOnMarkers();
    }
    this.selectedMarker = ms;
  }

  markerExists(pianId: string) {
    return this.markersList.find(m => m.pianId === pianId);
  }

  setPianFilter(pianId: string) {
    console.log('setPianFilter');
    this.zone.run(() => {
      this.router.navigate([], { queryParams: { pian_ident_cely: pianId, page: 0 }, queryParamsHandling: 'merge' });
    });
  }

  popUpHtml(id: string, presnost: string, pocet: number) {
    const t = this.service.getTranslation('entities.' + this.state.entity + '.title');
    return id + ' (' + this.service.getHeslarTranslation(presnost, 'presnost') + ') (' + t + ': ' + pocet + ')';
  }

  onMapReady(map: L.Map) {
    this.map = map;

    map.addControl(L.control.zoom(this.zoomOptions));

    this.locationFilter = new L.LocationFilter({
      adjustButton: false
    });

    L.setOptions(this.locationFilter, {
      enableButton: {
        enableText: this.service.getTranslation('select area'),
        disableText: this.service.getTranslation('remove selection')
      }
    });

    map.addLayer(this.locationFilter);

    //this.markers = new L.featureGroup();
    this.markers = new L.markerClusterGroup();
    map.addLayer(this.markers);

    map.on('enterFullscreen', () => map.invalidateSize());
    map.on('exitFullscreen', () => map.invalidateSize());
    let bounds = map.getBounds();
    if (this.route.snapshot.queryParamMap.has('loc_rpt')) {
      const loc_rpt = this.route.snapshot.queryParamMap.get('loc_rpt').split(',');
      const southWest = L.latLng(loc_rpt[0], loc_rpt[1]);
      const northEast = L.latLng(loc_rpt[2], loc_rpt[3]);
      bounds = L.latLngBounds(southWest, northEast);

      this.locationFilter.setBounds(bounds);
      this.map.fitBounds(bounds);
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
      this.map.fitBounds(bounds.pad(.03));
      this.locationFilter.setBounds(this.map.getBounds().pad(-0.95));

      // } else if(!this.isResults) {
      //   this.locationFilter.setBounds(bounds.pad(-0.95));
    } else {
      this.locationFilter.setBounds(bounds.pad(-0.95));
    }

    map.on('zoomend', (e) => {
      if (!this.zoomingOnMarker) {
        this.updateBounds(map.getBounds());
      }
      this.zoomingOnMarker = false;
    });
    map.on('dragend', () => {
      this.updateBounds(map.getBounds());
    });
    map.on('fullscreenchange', () => {
      this.updateBounds(map.getBounds());
    });
    map.on('resize', () => {
      this.updateBounds(map.getBounds());
    });

    this.locationFilter.on('change', (e) => {
      this.updateBounds(null);
    });

    this.locationFilter.on('enabled', () => {
      this.state.locationFilterEnabled = true;
      // this.updateBounds(bounds);
    });

    this.locationFilter.on('disabled', () => {
      this.state.locationFilterEnabled = false;
      this.updateBounds(null);
    });

    if (!this.isResults) {
      this.setData();
    }
    this.setHeatData();
    this.mapReady = true;
  }

  debounce(func, wait, immediate) {
    var timeout;
    return function () {
      var context = this, args = arguments;
      clearTimeout(timeout);
      timeout = setTimeout(function () {
        timeout = null;
        if (!immediate) func.apply(context, args);
      }, wait);
      if (immediate && !timeout) func.apply(context, args);
    };
  }

  updateBounds(mapBounds) {
    if (!this.isResults) {
      // Jsme v documentu, nepotrebujeme znovu nacist data
      return;
    }
    let bounds = this.map.getBounds();
    if (mapBounds) {
      bounds = mapBounds;
    }

    if (this.locationFilter.isEnabled()) {
      bounds = this.locationFilter.getBounds();
    } else {
      this.locationFilter.setBounds(bounds.pad(-0.95));
    }

    const value = bounds.getSouthWest().lat + ',' + bounds.getSouthWest().lng +
      ',' + bounds.getNorthEast().lat + ',' + bounds.getNorthEast().lng;

    this.zone.run(() => {
      this.router.navigate([], { queryParams: { loc_rpt: value, page: 0 }, queryParamsHandling: 'merge' });
    });

  }

  setHeatData() {
    if (this.heatmapLayer) {
      this.map.removeLayer(this.heatmapLayer);
    }
    if (!this.state.heatMaps?.loc_rpt) {
      return;
    }
    const markersToShow = Math.min(this.state.solrResponse.response.numFound, this.markersList.length);

    this.showHeat = this.state.solrResponse.response.numFound > this.maxNumMarkers && this.map.getZoom() < this.markerZoomLevel;
    this.showHeat = false;
    if (!this.showHeat) {
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
    this.config.mapOptions.heatmapOptions.radius = 1.9 * this.state.solrResponse.responseHeader.params['facet.heatmap.distErr'];
    this.heatmapLayer = new HeatmapOverlay(this.config.mapOptions.heatmapOptions);
    this.heatmapLayer.setData(this.data);
    this.map.addLayer(this.heatmapLayer);
  }

  addShapes() {
    // const z = "LINESTRING(50.61908,15.90431 50.61912,15.90433 50.61930,15.90411)";
    // wkt.read(z);
    this.state.solrResponse.response.docs.forEach(doc => {
      if (doc.pian && doc.pian.length > 0) {
        doc.pian.forEach((pian: any) => {

        });
      }
    });
  }

  addShape(ident_cely: string, presnost: string, pocet: number) {
    this.service.getWKT(ident_cely).subscribe((resp: any) => {
      // console.log(ident_cely, resp.geom_wkt_c);
      const wkt = new Wkt.Wkt();
      wkt.read(resp.geom_wkt_c);
      if (wkt.toJson().type !== 'Point') {
        const layer = geoJSON((wkt.toJson() as any), {
          style: () => ({
            color: this.config.mapOptions.shape.color,
            weight: this.config.mapOptions.shape.weight,
            fillColor: this.config.mapOptions.shape.fillColor,
            fillOpacity: this.config.mapOptions.shape.fillOpacity
          })
        });
        // layer.pianId = ident_cely;
        layer.on('click', (e) => {
          this.zone.run(() => {
            this.setPianId(ident_cely);
          });
        });
        layer.bindTooltip(this.popUpHtml(ident_cely, presnost, pocet)).openTooltip();
        // layer.addTo(this.overlays);
        layer.addTo(this.markers);
      }
    });

  }

}

