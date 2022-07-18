import { NgModule } from '@angular/core';
import { CommonModule, registerLocaleData } from '@angular/common';
import { LeafletModule } from '@asymmetrik/ngx-leaflet';
import { HttpClient } from '@angular/common/http';
import { TranslateModule, TranslateLoader } from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';

import { AppModule } from './app.module';
// import { AppMaterialModule } from 'src/app/app-material.module';
// import { MapaComponent } from 'src/app/components/mapa/mapa.component';
// import { environment } from 'src/environments/environment';
// import localeCs from '@angular/common/locales/cs';
import { AppComponent } from './app.component';


@NgModule({
  declarations: [
    // MapaComponent
  ],
  imports: [
    CommonModule,
    AppModule,
    LeafletModule,
    
  ],
  bootstrap: [AppComponent]
})
export class AppBrowserModule { }
