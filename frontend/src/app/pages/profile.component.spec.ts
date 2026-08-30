import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { AppComponent } from '../app.component';
import { routes } from '../app.routes';
import { PlayerApi } from '../player-api';
import { PlayerStore } from '../player-store';
import { PlayerProfile } from '../models';
import { ProfileComponent } from './profile.component';

const profile: PlayerProfile = {
  battleTag: 'Ana#1234', username: 'Ana', avatar: null, namecard: null, platform: 'pc',
  capturedAt: '2026-08-13T00:00:00Z', timePlayed: 0, gamesPlayed: 0, gamesWon: 0,
  gamesLost: 0, winRate: 0, ranks: [], heroes: [], globalStats: {}, demo: false
};

const detailedProfile: PlayerProfile = {
  ...profile,
  heroes: [{
    key: 'ana', name: 'Ana', timePlayed: 3600, gamesPlayed: 5, gamesWon: 3, winRate: 60,
    stats: {combat: {label: 'Combat', stats: [{key: 'eliminations', label: 'Eliminations', value: 42}]}}
  }]
};

describe('ProfileComponent route loading', () => {
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

  it('loads the routed profile when it is not already in the store', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/profil/Ana-1234', ProfileComponent);

    expect(api.stored).toHaveBeenCalledWith('Ana#1234');
    expect(TestBed.inject(PlayerStore).profile()).toBe(profile);
  });

  it('does not reload the routed profile when the store already has it', async () => {
    TestBed.inject(PlayerStore).setProfile(profile);
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/profil/Ana-1234', ProfileComponent);

    expect(api.stored).not.toHaveBeenCalled();
  });

  it('refreshes the player identified by the route', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/profil/Ana-1234', ProfileComponent);
    harness.detectChanges();

    harness.routeNativeElement?.querySelector<HTMLButtonElement>('button')?.click();

    expect(api.refresh).toHaveBeenCalledWith('Ana#1234');
  });

  it('opens a readable detailed hero card', async () => {
    api.stored.mockReturnValue(of(detailedProfile));
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/profil/Ana-1234', ProfileComponent);
    harness.detectChanges();

    harness.routeNativeElement?.querySelector<HTMLButtonElement>('.hero-row')?.click();
    harness.detectChanges();

    expect(harness.routeNativeElement?.querySelector('.hero-detail')?.textContent).toContain('FICHE DU HÉROS');
    expect(harness.routeNativeElement?.querySelector('.stat-grid')?.textContent).toContain('Éliminations');
    expect(harness.routeNativeElement?.querySelector('.stat-grid')?.textContent).toContain('42');
  });
});

describe('AppComponent player navigation', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter(routes)]
    });
  });

  it('shows canonical player links only while a profile is stored', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href^="/profil"]')).toBeNull();

    TestBed.inject(PlayerStore).setProfile(profile);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('a[href="/profil/Ana-1234"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/historique/Ana-1234"]')).not.toBeNull();
  });
});
