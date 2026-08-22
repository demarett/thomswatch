import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { PlayerStore } from './player-store';
import { toRouteBattleTag } from './player-route';

@Component({selector:'app-root',standalone:true,imports:[RouterOutlet,RouterLink,RouterLinkActive,MatIconModule],template:`
<header><a class="brand" routerLink="/"><span class="mark">OT</span><span>Overwatch Tracker</span></a><nav><a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">Recherche</a>@if(player.profile();as profile){<a [routerLink]="['/profil',routeTag(profile.battleTag)]" routerLinkActive="active">Profil</a><a [routerLink]="['/historique',routeTag(profile.battleTag)]" routerLinkActive="active">Historique</a>}<a routerLink="/aide" routerLinkActive="active">Aide</a></nav></header>
<main><router-outlet/></main><footer>Projet personnel non affilié à Blizzard Entertainment.</footer>`})
export class AppComponent { constructor(readonly player:PlayerStore){} routeTag(tag:string){return toRouteBattleTag(tag)} }
