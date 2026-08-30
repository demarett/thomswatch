import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { forkJoin } from 'rxjs';
import { Hero, PlayerProfile, Rank, RecentProfile } from '../models';
import { PlayerApi } from '../player-api';
import { PlayerStore } from '../player-store';

@Component({
  standalone:true,
  imports:[FormsModule,MatFormFieldModule,MatIconModule,MatSelectModule],
  template:`
  <section class="compare-page">
    <div class="section-title"><div><span class="eyebrow">FACE-À-FACE</span><h1>Comparer deux joueurs</h1><p>Comparaison basée sur le dernier snapshot enregistré de chaque profil.</p></div></div>
    @if (error()) {<div class="error"><mat-icon>error_outline</mat-icon>{{error()}}</div>}
    @if (profiles().length < 2) {
      <section class="empty compare-empty"><mat-icon>group</mat-icon><h2>Deux profils sont nécessaires</h2><p>Recherchez au moins deux BattleTags pour pouvoir les comparer.</p></section>
    } @else {
      <section class="compare-selectors">
        <mat-form-field appearance="outline"><mat-label>Premier joueur</mat-label><mat-select [ngModel]="leftTag()" (ngModelChange)="selectLeft($event)">@for(profile of profiles();track profile.battleTag){<mat-option [value]="profile.battleTag" [disabled]="profile.battleTag===rightTag()">{{profile.username}} · {{profile.battleTag}}</mat-option>}</mat-select></mat-form-field>
        <span class="versus">VS</span>
        <mat-form-field appearance="outline"><mat-label>Second joueur</mat-label><mat-select [ngModel]="rightTag()" (ngModelChange)="selectRight($event)">@for(profile of profiles();track profile.battleTag){<mat-option [value]="profile.battleTag" [disabled]="profile.battleTag===leftTag()">{{profile.username}} · {{profile.battleTag}}</mat-option>}</mat-select></mat-form-field>
      </section>
      @if (loading()) {<section class="empty"><p>Chargement de la comparaison…</p></section>}
      @if (left(); as a) {@if (right(); as b) {
        <section class="duel-head">
          <article><img [src]="a.avatar||fallback" alt=""><div><strong>{{a.username}}</strong><small>{{a.battleTag}}</small></div></article>
          <article><div><strong>{{b.username}}</strong><small>{{b.battleTag}}</small></div><img [src]="b.avatar||fallback" alt=""></article>
        </section>
        <section class="comparison-board">
          <h2>Statistiques globales</h2>
          @for(metric of metrics(a,b);track metric.label){<div class="compare-row"><strong [class.winner]="metric.winner==='left'">{{metric.left}}</strong><span>{{metric.label}}</span><strong [class.winner]="metric.winner==='right'">{{metric.right}}</strong></div>}
        </section>
        <section class="comparison-board">
          <h2>Rangs par rôle</h2>
          @for(role of roles;track role){<div class="compare-row"><strong [class.winner]="rankWinner(a,b,role)==='left'">{{rankLabel(a,role)}}</strong><span>{{roleLabel(role)}}</span><strong [class.winner]="rankWinner(a,b,role)==='right'">{{rankLabel(b,role)}}</strong></div>}
        </section>
        <section class="comparison-board hero-comparison">
          <h2>Héros les plus joués</h2>
          <div class="compare-hero-head"><strong>{{a.username}}</strong><span>Héros</span><strong>{{b.username}}</strong></div>
          @for(row of heroRows(a,b);track row.key){<div class="compare-row hero-duel"><span>{{heroValue(row.left)}}</span><strong>{{row.name}}</strong><span>{{heroValue(row.right)}}</span></div>}
          @empty {<p class="muted pad">Aucune statistique de héros disponible.</p>}
        </section>
      }}
    }
  </section>`
})
export class CompareComponent implements OnInit {
  readonly profiles=signal<RecentProfile[]>([]); readonly left=signal<PlayerProfile|null>(null); readonly right=signal<PlayerProfile|null>(null);
  readonly leftTag=signal(''); readonly rightTag=signal(''); readonly loading=signal(false); readonly error=signal('');
  readonly roles=['tank','damage','support']; readonly fallback='https://placehold.co/96x96/252c3a/ffffff?text=OW';
  constructor(private readonly api:PlayerApi,private readonly store:PlayerStore){}
  ngOnInit(){this.api.saved().subscribe({next:profiles=>{this.profiles.set(profiles);if(profiles.length>=2){const current=this.store.profile()?.battleTag;const left=profiles.find(p=>p.battleTag===current)?.battleTag||profiles[0].battleTag;const right=profiles.find(p=>p.battleTag!==left)!.battleTag;this.leftTag.set(left);this.rightTag.set(right);this.load();}},error:()=>this.error.set('Impossible de charger les profils enregistrés.')});}
  selectLeft(tag:string){this.leftTag.set(tag);this.load();} selectRight(tag:string){this.rightTag.set(tag);this.load();}
  metrics(a:PlayerProfile,b:PlayerProfile){return [
    this.metric('Temps de jeu',a.timePlayed,b.timePlayed,v=>`${Math.round(v/360)/10} h`),
    this.metric('Taux de victoire',a.winRate,b.winRate,v=>`${new Intl.NumberFormat('fr-FR',{maximumFractionDigits:1}).format(v)} %`),
    this.metric('Parties jouées',a.gamesPlayed,b.gamesPlayed,v=>String(v)),
    this.metric('Victoires',a.gamesWon,b.gamesWon,v=>String(v)),
    this.metric('Défaites',a.gamesLost,b.gamesLost,v=>String(v),true)
  ];}
  rankLabel(profile:PlayerProfile,role:string){const rank=this.rank(profile,role);return rank?`${this.division(rank.division)} ${rank.tier??''}`.trim():'Non classé';}
  rankWinner(a:PlayerProfile,b:PlayerProfile,role:string){const left=this.rank(a,role)?.score;const right=this.rank(b,role)?.score;return left==null||right==null||left===right?'none':left>right?'left':'right';}
  roleLabel(role:string){return ({tank:'Tank',damage:'Dégâts',support:'Soutien'} as Record<string,string>)[role];}
  heroRows(a:PlayerProfile,b:PlayerProfile){const left=new Map(a.heroes.map(hero=>[hero.key,hero]));const right=new Map(b.heroes.map(hero=>[hero.key,hero]));const keys=new Set([...a.heroes.slice(0,5).map(hero=>hero.key),...b.heroes.slice(0,5).map(hero=>hero.key)]);return [...keys].map(key=>({key,name:left.get(key)?.name||right.get(key)?.name||key,left:left.get(key),right:right.get(key)})).sort((x,y)=>(y.left?.timePlayed||0)+(y.right?.timePlayed||0)-(x.left?.timePlayed||0)-(x.right?.timePlayed||0));}
  heroValue(hero?:Hero){return hero?`${Math.round(hero.timePlayed/360)/10} h · ${new Intl.NumberFormat('fr-FR',{maximumFractionDigits:1}).format(hero.winRate)} %`:'—';}
  private load(){if(!this.leftTag()||!this.rightTag())return;this.loading.set(true);this.error.set('');forkJoin({left:this.api.stored(this.leftTag()),right:this.api.stored(this.rightTag())}).subscribe({next:value=>{this.left.set(value.left);this.right.set(value.right);},error:()=>this.error.set('Impossible de charger la comparaison.'),complete:()=>this.loading.set(false)});}
  private metric(label:string,left:number,right:number,format:(value:number)=>string,lowerWins=false){const winner=left===right?'none':(lowerWins?left<right:left>right)?'left':'right';return {label,left:format(left),right:format(right),winner};}
  private rank(profile:PlayerProfile,role:string):Rank|undefined{return profile.ranks.find(rank=>rank.role===role);}
  private division(value:string){return ({bronze:'Bronze',silver:'Argent',gold:'Or',platinum:'Platine',diamond:'Diamant',master:'Maître',grandmaster:'Grand maître',champion:'Champion'} as Record<string,string>)[value]||value;}
}
