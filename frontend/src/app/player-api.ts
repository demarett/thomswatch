import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { HeroPortrait, HistoryPoint, PlayerProfile, RecentProfile } from './models';
import { toRouteBattleTag } from './player-route';

@Injectable({providedIn: 'root'})
export class PlayerApi {
  constructor(private readonly http: HttpClient) {}

  lookup(battleTag: string) {
    return this.http.post<PlayerProfile>('/api/players/lookup', {battleTag});
  }

  recent() {
    return this.http.get<RecentProfile[]>('/api/players/recent');
  }

  stored(battleTag: string) {
    return this.http.get<PlayerProfile>(`/api/players/${encodeURIComponent(toRouteBattleTag(battleTag))}/stored`);
  }

  refresh(battleTag: string) {
    return this.http.post<PlayerProfile>(`/api/players/${encodeURIComponent(toRouteBattleTag(battleTag))}/refresh`, {});
  }

  history(battleTag: string) {
    return this.http.get<HistoryPoint[]>(`/api/players/${encodeURIComponent(toRouteBattleTag(battleTag))}/history`);
  }

  heroes() {
    return this.http.get<HeroPortrait[]>('/api/heroes');
  }
}
