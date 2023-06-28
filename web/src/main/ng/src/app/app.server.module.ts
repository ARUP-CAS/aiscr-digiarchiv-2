import { NgModule } from '@angular/core';
import { ServerModule } from '@angular/platform-server';

import { AppSsrModule } from './app.ssr.module';
import { AppComponent } from './app.component';
import {FlexLayoutServerModule} from 'ngx-flexible-layout/server';
@NgModule({
  imports: [
    AppSsrModule,
    FlexLayoutServerModule,
    ServerModule,
  ],
  bootstrap: [AppComponent],
})
export class AppServerModule {}
