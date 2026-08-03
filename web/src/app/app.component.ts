import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AdminDashboardComponent } from './admin-dashboard.component';

type Event = {id:number;title:string;venue:string;startsAt:string;price:number;availableTickets:number};
@Component({selector:'app-root',standalone:true,imports:[CommonModule,FormsModule,AdminDashboardComponent],template:`<header><b>PulseTickets</b><span>Discover memorable events</span><button (click)="login()" [disabled]="!!token()">{{token()?'Signed in':'Demo login'}}</button></header><main><section class="hero"><p>LIVE EXPERIENCES</p><h1>Find your next<br>great night out.</h1><input [(ngModel)]="keyword" (keyup.enter)="load()" placeholder="Search events, venues, cities"><button (click)="load()">Search</button></section><section><h2>Upcoming events</h2><p class="error" *ngIf="error()">{{error()}}</p><div class="grid"><article *ngFor="let e of events()"><div class="poster">{{e.title.slice(0,1)}}</div><small>{{e.startsAt | date:'EEE, MMM d • h:mm a'}}</small><h3>{{e.title}}</h3><p>{{e.venue}}</p><footer><b>{{e.price | currency}}</b><button (click)="reserve(e)">Reserve</button></footer></article></div><p *ngIf="!events().length && !error()">No events yet. An administrator can add one through the API.</p></section><app-admin-dashboard></app-admin-dashboard></main>`,styles:[`header{height:64px;display:flex;align-items:center;gap:24px;padding:0 7%;border-bottom:1px solid #e8e8ef}header b{font-size:22px;color:#5319c9}header span{color:#666;flex:1}button{background:#5c20d4;color:#fff;border:0;border-radius:7px;padding:10px 15px;font-weight:600;cursor:pointer}.hero{background:linear-gradient(115deg,#230462,#832be0);color:white;padding:70px 7%;margin:0 -7% 40px}.hero p{letter-spacing:2px;font-size:12px}.hero h1{font-size:52px;line-height:1.05;margin:12px 0 28px}.hero input{width:min(430px,65%);padding:14px;border:0;border-radius:7px 0 0 7px}.hero button{border-radius:0 7px 7px 0;background:#f64c72}main{padding:0 7% 50px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:22px}article{border:1px solid #e5e3ea;border-radius:10px;padding:15px;box-shadow:0 3px 12px}.poster{height:125px;background:linear-gradient(45deg,#f64c72,#5c20d4);display:grid;place-items:center;color:#fff;font-size:56px;border-radius:6px;margin-bottom:15px}article h3{margin:5px 0}article p,small{color:#6b6872}article footer{display:flex;align-items:center;justify-content:space-between}.error{color:#b00020}`]})
export class AppComponent {
  private http = inject(HttpClient);
  events = signal<Event[]>([]);
  error = signal('');
  token = signal(localStorage.getItem('token'));
  keyword = '';

  constructor() { this.load(); }

  load() {
    this.http.get<any>(`/api/events?keyword=${encodeURIComponent(this.keyword)}`).subscribe({
      next: response => this.events.set(response.content ?? response),
      error: () => this.error.set('We could not load events right now. Please try again.')
    });
  }

  async login() {
    const credentials = { username: 'demo', email: 'demo@pulsetickets.local', password: 'DemoPass123' };
    try {
      let response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ username: credentials.username, password: credentials.password })
      });
      if (!response.ok) {
        response = await fetch('/api/auth/register', {
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify(credentials)
        });
      }
      if (!response.ok) throw new Error('Demo authentication failed');
      const body = await response.json();
      this.completeSignIn(body.token);
    } catch {
      this.error.set('Demo sign-in is currently unavailable.');
    }
  }

  private completeSignIn(token: string) {
    localStorage.setItem('token', token);
    this.token.set(token);
    this.error.set('');
  }

  reserve(event: Event) {
    if (!this.token()) return this.error.set('Sign in before reserving tickets.');
    this.http.post('/api/reservations', null, { params: { eventId: event.id, quantity: 1 } }).subscribe({
      next: () => alert('Reservation created.'),
      error: () => this.error.set('Reservation could not be created.')
    });
  }
}
