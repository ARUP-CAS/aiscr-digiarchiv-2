
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
  @Input() showResults = false;

  isBrowser: boolean;
  data = {
    data: []
  };

  heatmapLayer;

  locationFilter;

  zoomOptions = {
    zoomInTitle: this.service.getTranslation('map.desc.zoom in'),
    zoomOutTitle: this.service.getTranslation('map.desc.zoom out'),
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

  icon = L.icon({
    iconSize: [12, 20],
    iconAnchor: [6, 20],
    iconUrl: 'assets/img/pin.png',
    //shadowUrl: 'assets/img/marker-shadow.png'
  });

  iconPoint = L.icon({
    iconSize: [16, 26],
    iconAnchor: [8, 26],
    iconUrl: 'assets/img/pin-point.png',
    //shadowUrl: 'assets/img/marker-shadow.png'
  });

  hitIcon = L.icon({
    iconSize: [12, 20],
    iconAnchor: [6, 20],
    iconUrl: 'assets/img/pin-hit.png',
    //shadowUrl: 'assets/img/marker-shadow.png'
  });

  hitIconPoint = L.icon({
    iconSize: [16, 26],
    iconAnchor: [8, 26],
    iconUrl: 'assets/img/pin-hit-point.png',
    //shadowUrl: 'assets/img/marker-shadow.png'
  });

  params: HttpParams;

  maxNumMarkers = 0;
  markerZoomLevel = 16;
  currentZoom: number;
  zoomingOnMarker = false;
  showType = 'marker'; // 'heat', 'cluster', 'marker'

  map;
  // markers = new L.featureGroup();
  markers = new L.markerClusterGroup();

  piansList: string[] = [];
  markersList: any[] = [];
  selectedMarker = [];

  showHeat: boolean;
  mapReady = false;
  subs: any[] = [];
  isInit = true;
  firstChange = true;
  firstZoom = true;

  layersControl = { baseLayers: {}, overlays: {} };

  osm = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: this.config.mapOptions.maxZoom,
    maxNativeZoom: 19,
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> '
  });

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
  showDetail = false;
  currentLocBounds: any;

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
    this.initLayers();
    this.showDetail = false;
    this.maxNumMarkers = this.config.mapOptions.docsForMarker;


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
        this.setData(res.pageChanged);
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

    this.subs.push(this.service.currentLang.subscribe(res => {
      if (this.mapReady) {
        setTimeout(() => {
          this.initLayers();
          this.map.invalidateSize({ pan: false });
        }, 500);
      }
    }));

    if (!this.isResults && this.mapReady) {
      this.setData(false);
    }
  }

  initLayers() {
    this.baseLayers = {};
    this.baseLayers[this.service.getTranslation('map.layer.cuzk_zakladni')] = this.cuzkZM,
      this.baseLayers[this.service.getTranslation('map.layer.cuzk_orto')] = this.cuzkOrt,
      this.baseLayers[this.service.getTranslation('map.layer.cuzk_stin')] = this.cuzkEL,
      this.baseLayers[this.service.getTranslation('map.layer.openstreet')] = this.osm;
    this.overlays = {};
    this.overlays[this.service.getTranslation('map.layer.cuzk_katastr_mapa')] = this.cuzkWMS;
    this.overlays[this.service.getTranslation('map.layer.cuzk_katastr_uzemi')] = this.cuzkWMS2;

    this.options.layers = [this.osm];

    this.layersControl = {
      baseLayers: this.baseLayers,
      overlays: this.overlays
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

  setData(pageChanged: boolean) {
    if (this.state.solrResponse) {
      const byLoc = this.state.entity === 'knihovna_3d' || this.state.entity === 'samostatny_nalez';
      if (this.state.solrResponse.response.numFound > this.config.mapOptions.docsForCluster) {
        this.showType = 'heat';
      } else if (this.state.solrResponse.response.numFound > this.maxNumMarkers) {
        this.showType = 'cluster';
        if (pageChanged) {
          this.state.loading = false;
          return;
        }
      } else {
        this.showType = 'marker';
      }

      this.markers = new L.markerClusterGroup();
      this.markersList = [];
      this.piansList = [];
      switch (this.showType) {
        case 'cluster': {
          this.state.loading = true;
          const p: any = Object.assign({}, this.route.snapshot.queryParams);
          p.rows = this.state.solrResponse.response.numFound;
          this.service.getPians(p as HttpParams).subscribe((res: any) => {
            if (byLoc) {
              this.setClusterDataByLoc(res.response.docs);
            } else {
              this.setClusterDataByPian(res.response.docs);
            }
            this.state.loading = false;
            setTimeout(() => {
              if (this.state.mapResult) {
                this.hitMarker(this.state.mapResult);
              }

            }, 100);
          });
          break;
        }
        case 'marker': {

          if (this.state.mapResult) {
            this.markers = new L.featureGroup();
            if (this.state.mapResult.pian_id && this.state.mapResult.pian_id.length > 0) {
              this.state.mapResult.pian = [];
              this.state.mapResult.pian_id.forEach(pian_id => {
                if (!this.piansList.includes(pian_id)) {
                  this.piansList.push(pian_id);
                  this.service.getId(pian_id, false).subscribe(resp => {
                    const pian = resp.response.docs[0];
                    if (!this.state.mapResult.pian) {
                      this.state.mapResult.pian = [];
                    }
                    this.state.mapResult.pian.push(pian);
                    const coords = pian.loc_rpt[0].split(',');
                    const mrk = this.addMarker(pian.ident_cely, true, coords[0], coords[1], pian.pian_presnost, pian.typ, this.state.mapResult);

                    mrk.addTo(this.markers);
                    if (!byLoc) {
                      this.addShape(mrk.pianId, mrk.pianPresnost, mrk.docId.length);
                    }
                    this.hitMarker(this.state.mapResult);

                    setTimeout(() => {
                      this.setMarkersData(false);
                    }, 10);
                  });
                }
              });
            } else {
              this.setMarkersData(true);
              this.hitMarker(this.state.mapResult);
            }
          } else {
            this.setMarkersData(true);
            if (!byLoc) {
              this.markersList.forEach(m => {
                if (m.pianPresnost !== 'HES-000864' && m.pianTyp !== 'bod') {
                  //if (m.pianPresnost < 4 && m.pianTyp !== 'bod') {
                  this.addShape(m.pianId, m.pianPresnost, m.docId.length);
                }
              });
            }
          }




          // this.setMarkersData(true);
          // setTimeout(() => {
          //   if (this.state.mapResult) {
          //     this.hitMarker(this.state.mapResult);
          //   } else {
          //     // this.fitOnMarkers();
          //   }

          // }, 100);
          break;
        }
        case 'heat': {
          // this.setMarkersData(true);
          this.state.loading = false;
          setTimeout(() => {
            if (this.state.mapResult) {
              this.hitMarker(this.state.mapResult);
            }
          }, 100);
          break;
        }
      }

      if (this.state.locationFilterEnabled) {
        this.locationFilter.enable();
        this.locationFilter.setBounds(this.state.locationFilterBounds);

      } else {
        this.locationFilter.disable();
      }

      if ((this.showDetail || this.isInit) && this.state.solrResponse.response.docs.length === 1) {
        this.state.setMapResult(this.state.solrResponse.response.docs[0], false);
      }
      this.isInit = false;
      this.showDetail = false;
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
      console.log(bounds)
      this.map.fitBounds(bounds.pad(.03));

    }
  }

  addMarkerByResult(doc: any) {
    if (doc.pian && doc.pian.length > 0) {
      doc.pian.forEach(pian => {
        const coords = pian.loc_rpt[0].split(',');
        this.addMarker(pian.ident_cely, true, coords[0], coords[1], pian.pian_presnost, pian.typ, doc);
      });
    } else if (doc.pian_id && doc.pian_id.length > 0) {
      doc.pian_id.forEach(pian_id => {
        if (!this.piansList.includes(pian_id)) {
          this.piansList.push(pian_id);
          this.service.getId(pian_id, false).subscribe(resp => {
            const pian = resp.response.docs[0];
            doc.pian = pian;
            const coords = pian.loc_rpt[0].split(',');
            const mrk = this.addMarker(pian.ident_cely, true, coords[0], coords[1], pian.pian_presnost, pian.typ, doc);
            mrk.addTo(this.markers);
            //this.addShape(mrk.pianId, mrk.pianPresnost, mrk.docId.length);
          });
        }
      });
    } else if (doc.loc_rpt) {
      const coords = doc.loc_rpt[0].split(',');
      this.addMarker(doc.ident_cely, false, coords[0], coords[1], '', '', doc);
    }
  }

  addMarker(id: string, isPian: boolean, lat: string, lng: string, presnost: string, typ: string, doc: any) {
    let mrk = this.markerExists(id);
    if (!mrk) {
      mrk = L.marker([lat, lng], { pianId: id, icon: typ === 'bod' ? this.iconPoint : this.icon, docId: [], doc, riseOnHover: true });
      this.markersList.push(mrk);
      mrk.pianId = id;
      mrk.pianPresnost = presnost;
      mrk.pianTyp = typ;
      mrk.docId = [doc.ident_cely];
      mrk.doc = doc;
      if (isPian) {
        mrk.on('click', (e) => {
          this.setPianId(e.target.pianId);
        });
        mrk.bindTooltip(this.popUpHtml(id, presnost, mrk.docId));
      } else {
        mrk.on('click', (e) => {
          this.setMarker(e.target.doc);
        });
        mrk.bindTooltip(doc.ident_cely);
      }
      // mrk.addTo(this.markers);
    } else if (isPian) {
      mrk.docId.push(doc.ident_cely);
      mrk.bindTooltip(this.popUpHtml(id, presnost, mrk.docId));
    }
    return mrk;
  }

  setClusterDataByPian(docs: any[]) {
    this.markers = new L.markerClusterGroup();
    docs.forEach(pian => {
      //if (doc.pian && doc.pian.length > 0) {
      //  doc.pian.forEach(pian => {
      if (this.state.hasRights(pian.pristupnost, pian.organizace)) {
        const coords = pian.loc_rpt[0].split(',');
        this.addMarker(pian.ident_cely, true, coords[0], coords[1], pian.presnost, pian.typ, pian);
      }
      //  });
      //}
    });
    this.markers.addLayers(this.markersList);
    this.currentZoom = this.map.getZoom();
  }

  setClusterDataByLoc(docs: any[]) {
    this.markers = new L.markerClusterGroup();
    this.markersList = [];
    this.piansList = [];
    docs.forEach(doc => {
      if (doc.loc_rpt) {
        if (this.state.hasRights(doc.pristupnost, doc.organizace)) {
          const coords = doc.loc_rpt[0].split(',');
          this.addMarker(doc.ident_cely, false, coords[0], coords[1], '', '', doc);
        }
      }
    });
    this.markers.addLayers(this.markersList);
    this.currentZoom = this.map.getZoom();
  }



  setMarkersData(clean: boolean) {
    if (clean) {
      this.markersList = [];
      this.piansList = [];
      this.markers = new L.featureGroup();
    }
    //this.markers = new L.markerClusterGroup();
    this.state.solrResponse.response.docs.forEach(doc => {
      if (doc.pian && doc.pian.length > 0) {
        doc.pian.forEach(pian => {
          if (this.state.hasRights(pian.pristupnost, doc.organizace)) {
            const coords = pian.loc_rpt[0].split(',');
            this.addMarker(pian.ident_cely, true, coords[0], coords[1], pian.pian_presnost, pian.typ, doc);
          }
        });
        if (this.showType !== 'heat') {
          this.markersList.forEach(mrk => {
            mrk.addTo(this.markers);
          });
        }
      } else if (doc.pian_id && doc.pian_id.length > 0) {
        doc.pian = [];
        doc.pian_id.forEach(pian_id => {

          if (!this.piansList.includes(pian_id)) {
            this.piansList.push(pian_id);
            if (this.showType !== 'heat') {
              this.service.getId(pian_id, false).subscribe(resp => {
                const pian = resp.response.docs[0];
                if (pian) {
                  doc.pian.push(pian);
                  const coords = pian.loc_rpt[0].split(',');
                  const mrk = this.addMarker(pian.ident_cely, true, coords[0], coords[1], pian.pian_presnost, pian.typ, doc);
                  mrk.addTo(this.markers);
                  this.addShape(mrk.pianId, mrk.pianPresnost, mrk.docId.length);
                }
              });
            }
          }

        });
      } else if (doc.loc_rpt) {
        if (this.state.hasRights(doc.pristupnost, doc.organizace) || doc.entity === 'dokument') {
          const coords = doc.loc_rpt[0].split(',');
          this.addMarker(doc.ident_cely, false, coords[0], coords[1], '', '', doc);
        }
        if (this.showType !== 'heat') {
          this.markersList.forEach(mrk => {
            mrk.addTo(this.markers);
          });
        }
      }

    });
    this.state.loading = false;
    this.currentZoom = this.map.getZoom();
  }

  setMarker(res) {
    this.zone.run(() => {
      this.state.setMapResult(res, false);
    });
  }

  setPianId(pian_id: string) {
    this.zone.run(() => {
      this.showDetail = true;
      this.router.navigate([], { queryParams: { pian_id, page: 0 }, queryParamsHandling: 'merge' });
    });
  }

  clearPian() {
    this.router.navigate([], { queryParams: { pian_id: null, page: 0 }, queryParamsHandling: 'merge' });
  }

  clearSelectedMarker() {

    this.selectedMarker.forEach(m => {
      m.setIcon(m.pianTyp === 'bod' ? this.iconPoint : this.icon);
      m.setZIndexOffset(0);
    });
    this.selectedMarker = [];
  }

  hitMarker(res) {
    if (!res) {
      this.clearSelectedMarker();
      this.updateBounds(this.map.getBounds(), false, 'hitMarker');
      return;
    }
    const docId = res.ident_cely;
    let changed = true;
    if ((this.selectedMarker.length > 0 && this.selectedMarker[0].docId.includes(docId))) {
      // the same or document
      changed = false;
    }
    let ms = this.markersList.filter(mrk => mrk.docId.includes(docId));
    if (changed) {
      this.clearSelectedMarker();
    }

    if (!ms || ms.length === 0) {
      this.addMarkerByResult(res);

      if (this.showType !== 'heat') {
        this.markersList.forEach(mrk => {
          mrk.addTo(this.markers);
        });
      }


      ms = this.markersList.filter(mrk => mrk.docId.includes(docId));
    }

    ms.forEach(m => {
      m.setIcon(m.pianTyp === 'bod' ? this.hitIconPoint : this.hitIcon);
      m.setZIndexOffset(100);
      if (this.showType === 'heat') {
        m.addTo(this.markers);
        if (m.pianPresnost < 4 && m.pianTyp !== 'bod') {
          this.addShape(m.pianId, m.pianPresnost, m.docId.length);
        }
      }
    });

    if (changed && ms.length > 0) {
      this.zoomingOnMarker = true;
      if (this.map.getZoom() < this.config.mapOptions.hitZoomLevel) {
        this.map.setView(ms[ms.length - 1].getLatLng(), this.config.mapOptions.hitZoomLevel);
      } else {
        this.map.setView(ms[ms.length - 1].getLatLng());
      }

    }
    if (!ms || ms.length === 0) {
      //this.state.mapResult = null;
    }
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
        enableText: this.service.getTranslation('map.desc.select area'),
        disableText: this.service.getTranslation('map.desc.remove selection')
      }
    });

    map.addLayer(this.locationFilter);

    //this.markers = new L.featureGroup();
    this.markers = new L.markerClusterGroup();
    map.addLayer(this.markers);

    map.on('enterFullscreen', () => map.invalidateSize());
    map.on('exitFullscreen', () => map.invalidateSize());
    let bounds = map.getBounds();
    if (this.route.snapshot.queryParamMap.has('vyber')) {
      const loc_rpt = this.route.snapshot.queryParamMap.get('vyber').split(',');
      const southWest = L.latLng(loc_rpt[0], loc_rpt[1]);
      const northEast = L.latLng(loc_rpt[2], loc_rpt[3]);
      bounds = L.latLngBounds(southWest, northEast);
      // if (this.state.locationFilterEnabled) {
      //   this.locationFilter.setBounds(bounds);
      // }

      //this.map.fitBounds(bounds);
      this.map.fitBounds(bounds.pad(.3));
    } else if (this.route.snapshot.queryParamMap.has('loc_rpt')) {
      const loc_rpt = this.route.snapshot.queryParamMap.get('loc_rpt').split(',');
      const southWest = L.latLng(loc_rpt[0], loc_rpt[1]);
      const northEast = L.latLng(loc_rpt[2], loc_rpt[3]);
      bounds = L.latLngBounds(southWest, northEast);

      this.map.fitBounds(bounds);
      this.updateBounds(this.map.getBounds(), false, 'init');
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
      this.map.fitBounds(bounds.pad(.5));
      //this.map.fitBounds(bounds);
      if (this.state.locationFilterEnabled) {
        this.locationFilter.setBounds(this.map.getBounds().pad(this.config.mapOptions.selectionInitPad));
      }
    } else {
      if (this.state.locationFilterEnabled) {
        this.locationFilter.setBounds(bounds.pad(this.config.mapOptions.selectionInitPad));
      }
    }

    map.on('zoomend', (e) => {
      if (!this.zoomingOnMarker && !this.firstZoom) {
        this.debounce(this.updateBounds(map.getBounds(), false, 'mapZoomEnd'), 200, false);
      }
      this.firstZoom = false;
      this.zoomingOnMarker = false;
    });
    map.on('dragend', () => {
      this.updateBounds(map.getBounds(), false, 'mapDragEnd');
    });
    map.on('fullscreenchange', () => {
      this.updateBounds(map.getBounds(), false, 'mapFull');
    });
    map.on('resize', () => {
      this.updateBounds(map.getBounds(), false, 'mapResize');
    });

    this.locationFilter.on('change', (e) => {
      if (JSON.stringify(this.state.locationFilterBounds) !== JSON.stringify(this.locationFilter.getBounds()) && !this.firstChange) {
        this.updateBounds(null, true, 'locChange');
      }
      this.firstChange = false;

    });

    this.locationFilter.on('enabled', () => {
      if (!this.state.locationFilterEnabled) {
        this.state.locationFilterEnabled = true;
        this.locationFilter.setBounds(this.map.getBounds().pad(this.config.mapOptions.selectionInitPad));
        // this.state.locationFilterBounds = this.map.getBounds().pad(this.config.mapOptions.selectionInitPad);
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

    if (!this.isResults) {
      this.setData(false);
    }
    this.setHeatData();
    this.mapReady = true;
  }

  debounce(func, wait, immediate) {
    var timeout;
    return () => {
      var context = this, args = arguments;
      clearTimeout(timeout);
      timeout = setTimeout(function () {
        timeout = null;
        if (!immediate) func.apply(context, args);
      }, wait);
      if (immediate && !timeout) func.apply(context, args);
    };
  }

  updateBounds(mapBounds: any, isLocation: boolean, caller: string) {
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

  setHeatData() {
    if (this.heatmapLayer) {
      this.map.removeLayer(this.heatmapLayer);
    }
    if (!this.state.heatMaps?.loc_rpt) {
      return;
    }
    const markersToShow = Math.min(this.state.solrResponse.response.numFound, this.markersList.length);

    this.showHeat = this.state.solrResponse.response.numFound > this.config.mapOptions.docsForCluster
      && this.map.getZoom() < this.markerZoomLevel;
    // this.showHeat = false;
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
    this.state.loading = false;
  }

  addShapes() {
    this.state.solrResponse.response.docs.forEach(doc => {
      if (doc.pian && doc.pian.length > 0) {
        doc.pian.forEach((pian: any) => {

        });
      }
    });
  }

  addShape(ident_cely: string, presnost: string, pocet: number) {
    this.service.getWKT(ident_cely).subscribe((resp: any) => {
      if (!resp.geom_wkt_c) {
        return;
      }
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
          this.setPianId(ident_cely);
        });
        layer.bindTooltip(this.popUpHtml(ident_cely, presnost, pocet));
        // layer.addTo(this.overlays);
        layer.addTo(this.markers);
      }
    });

  }
}

