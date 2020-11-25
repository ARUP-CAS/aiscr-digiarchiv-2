
import { Component, OnInit, NgZone, Renderer2, Inject, PLATFORM_ID, OnDestroy, Input } from '@angular/core';

import { HttpParams } from '@angular/common/http';
import { Router, ActivatedRoute } from '@angular/router';
// import { Renderer } from 'leaflet';


import 'src/assets/js/locationfilter';
// import 'leaflet/dist/images/marker-shadow.png';
// import 'leaflet/dist/images/marker-icon.png';
// import 'leaflet/dist/images/marker-icon-2x.png';

import { AppService } from 'src/app/app.service';
import { SolrResponse } from 'src/app/shared/solr-response';
import { AppState } from 'src/app/app.state';
import { AppConfiguration } from 'src/app/app-configuration';

import 'node_modules/leaflet.fullscreen/Control.FullScreen.js';
import { marker } from 'leaflet';
import { isPlatformBrowser } from '@angular/common';

declare var L;
declare var HeatmapOverlay;


@Component({
  selector: 'app-mapa',
  templateUrl: './mapa.component.html',
  styleUrls: ['./mapa.component.scss']
})
export class MapaComponent implements OnInit, OnDestroy {

  @Input() isResults = true;

  isBrowser: boolean;

  // solrData: SolrResponse;
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
    layers: [
      L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 20,
        attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> '
      })
    ],
    zoom: 4,
    zoomControl: false,
    zoomSnap: 0,
    center: L.latLng(49.803, 15.496)
  };

  icon = L.icon({
    iconSize: [25, 41],
    iconAnchor: [13, 41],
    iconUrl: 'assets/marker-icon.png',
    shadowUrl: 'assets/marker-shadow.png'
  });

  hitIcon = L.icon({
    iconSize: [25, 41],
    iconAnchor: [13, 41],
    iconUrl: 'assets/img/marker-icon-hit.png',
    shadowUrl: 'assets/marker-shadow.png'
  });

  params: HttpParams;

  maxNumMarkers = 50;
  markerZoomLevel = 16;

  map;
  markers;
  markersList: any[] = [];
  selectedMarker = [];

  showHeat: boolean;
  mapReady = false;
  subs: any[] = [];

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

    this.subs.push(this.state.mapResultChanged.subscribe((res: any) => {
      if (this.mapReady) {
        this.hitMarker(res);
      }
    }));
    this.options.zoom = this.config.mapOptions.zoom;
    this.options.center = L.latLng(this.config.mapOptions.centerX, this.config.mapOptions.centerY);

    L.control.zoom(this.zoomOptions);

    this.params = this.route.snapshot.queryParams as HttpParams;
    // this.subs.push(this.route.queryParams.subscribe(val => {
    //   if (this.mapReady) {
    //     this.setData();
    //   }
    // }));

    this.subs.push(this.state.stateChanged.subscribe(res => {
      if (this.mapReady) {
        this.setData();
      }
    }));

    this.subs.push(this.state.mapViewChanged.subscribe(res => {
      if (this.mapReady) {
        setTimeout(() => {
          this.map.invalidateSize({pan: false});
        }, 500);
      }
    }));
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
      if (this.heatmapLayer) {
        this.map.removeLayer(this.heatmapLayer);
      }
      this.setMarkersData();
      const markersToShow = Math.min(this.state.solrResponse.response.numFound, this.markersList.length);
      this.showHeat = this.state.solrResponse.response.numFound > this.maxNumMarkers && this.map.getZoom() < this.markerZoomLevel;
      if (this.showHeat) {
        this.setHeatData();
      }
      if (this.state.locationFilterEnabled) {
        this.locationFilter.enable();
      } else {
        this.locationFilter.disable();
      }
      this.hitMarker(this.state.mapResult);
    }
  }

  setMarkersData() {
    this.markersList = [];
    this.markers = new L.featureGroup();
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
              mrk.docId = [doc.ident_cely];
              mrk.on('click', (e) => {
                this.setPianId(e.target.pianId);
              });
      
              // mrk.bindTooltip(this.popUpHtml(pianId, presnost)).addTo(this.markers);
              mrk.bindTooltip(this.popUpHtml(pianId, presnost)).openTooltip();

              mrk.addTo(this.markers);
            } else {
              mrk.docId.push(doc.ident_cely);
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
        iconUrl: 'assets/marker-icon.png'
      }));
      m.setZIndexOffset(0);
    });
    this.selectedMarker = [];
  }

  hitMarker(res) {
    if (!res) {
      this.clearSelectedMarker();
      return;
    }
    const docId = res.ident_cely;
    let changed = true;
    if ((this.selectedMarker.length > 0 && this.selectedMarker[0].docId.includes(docId)) || !this.isResults ) {
      // the same or document
      changed = false;
    }
    const ms = this.markersList.filter(mrk => mrk.docId.includes(docId));
    this.clearSelectedMarker();
    ms.forEach(m => {
      // m.fire('click');
      m.setIcon(this.hitIcon);
      m.setZIndexOffset(100);
      if (changed) {
        this.map.setView(m.getLatLng());
      }
    });
    this.selectedMarker = ms;
  }

  markerExists(pianId: string) {
    return this.markersList.find(m => m.pianId === pianId);
  }

  setPianFilter(pianId: string) {
    this.zone.run(() => {
      this.router.navigate([], { queryParams: { pian_ident_cely: pianId, page: 0 }, queryParamsHandling: 'merge' });
    });
  }

  popUpHtml(id: string, presnost: string) {
    return id + ' (' + this.service.getHeslarTranslation(presnost, 'presnost')  + ')';
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

    this.markers = new L.featureGroup();
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
      // this.locationFilter.enable();
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
      // this.locationFilter.enable();
      this.map.fitBounds(bounds.pad(.03));
      this.locationFilter.setBounds(this.map.getBounds().pad(-0.95));

    } else {
      this.locationFilter.setBounds(bounds.pad(-0.95));
    }
    
    map.on('zoomend', () => {
      // this.heatmapLayer.setOptions(this.config.mapOptions.heatmapOptions);
      this.updateBounds(map.getBounds());
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

    this.setData();
    this.mapReady = true;
  }

  onResize(mapBounds) {
    this.updateBounds(mapBounds);
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
    if (!this.state.solrResponse.facet_counts.facet_heatmaps?.loc_rpt) {
      return;
    }
    this.data.data = [];
    const f = this.state.solrResponse.facet_counts.facet_heatmaps.loc_rpt;
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


    // this.data = heat;
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

}
