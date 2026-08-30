import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { HeroPortrait, HistoryPoint, PlayerProfile, RankReference, RecentProfile } from './models';
import { toRouteBattleTag } from './player-route';
import { apiUrl } from './runtime-config';

@Injectable({providedIn: 'root'})
export class PlayerApi {
  constructor(private readonly http: HttpClient) {}

  lookup(battleTag: string) {
    return this.http.post<PlayerProfile>(apiUrl('/api/players/lookup'), {battleTag});
  }

  recent() {
    return this.http.get<RecentProfile[]>(apiUrl('/api/players/recent'));
  }

  saved() {
    return this.http.get<RecentProfile[]>(apiUrl('/api/players/saved'));
  }

  stored(battleTag: string) {
    return this.http.get<PlayerProfile>(apiUrl(`/api/players/${encodeURIComponent(toRouteBattleTag(battleTag))}/stored`));
  }

  refresh(battleTag: string) {
    return this.http.post<PlayerProfile>(apiUrl(`/api/players/${encodeURIComponent(toRouteBattleTag(battleTag))}/refresh`), {});
  }

  history(battleTag: string) {
    return this.http.get<HistoryPoint[]>(apiUrl(`/api/players/${encodeURIComponent(toRouteBattleTag(battleTag))}/history`));
  }

  references(battleTag: string, role: string) {
    return this.http.get<RankReference>(apiUrl(`/api/players/${encodeURIComponent(toRouteBattleTag(battleTag))}/references/${role}`));
  }

  heroes() {
    return this.http.get<HeroPortrait[]>(apiUrl('/api/heroes'));
  }
}
