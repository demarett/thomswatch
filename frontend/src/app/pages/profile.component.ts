import { DecimalPipe } from '@angular/common';
import { Component, HostListener, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { Hero, HeroStat, RankReference } from '../models';
import { coachHero, rankHeroes } from '../hero-coaching';
import { PlayerApi } from '../player-api';
import { fromRouteBattleTag, toRouteBattleTag } from '../player-route';
import { PlayerStore } from '../player-store';

@Component({
  standalone: true,
  imports: [DecimalPipe, RouterLink, MatButtonModule, MatIconModule],
  template: `
@if (player.profile(); as p) {
  <section class="profile-head" [style.background-image]="p.namecard ? 'linear-gradient(90deg,#111827ee,#11182799),url('+p.namecard+')' : ''">
    <img [src]="p.avatar || 'https://placehold.co/128x128/252c3a/ffffff?text=OW'" alt="Avatar">
    <div><span class="eyebrow">PROFIL PUBLIC</span><h1>{{p.username}}</h1><p>{{p.battleTag}} · {{p.platform?.toUpperCase() || 'Plateforme inconnue'}}</p></div>
    <button mat-flat-button (click)="refresh()" [disabled]="loading"><mat-icon>refresh</mat-icon>{{loading ? 'Actualisation…' : 'Actualiser'}}</button>
  </section>
  <section class="metric-grid">
    <article><span>Temps de jeu</span><strong>{{hours(p.timePlayed)}} h</strong><small>mode compétitif</small></article>
    <article><span>Taux de victoire</span><strong>{{p.winRate | number:'1.1-1'}} %</strong><small>{{p.gamesWon}} V · {{p.gamesLost}} D</small></article>
    <article><span>Parties jouées</span><strong>{{p.gamesPlayed}}</strong><small>profil public</small></article>
  </section>
  <div class="section-title"><div><span class="eyebrow">COMPÉTITIF</span><h2>Rangs par rôle</h2></div></div>
  <section class="rank-grid">
    @for (r of p.ranks; track r.role) {<article><span class="role-icon">{{roleIcon(r.role)}}</span><div><small>{{roleName(r.role)}}</small><h3>{{r.division}} {{r.tier}}</h3></div></article>}
    @empty {<p class="muted">Aucun rang public disponible.</p>}
  </section>
  @if (ranking(p.heroes); as ranking) {
    @if (ranking.top.length || ranking.flop.length) {
      <div class="section-title"><div><span class="eyebrow">BILAN DE PERFORMANCE</span><h2>Top 3 et axes prioritaires</h2><p>Score composé à 60 % des objectifs de coaching et à 40 % du taux de victoire.</p></div></div>
      <section class="hero-rankings">
        <div class="ranking-column top"><h3><mat-icon>emoji_events</mat-icon>Top 3</h3>
          @for(item of ranking.top;track item.hero.key){<button type="button" (click)="selectHero(item.hero)"><span class="ranking-position">{{$index+1}}</span><img [src]="portrait(item.hero.key)" [alt]="item.hero.name" (error)="imageFallback($event)"><span class="ranking-copy"><strong>{{item.hero.name}}</strong><small>{{item.hero.winRate|number:'1.0-1'}} % victoire · {{item.good}} indicateur(s) dans la cible</small></span><b>{{item.score}}<small>/100</small></b></button>}
        </div>
        <div class="ranking-column flop"><h3><mat-icon>construction</mat-icon>À travailler</h3>
          @for(item of ranking.flop;track item.hero.key){<button type="button" (click)="selectHero(item.hero)"><span class="ranking-position">{{$index+1}}</span><img [src]="portrait(item.hero.key)" [alt]="item.hero.name" (error)="imageFallback($event)"><span class="ranking-copy"><strong>{{item.hero.name}}</strong><small>{{item.hero.winRate|number:'1.0-1'}} % victoire · {{item.improve}} indicateur(s) à améliorer</small></span><b>{{item.score}}<small>/100</small></b></button>}
        </div>
      </section>
      <p class="ranking-note"><mat-icon>info</mat-icon>Seuls les héros avec au moins 5 parties, 30 minutes de jeu et des statistiques comparables sont classés.</p>
    }
  }
  <div class="section-title">
    <div><span class="eyebrow">STATISTIQUES DÉTAILLÉES</span><h2>Héros les plus joués</h2><p>Cliquez sur un héros pour consulter toute sa fiche.</p></div>
    <a [routerLink]="['/historique', routeTag(p.battleTag)]">Voir l'évolution →</a>
  </div>
  <section class="hero-table">
    <div class="table-row table-head"><span>Héros</span><span>Temps</span><span>Parties</span><span>Victoires</span><span>Taux</span></div>
    @for (h of p.heroes; track h.key) {
      <button class="table-row hero-row" type="button" (click)="selectHero(h)" [class.selected]="selectedHero()?.key === h.key" [attr.aria-expanded]="selectedHero()?.key === h.key">
        <strong class="hero-name"><img [src]="portrait(h.key)" [alt]="h.name" (error)="imageFallback($event)"><span>{{h.name}}</span></strong>
        <span>{{hours(h.timePlayed)}} h</span><span>{{h.gamesPlayed}}</span><span>{{h.gamesWon}}</span><span>{{h.winRate | number:'1.0-1'}} %</span><mat-icon>chevron_right</mat-icon>
      </button>
    } @empty {<p class="muted pad">Les statistiques par héros ne sont pas publiques.</p>}
  </section>
  @if (selectedHero(); as hero) {
    <div class="hero-detail-backdrop" (click)="closeHero()">
    <section class="hero-detail" role="dialog" aria-modal="true" [attr.aria-label]="'Statistiques détaillées de ' + hero.name" (click)="$event.stopPropagation()">
      <div class="hero-detail-head">
        <div class="hero-identity"><img [src]="portrait(hero.key)" [alt]="hero.name" (error)="imageFallback($event)"><div><span class="eyebrow">FICHE DU HÉROS</span><h2>{{hero.name}}</h2><p>{{hours(hero.timePlayed)}} h · {{hero.gamesPlayed}} parties · {{hero.winRate | number:'1.0-1'}} % de victoires</p></div></div>
        <button class="hero-close" mat-icon-button type="button" (click)="closeHero()" aria-label="Fermer la fiche"><mat-icon>close</mat-icon></button>
      </div>
      @if (coaching(hero); as coaching) {
        <section class="coaching-panel">
          <div class="coaching-title"><div><span class="eyebrow">COACHING INDICATIF</span><h3>Points de comparaison</h3></div><span class="confidence">Confiance {{coaching.confidence}}</span></div>
          <p class="coaching-disclaimer">{{referenceText(hero)}}</p>
          @if (coaching.comparisons.length) {
            <div class="comparison-grid">
              @for (item of coaching.comparisons; track item.key) {
                <article [class]="'comparison '+item.status"><span>{{item.label}}</span><strong>{{numberValue(item.value)}}</strong><small>Référence {{item.reference}}</small><em>{{statusName(item.status)}}</em></article>
              }
            </div>
            <div class="priorities">
              <h4>{{coaching.priorities.length ? 'Priorités recommandées' : 'Points clés dans la cible'}}</h4>
              @for (priority of coaching.priorities; track priority.key) {<div><mat-icon>trending_up</mat-icon><p><strong>{{priority.label}}</strong>{{priority.advice}}</p></div>}
              @empty {<div><mat-icon>verified</mat-icon><p><strong>Bon équilibre</strong>Les indicateurs comparables disponibles sont dans les objectifs indicatifs.</p></div>}
            </div>
          } @else {
            <div class="no-detail"><mat-icon>hourglass_empty</mat-icon><p>Pas assez de statistiques comparables pour produire un diagnostic sur ce héros.</p></div>
          }
        </section>
      }
      @if (heroCategories(hero).length) {
        <div class="stat-categories">
          @for (category of heroCategories(hero); track category.key) {
            <details class="stat-category" [open]="category.key === 'combat' || category.key === 'hero_specific'">
              <summary><span>{{categoryName(category.key)}}</span><small>{{category.value.stats.length}} statistiques</small></summary>
              <div class="stat-grid">
                @for (stat of category.value.stats; track stat.key) {<article><span>{{statName(stat)}}</span><strong>{{statValue(stat)}}</strong></article>}
              </div>
            </details>
          }
        </div>
      } @else {
        <div class="no-detail"><mat-icon>info</mat-icon><p>Ce snapshot ne contient pas encore les statistiques détaillées. Cliquez sur <strong>Actualiser</strong> pour les importer depuis OverFast.</p></div>
      }
    </section>
    </div>
  }
  @if (error) {<div class="error">{{error}}</div>}
} @else {<section class="empty"><h1>Aucun profil chargé</h1><a mat-flat-button routerLink="/">Rechercher un BattleTag</a></section>}
`
})
export class ProfileComponent implements OnInit {
  loading = false;
  error = '';
  readonly portraits = signal<Record<string,string>>({});
  readonly roles = signal<Record<string,string>>({});
  readonly references = signal<Record<string,RankReference>>({});
  readonly selectedHero = signal<Hero|null>(null);
  private battleTag = '';
  constructor(readonly player: PlayerStore, private readonly api: PlayerApi, private readonly route: ActivatedRoute) {}
  ngOnInit() {
    this.battleTag = fromRouteBattleTag(this.route.snapshot.paramMap.get('battleTag') ?? '');
    if (this.player.profile()?.battleTag !== this.battleTag) {
      this.player.begin();
      this.api.stored(this.battleTag).subscribe({next: p => {this.player.setProfile(p);this.loadReferences();}, error: e => this.player.fail(e.error?.message ?? 'Impossible de charger ce profil.')});
    }
    this.api.heroes().subscribe({next: heroes => {this.portraits.set(Object.fromEntries(heroes.map(h => [h.key, h.portrait])));this.roles.set(Object.fromEntries(heroes.map(h => [h.key, h.role])));this.loadReferences();}});
  }
  portrait(key: string) { return this.portraits()[key] || 'https://placehold.co/80x80/252c3a/ffffff?text=OW'; }
  imageFallback(event: Event) { (event.target as HTMLImageElement).src = 'https://placehold.co/80x80/252c3a/ffffff?text=OW'; }
  hours(seconds: number) { return Math.round(seconds / 360) / 10; }
  roleName(role: string) { return ({tank: 'Tank', damage: 'Dégâts', support: 'Soutien'} as Record<string,string>)[role] || role; }
  roleIcon(role: string) { return ({tank: '◆', damage: '⌁', support: '✚'} as Record<string,string>)[role] || '●'; }
  routeTag(tag: string) { return toRouteBattleTag(tag); }
  selectHero(hero: Hero) { this.selectedHero.set(hero); }
  closeHero() { this.selectedHero.set(null); }
  @HostListener('document:keydown.escape') onEscape() { this.closeHero(); }
  heroCategories(hero: Hero) { return Object.entries(hero.stats || {}).map(([key, value]) => ({key, value})).filter(item => Array.isArray(item.value?.stats)); }
  coaching(hero:Hero){const role=this.roles()[hero.key]||'';return coachHero(hero,role,this.references()[role]);}
  ranking(heroes:Hero[]){return rankHeroes(heroes,this.roles(),this.references());}
  referenceText(hero:Hero){
    const coaching=this.coaching(hero);
    if(coaching.comparisons.some(item=>item.source==='database')){
      return 'Moyennes calculées sur le dernier snapshot de chaque joueur enregistré du même palier. Il ne s’agit pas de données officielles Blizzard.';
    }
    const role=this.roles()[hero.key]||'';
    const reference=this.references()[role];
    const progress=reference?.division ? ` La base contient actuellement ${reference.playerCount}/${reference.minimumSampleSize} autres joueurs ${this.divisionName(reference.division)}.` : '';
    return `Objectifs pédagogiques par rôle et par héros. Les moyennes du même rang prendront automatiquement le relais à partir de 20 joueurs comparables.${progress}`;
  }
  numberValue(value:number){return new Intl.NumberFormat('fr-FR',{maximumFractionDigits:2}).format(value);}
  statusName(status:string){return ({good:'Dans la cible',watch:'À surveiller',improve:'À améliorer'} as Record<string,string>)[status]||status;}
  categoryName(key: string) { return ({combat: 'Combat', assists: 'Assistance et soins', average: 'Moyennes', best: 'Records personnels', hero_specific: 'Spécifique au héros', game: 'Parties', match_awards: 'Récompenses'} as Record<string,string>)[key] || this.humanize(key); }
  statName(stat: HeroStat) {
    const suffixes: [string,string][] = [['_avg_per_10_min', ' — moyenne / 10 min'], ['_most_in_game', ' — record sur une partie'], ['_most_in_life', ' — record sur une vie'], ['_best_in_game', ' — meilleur sur une partie'], ['_average', ' — moyenne']];
    const suffix = suffixes.find(([key]) => stat.key.endsWith(key));
    const base = suffix ? stat.key.slice(0, -suffix[0].length) : stat.key;
    const names: Record<string,string> = {eliminations:'Éliminations',deaths:'Morts',assists:'Assistances',final_blows:'Coups finaux',solo_kills:'Éliminations solo',objective_kills:'Éliminations sur objectif',multikills:'Multi-éliminations',critical_hits:'Coups critiques',critical_hit_kills:'Éliminations critiques',melee_final_blows:'Coups finaux en mêlée',environmental_kills:'Éliminations environnementales',kill_streak:'Meilleure série',kill_streak_best:'Meilleure série',damage_done:'Dégâts infligés',all_damage_done:'Dégâts totaux',hero_damage_done:'Dégâts aux héros',barrier_damage_done:'Dégâts aux barrières',healing_done:'Soins prodigués',self_healing:'Auto-soins',weapon_accuracy:'Précision',scoped_accuracy:'Précision avec viseur',unscoped_accuracy:'Précision sans viseur',sleep_dart_accuracy:'Précision fléchette hypodermique',games_played:'Parties jouées',games_won:'Victoires',games_lost:'Défaites',games_tied:'Égalités',game_tied:'Égalités',hero_wins:'Victoires avec ce héros',win_percentage:'Taux de victoire',time_played:'Temps joué',objective_time:"Temps sur l'objectif",obj_contest_time:'Temps de contestation',objective_contest_time:'Temps de contestation',time_spent_on_fire:'Temps en feu',eliminations_per_life:'Éliminations par vie',offensive_assists:'Assistances offensives',defensive_assists:'Assistances défensives',recon_assists:'Assistances de reconnaissance',cards:'Cartes',players_saved:'Joueurs sauvés',enemies_slept:'Ennemis endormis',nano_boost_assists:'Assistances Nano-boost',biotic_grenade_kills:'Éliminations grenade biotique',damage_amplified:'Dégâts amplifiés',healing_amplified:'Soins amplifiés',saves:'Sauvetages',protects:'Protections'};
    return (names[base] || this.humanize(base)) + (suffix?.[1] || '');
  }
  statValue(stat: HeroStat) {
    if (typeof stat.value !== 'number') return String(stat.value);
    if (stat.key.includes('accuracy') || stat.key.includes('percentage') || stat.key === 'of_match_on_fire') return `${this.number(stat.value)} %`;
    if (stat.key.includes('time')) return this.duration(stat.value);
    return this.number(stat.value);
  }
  refresh() {
    this.loading = true; this.error = ''; this.player.begin(); this.selectedHero.set(null);
    this.api.refresh(this.battleTag).subscribe({next: p => {this.player.setProfile(p);this.loadReferences();}, error: e => {this.player.fail(e.error?.message ?? 'Actualisation impossible.'); this.error = this.player.error(); this.loading = false;}, complete: () => this.loading = false});
  }
  private loadReferences(){
    if(!this.player.profile()||!Object.keys(this.roles()).length)return;
    forkJoin(Object.fromEntries(['tank','damage','support'].map(role=>[role,this.api.references(this.battleTag,role)])))
      .subscribe({next: references=>this.references.set(references),error:()=>this.references.set({})});
  }
  private divisionName(division:string){return ({bronze:'Bronze',silver:'Argent',gold:'Or',platinum:'Platine',diamond:'Diamant',master:'Maître',grandmaster:'Grand maître',champion:'Champion'} as Record<string,string>)[division]||division;}
  private humanize(key: string) { const text = key.replaceAll('_', ' '); return text.charAt(0).toUpperCase() + text.slice(1); }
  private number(value: number) { return new Intl.NumberFormat('fr-FR', {maximumFractionDigits: 2}).format(value); }
  private duration(seconds: number) { const hours = Math.floor(seconds / 3600); const minutes = Math.floor((seconds % 3600) / 60); const rest = Math.round(seconds % 60); return [hours && `${hours} h`, minutes && `${minutes} min`, !hours && rest && `${rest} s`].filter(Boolean).join(' ') || '0 s'; }
}
