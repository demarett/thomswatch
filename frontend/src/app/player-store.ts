import { Injectable, signal } from '@angular/core';
import { PlayerProfile } from './models';

@Injectable({providedIn: 'root'})
export class PlayerStore {
  readonly profile = signal<PlayerProfile | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');

  setProfile(profile: PlayerProfile): void {
    this.profile.set(profile);
    this.loading.set(false);
  }

  begin(): void {
    this.loading.set(true);
    this.error.set('');
  }

  fail(message: string): void {
    this.error.set(message);
    this.loading.set(false);
  }

  clear(): void {
    this.profile.set(null);
    this.loading.set(false);
    this.error.set('');
  }
}
