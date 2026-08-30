import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, withHashLocation } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { environment } from './environments/environment';

const routerFeatures = environment.useHashRouting ? [withHashLocation()] : [];

bootstrapApplication(AppComponent,{providers:[provideHttpClient(),provideRouter(routes, ...routerFeatures),provideAnimationsAsync()]}).catch(console.error);
