import { PlayerProfile } from './models';
import { PlayerStore } from './player-store';

const profile: PlayerProfile = {
  battleTag: 'Ana#1234', username: 'Ana', avatar: null, namecard: null, platform: 'pc',
  capturedAt: '2026-08-13T00:00:00Z', timePlayed: 0, gamesPlayed: 0, gamesWon: 0,
  gamesLost: 0, winRate: 0, ranks: [], heroes: [], globalStats: {}, demo: false
};

describe('PlayerStore', () => {
  it('begins loading and clears the prior error', () => {
    const store = new PlayerStore();
    store.fail('prior error');

    store.begin();

    expect(store.loading()).toBe(true);
    expect(store.error()).toBe('');
  });

  it('stores a profile and ends loading', () => {
    const store = new PlayerStore();
    store.begin();

    store.setProfile(profile);

    expect(store.profile()).toBe(profile);
    expect(store.loading()).toBe(false);
  });

  it('stores an error and ends loading', () => {
    const store = new PlayerStore();
    store.begin();

    store.fail('x');

    expect(store.error()).toBe('x');
    expect(store.loading()).toBe(false);
  });

  it('clears all state', () => {
    const store = new PlayerStore();
    store.setProfile(profile);
    store.fail('x');

    store.clear();

    expect(store.profile()).toBeNull();
    expect(store.loading()).toBe(false);
    expect(store.error()).toBe('');
  });
});
