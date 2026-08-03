import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { AppComponent } from './app/app.component';
bootstrapApplication(AppComponent,{providers:[provideHttpClient(withInterceptors([(request,next)=>{const token=localStorage.getItem('token');return next(token?request.clone({setHeaders:{Authorization:`Bearer ${token}`}}):request);}] ))]}).catch(error=>{console.error(error);const host=document.querySelector('app-root');if(host)host.innerHTML='<div class="loading"><div><b>PulseTickets could not start</b><small>Open the browser console for the error details, then refresh.</small></div></div>';});
