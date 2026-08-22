import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { routes } from '../app.routes';
import { HistoryPoint } from '../models';
import { PlayerApi } from '../player-api';
import { PlayerStore } from '../player-store';
import { HistoryComponent } from './history.component';

const history: HistoryPoint[] = [{
  capturedAt: '2026-08-13T00:00:00Z', timePlayed: 3600, winRate: 50,
  tankRank: null, damageRank: null, supportRank: null
}];

describe('HistoryComponent route loading', () => {
  const api = {
    lookup: vi.fn(), recent: vi.fn(), stored: vi.fn(), refresh: vi.fn(), history: vi.fn(), heroes: vi.fn()
  };

  beforeEach(() => {
    api.lookup.mockReset().mockReturnValue(of(undefined));
    api.recent.mockReset().mockReturnValue(of([]));
    api.stored.mockReset().mockReturnValue(of(undefined));
    api.refresh.mockReset().mockReturnValue(of(undefined));
    api.history.mockReset().mockReturnValue(of(history));
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

  it('always loads history for the routed player', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/historique/Ana-1234', HistoryComponent);

    expect(api.history).toHaveBeenCalledWith('Ana#1234');
    expect(harness.routeNativeElement?.textContent).toContain('Snapshots enregistrés');
  });
});
