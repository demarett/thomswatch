import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { routes } from '../app.routes';
import { PlayerApi } from '../player-api';
import { PlayerStore } from '../player-store';
import { PlayerProfile, RecentProfile } from '../models';
import { HomeComponent } from './home.component';

const profile: PlayerProfile = {
  battleTag: 'Ana#1234', username: 'Ana', avatar: null, namecard: null, platform: 'pc',
  capturedAt: '2026-08-13T00:00:00Z', timePlayed: 0, gamesPlayed: 0, gamesWon: 0,
  gamesLost: 0, winRate: 0, ranks: [], heroes: [], globalStats: {}, demo: false
};

const recent: RecentProfile = {
  battleTag: 'Ana#1234', username: 'Ana', avatar: null, platform: 'pc',
  lastViewedAt: '2026-08-13T00:00:00Z'
};

describe('HomeComponent route navigation', () => {
  const api = {
    lookup: vi.fn(), recent: vi.fn(), stored: vi.fn(), refresh: vi.fn(), history: vi.fn(), heroes: vi.fn()
  };

  beforeEach(() => {
    api.lookup.mockReset().mockReturnValue(of(profile));
    api.recent.mockReset().mockReturnValue(of([]));
    api.stored.mockReset().mockReturnValue(of(profile));
    api.refresh.mockReset().mockReturnValue(of(profile));
    api.history.mockReset().mockReturnValue(of([]));
    api.heroes.mockReset().mockReturnValue(of([]));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        provideRouter(routes),
        {provide: PlayerApi, useValue: api},
        {provide: PlayerStore, useFactory: () => new PlayerStore()}
      ]
    });
  });

  it('navigates a successful lookup to the canonical profile route', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/', HomeComponent);
    component.tag.setValue('Ana#1234');

    component.search();
    await harness.fixture.whenStable();

    expect(api.lookup).toHaveBeenCalledWith('Ana#1234');
    expect(TestBed.inject(Router).url).toBe('/profil/Ana-1234');
  });

  it('loads a recent profile before navigating to its canonical route', async () => {
    api.recent.mockReturnValue(of([recent]));
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/', HomeComponent);
    harness.detectChanges();

    const recentButton = harness.routeNativeElement?.querySelector<HTMLButtonElement>('.recent-card');
    expect(recentButton).not.toBeNull();
    recentButton!.click();
    await harness.fixture.whenStable();

    expect(api.stored).toHaveBeenCalledWith('Ana#1234');
    expect(TestBed.inject(Router).url).toBe('/profil/Ana-1234');
  });

  it('renders the French HTTP error message returned by lookup', async () => {
    api.lookup.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 404, error: {message: 'Profil Overwatch introuvable.'}
    })));
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/', HomeComponent);
    component.tag.setValue('Ana#1234');

    component.search();
    harness.detectChanges();

    expect(harness.routeNativeElement?.querySelector('[role="alert"]')?.textContent)
      .toContain('Profil Overwatch introuvable.');
  });
});
