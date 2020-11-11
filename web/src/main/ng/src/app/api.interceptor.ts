import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AppConfiguration } from './app-configuration';
import { isPlatformBrowser } from '@angular/common';


@Injectable()
export class ApiInterceptor implements HttpInterceptor {
    constructor(
        @Inject(PLATFORM_ID) private platformId: any,
        private config: AppConfiguration) { }

    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        if (request.url.startsWith('/api') && isPlatformBrowser(this.platformId)) {
            request = request.clone({ url: `${this.config.context}${request.url}`, withCredentials: true });
        }
        return next.handle(request);
    }
}
