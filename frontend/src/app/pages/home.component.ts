import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { RecentProfile } from '../models';
import { PlayerApi } from '../player-api';
import { toRouteBattleTag } from '../player-route';
import { PlayerStore } from '../player-store';

@Component({standalone:true,imports:[DatePipe,ReactiveFormsModule,MatButtonModule,MatFormFieldModule,MatInputModule,MatIconModule],template:`
<section class="hero"><div class="eyebrow">VOTRE CARRIÈRE, LISIBLE</div><h1>Chaque partie raconte<br><span>votre progression.</span></h1><p>Recherchez votre profil public Overwatch, explorez vos héros et suivez votre évolution au fil du temps.</p>
<form (submit)="submit($event)"><mat-form-field appearance="outline"><mat-label>BattleTag public</mat-label><input matInput [formControl]="tag" placeholder="Pseudo#1234" autocomplete="off"><mat-icon matPrefix>person_search</mat-icon>@if(tag.invalid&&tag.touched){<mat-error>Format attendu : Pseudo#1234</mat-error>}</mat-form-field><button mat-flat-button type="submit" [disabled]="tag.invalid||loading()">{{loading()?'Recherche…':'Voir mon profil'}}</button></form>
@if(error()){<div class="error" role="alert"><mat-icon>error_outline</mat-icon>{{error()}}</div>}
<button mat-button class="demo" (click)="demo()"><mat-icon>play_circle</mat-icon> Explorer avec le profil de démonstration</button></section>
@if(recent().length){<section class="recent"><div class="section-title"><div><span class="eyebrow">REPRENDRE</span><h2>Profils récemment consultés</h2></div></div><div class="recent-grid">@for(profile of recent();track profile.battleTag){<button type="button" class="recent-card" (click)="open(profile)" [disabled]="loading()"><img [src]="profile.avatar||'https://placehold.co/80x80/252c3a/ffffff?text=OW'" alt=""><span><strong>{{profile.username}}</strong><small>{{profile.battleTag}} · {{profile.platform?.toUpperCase()||'Plateforme inconnue'}}</small><small>Consulté le {{profile.lastViewedAt|date:'dd/MM/yyyy à HH:mm'}}</small></span><mat-icon>arrow_forward</mat-icon></button>}</div></section>}
<section class="features"><article><mat-icon>shield</mat-icon><h2>Vos rangs</h2><p>Une vue nette par rôle et plateforme.</p></article><article><mat-icon>sports_esports</mat-icon><h2>Vos héros</h2><p>Temps de jeu, victoires et performances.</p></article><article><mat-icon>query_stats</mat-icon><h2>Votre évolution</h2><p>Des snapshots pour voir le chemin parcouru.</p></article></section>`})
export class HomeComponent implements OnInit {
  readonly tag=new FormControl('',{nonNullable:true,validators:[Validators.required,Validators.pattern(/^[^#\s]{2,32}#[0-9]{3,12}$/)]});
  readonly loading=signal(false); readonly error=signal(''); readonly recent=signal<RecentProfile[]>([]);
  constructor(private readonly api:PlayerApi,private readonly store:PlayerStore,private readonly router:Router){}
  ngOnInit(){this.api.recent().subscribe({next:profiles=>this.recent.set(profiles)})}
  submit(event:SubmitEvent){event.preventDefault();this.search()}
  search(){if(this.tag.invalid)return;this.run(this.tag.value)}
  demo(){this.run('Demo#0000')}
  open(profile:RecentProfile){this.loading.set(true);this.error.set('');this.store.begin();this.api.stored(profile.battleTag).subscribe({next:p=>{this.store.setProfile(p);void this.router.navigate(['/profil',toRouteBattleTag(p.battleTag)])},error:()=>{this.store.fail('Impossible de rouvrir ce profil.');this.loading.set(false);this.error.set('Impossible de rouvrir ce profil.')},complete:()=>this.loading.set(false)})}
  private run(tag:string){this.loading.set(true);this.error.set('');this.store.begin();this.api.lookup(tag).subscribe({next:p=>{this.store.setProfile(p);void this.router.navigate(['/profil',toRouteBattleTag(p.battleTag)])},error:(e:HttpErrorResponse)=>{const message=e.error?.message??'Une erreur inattendue est survenue.';this.store.fail(message);this.loading.set(false);this.error.set(message)},complete:()=>this.loading.set(false)});}
}
