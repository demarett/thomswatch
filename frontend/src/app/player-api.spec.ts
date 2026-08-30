import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PlayerApi } from './player-api';

describe('PlayerApi', () => {
  let api: PlayerApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});
    api = TestBed.inject(PlayerApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('looks up a BattleTag', () => {
    api.lookup('Ana#1234').subscribe();
    const request = http.expectOne('/api/players/lookup');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({battleTag: 'Ana#1234'});
  });

  it('loads recent profiles', () => {
    api.recent().subscribe();
    expect(http.expectOne('/api/players/recent').request.method).toBe('GET');
  });

  it('loads every saved profile for comparison', () => {
    api.saved().subscribe();
    expect(http.expectOne('/api/players/saved').request.method).toBe('GET');
  });

  it('loads a stored profile using its route BattleTag', () => {
    api.stored('Ana#1234').subscribe();
    expect(http.expectOne('/api/players/Ana-1234/stored').request.method).toBe('GET');
  });

  it('refreshes a profile using its route BattleTag', () => {
    api.refresh('Ana#1234').subscribe();
    const request = http.expectOne('/api/players/Ana-1234/refresh');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
  });

  it('loads history using its route BattleTag', () => {
    api.history('Ana#1234').subscribe();
    expect(http.expectOne('/api/players/Ana-1234/history').request.method).toBe('GET');
  });

  it('loads hero portraits', () => {
    api.heroes().subscribe();
    expect(http.expectOne('/api/heroes').request.method).toBe('GET');
  });
});
