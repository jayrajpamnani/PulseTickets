import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { AdminDashboardComponent } from './admin-dashboard.component';

type Event = { id: number; title: string; venue: string; startsAt: string; price: number; availableTickets: number; bannerUrl?: string; description?: string };

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminDashboardComponent],
  template: `
    <header><b>PulseTickets</b><span>Discover memorable events</span>
      <button (click)="openAuth()">{{ token() ? 'Account' : 'Sign in' }}</button>
      <button *ngIf="token()" class="outline" (click)="signOut()">Sign out</button>
    </header>
    <main>
      <section class="hero"><p>LIVE EXPERIENCES</p><h1>Find your next<br>great night out.</h1>
        <div class="search"><input [(ngModel)]="keyword" (keyup.enter)="load()" placeholder="Search events, venues, cities"><button (click)="load()">Search</button></div>
      </section>
      <section><div class="section-heading"><div><p class="eyebrow">CURATED FOR YOU</p><h2>Upcoming events</h2></div><span *ngIf="events().length">{{ events().length }} events</span></div>
        <p class="error" *ngIf="error()">{{ error() }}</p>
        <div class="grid"><article *ngFor="let e of events()">
          <div class="poster" [style.background-image]="e.bannerUrl ? 'url(' + e.bannerUrl + ')' : null"><span *ngIf="!e.bannerUrl">{{ e.title.slice(0,1) }}</span></div>
          <small>{{ e.startsAt | date:'EEE, MMM d • h:mm a' }}</small><h3>{{ e.title }}</h3><p>{{ e.venue }}</p>
          <p class="description" *ngIf="e.description">{{ e.description }}</p>
          <footer><div><b>{{ e.price | currency }}</b><small> · {{ e.availableTickets }} left</small></div><button (click)="reserve(e)" [disabled]="!e.availableTickets">Reserve</button></footer>
        </article></div>
        <p *ngIf="!events().length && !error()">No events match your search yet.</p>
      </section>
      <app-admin-dashboard *ngIf="isAdmin()" (eventPublished)="load()"></app-admin-dashboard>
    </main>

    <div class="modal-backdrop" *ngIf="authOpen()" (click)="closeAuth()"><section class="auth-modal" (click)="$event.stopPropagation()">
      <button class="close" (click)="closeAuth()">×</button><p class="eyebrow">YOUR PULSE TICKETS ACCOUNT</p><h2>{{ authMode === 'login' ? 'Welcome back' : 'Create your account' }}</h2>
      <div class="tabs"><button [class.active]="authMode === 'login'" (click)="authMode='login'">Sign in</button><button [class.active]="authMode === 'register'" (click)="authMode='register'">Register</button></div>
      <label>Username<input [(ngModel)]="authUsername" autocomplete="username" placeholder="e.g. alex"></label>
      <label *ngIf="authMode === 'register'">Email<input [(ngModel)]="authEmail" type="email" autocomplete="email" placeholder="you@example.com"></label>
      <label>Password<input [(ngModel)]="authPassword" type="password" autocomplete="current-password" placeholder="At least 8 characters"></label>
      <label class="admin-option"><input type="checkbox" [(ngModel)]="adminMode"> Sign in as an administrator</label>
      <p class="hint">{{ adminMode ? 'Use the administrator credentials configured for this installation.' : 'Create an account to reserve tickets.' }}</p>
      <p class="error" *ngIf="authError()">{{ authError() }}</p><button class="full" (click)="submitAuth()" [disabled]="authBusy()">{{ authBusy() ? 'Please wait…' : (authMode === 'login' ? 'Sign in' : 'Create account') }}</button>
    </section></div>
  `,
  styles: [`
    header{height:64px;display:flex;align-items:center;gap:12px;padding:0 7%;border-bottom:1px solid #e8e8ef}header b{font-size:22px;color:#5319c9}header span{color:#666;flex:1}button{background:#5c20d4;color:#fff;border:0;border-radius:7px;padding:10px 15px;font-weight:600;cursor:pointer}button:disabled{opacity:.55;cursor:not-allowed}.outline{background:white;color:#5c20d4;border:1px solid #c9bce9}.hero{background:linear-gradient(115deg,#230462,#832be0);color:white;padding:70px 7%;margin:0 -7% 40px}.hero p,.eyebrow{letter-spacing:2px;font-size:12px}.hero h1{font-size:52px;line-height:1.05;margin:12px 0 28px}.search{display:flex;max-width:560px}.search input{flex:1;padding:14px;border:0;border-radius:7px 0 0 7px;font:inherit}.search button{border-radius:0 7px 7px 0;background:#f64c72}main{padding:0 7% 50px}.section-heading{display:flex;align-items:end;justify-content:space-between}.section-heading h2{margin-top:0}.section-heading span{color:#777}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:22px}article{border:1px solid #e5e3ea;border-radius:10px;padding:15px;box-shadow:0 3px 12px #25104d12;overflow:hidden}.poster{height:145px;background:linear-gradient(45deg,#f64c72,#5c20d4);background-size:cover;background-position:center;display:grid;place-items:center;color:#fff;font-size:56px;border-radius:6px;margin-bottom:15px}.poster span{filter:drop-shadow(0 2px 5px #0006)}article h3{margin:5px 0}article p,small{color:#6b6872}.description{font-size:13px;min-height:38px}article footer{display:flex;align-items:center;justify-content:space-between;margin-top:15px}.error{color:#b00020}.modal-backdrop{position:fixed;inset:0;background:#17052f99;display:grid;place-items:center;padding:20px;z-index:3}.auth-modal{position:relative;background:white;border-radius:14px;padding:30px;width:min(430px,100%);box-shadow:0 20px 70px #0004}.auth-modal h2{margin:4px 0 18px}.close{position:absolute;right:14px;top:12px;background:transparent;color:#555;font-size:25px;padding:3px 9px}.tabs{display:flex;border-bottom:1px solid #ddd;margin-bottom:18px}.tabs button{background:transparent;color:#777;border-radius:0;border-bottom:2px solid transparent;flex:1}.tabs button.active{color:#5c20d4;border-bottom-color:#5c20d4}label{display:grid;gap:6px;margin:13px 0;font-weight:600}label input:not([type=checkbox]){padding:11px;border:1px solid #c9c3d8;border-radius:6px;font:inherit}.admin-option{display:flex;align-items:center;gap:8px;font-weight:500}.admin-option input{width:auto}.hint{font-size:13px;color:#777}.full{width:100%;margin-top:5px}
  `]
})
export class AppComponent {
  private http = inject(HttpClient);
  events = signal<Event[]>([]); error = signal(''); token = signal(localStorage.getItem('token')); keyword = '';
  authOpen = signal(false); authError = signal(''); authBusy = signal(false); authMode: 'login'|'register' = 'login'; adminMode = false; authUsername = ''; authEmail = ''; authPassword = '';
  constructor() { this.load(); }
  isAdmin() { return this.role() === 'ADMIN'; }
  load() { this.http.get<any>('/api/events', { params: { keyword: this.keyword.trim() } }).subscribe({ next: r => { this.events.set(r.content ?? r); this.error.set(''); }, error: () => this.error.set('We could not load events right now. Please try again.') }); }
  openAuth() { this.authError.set(''); this.authOpen.set(true); }
  closeAuth() { this.authOpen.set(false); }
  signOut() { localStorage.removeItem('token'); this.token.set(null); this.load(); }
  submitAuth() {
    this.authError.set(''); this.authBusy.set(true);
    const request = this.authMode === 'login' ? this.http.post<{token:string}>('/api/auth/login', { username: this.authUsername, password: this.authPassword }) : this.http.post<{token:string}>('/api/auth/register', { username: this.authUsername, email: this.authEmail, password: this.authPassword });
    request.subscribe({ next: r => { localStorage.setItem('token', r.token); this.token.set(r.token); if (this.adminMode && !this.isAdmin()) { this.authError.set('This account does not have administrator access.'); this.signOut(); this.authBusy.set(false); return; } this.authBusy.set(false); this.closeAuth(); }, error: (e: HttpErrorResponse) => { this.authBusy.set(false); this.authError.set(e.status === 409 ? 'That username or email is already registered.' : e.status === 401 ? 'Incorrect username or password.' : 'Please check your details and try again.'); } });
  }
  reserve(event: Event) { if (!this.token()) { this.authError.set(''); this.authMode = 'login'; this.openAuth(); return; } this.http.post('/api/reservations', {}, { params: { eventId: event.id, quantity: 1 } }).subscribe({ next: () => { this.error.set('Reservation confirmed!'); this.load(); }, error: (e: HttpErrorResponse) => { if (e.status === 401) { this.signOut(); this.openAuth(); } else this.error.set(e.error?.message || 'Reservation could not be created. Please try again.'); } }); }
  private role() { try { return JSON.parse(atob((localStorage.getItem('token') || '').split('.')[1] || '')).role || ''; } catch { return ''; } }
}
