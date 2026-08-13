import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { HeroPortrait, HistoryPoint, PlayerProfile, RecentProfile } from './models';

@Injectable({providedIn:'root'})
export class PlayerService {
  readonly current=signal<PlayerProfile|null>(null);
  constructor(private readonly http:HttpClient){}
  lookup(battleTag:string){return this.http.post<PlayerProfile>('/api/players/lookup',{battleTag}).pipe(tap(p=>this.current.set(p)));}
  recent(){return this.http.get<RecentProfile[]>('/api/players/recent');}
  heroCatalog(){return this.http.get<HeroPortrait[]>('/api/heroes');}
  openStored(battleTag:string){return this.http.get<PlayerProfile>(`/api/players/${encodeURIComponent(battleTag.replace('#','-'))}/stored`).pipe(tap(p=>this.current.set(p)));}
  refresh(){const p=this.current();if(!p)throw new Error('Aucun profil');return this.http.post<PlayerProfile>(`/api/players/${encodeURIComponent(p.battleTag.replace('#','-'))}/refresh`,{}).pipe(tap(x=>this.current.set(x)));}
  history(){const p=this.current();if(!p)throw new Error('Aucun profil');return this.http.get<HistoryPoint[]>(`/api/players/${encodeURIComponent(p.battleTag.replace('#','-'))}/history`);}
}
