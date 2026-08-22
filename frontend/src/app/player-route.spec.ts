import { fromRouteBattleTag, toRouteBattleTag } from './player-route';

describe('player route BattleTags', () => {
  it('converts a BattleTag separator for a route', () => {
    expect(toRouteBattleTag('Ana#1234')).toBe('Ana-1234');
  });

  it('restores only the final numeric route separator', () => {
    expect(fromRouteBattleTag('Ana-Marie-1234')).toBe('Ana-Marie#1234');
  });
});
