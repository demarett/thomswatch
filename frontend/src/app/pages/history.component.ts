import { Component, OnInit } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { HistoryPoint } from '../models';
import { PlayerService } from '../player.service';

@Component({standalone:true,imports:[DatePipe,DecimalPipe,RouterLink,MatButtonModule],template:`
<div class="section-title"><div><span class="eyebrow">CHRONOLOGIE</span><h1>Votre évolution</h1><p>Chaque actualisation ajoute un nouveau point de comparaison.</p></div></div>
@if(points.length){<section class="charts"><article><h2>Temps de jeu</h2><strong>{{hours(points.at(-1)!.timePlayed)}} h</strong><div class="chart"><svg viewBox="0 0 600 180" preserveAspectRatio="none"><polyline [attr.points]="line('timePlayed')"/></svg></div></article><article><h2>Taux de victoire</h2><strong>{{points.at(-1)!.winRate|number:'1.1-1'}} %</strong><div class="chart purple"><svg viewBox="0 0 600 180" preserveAspectRatio="none"><polyline [attr.points]="line('winRate')"/></svg></div></article></section><section class="history-list"><h2>Snapshots enregistrés</h2>@for(p of points.slice().reverse();track p.capturedAt){<div><time>{{p.capturedAt|date:'dd/MM/yyyy HH:mm'}}</time><span>{{hours(p.timePlayed)}} h</span><span>{{p.winRate|number:'1.1-1'}} % victoire</span></div>}</section>}@else{<section class="empty"><h2>Pas encore assez de données</h2><p>Actualisez le profil à différents moments pour construire les graphiques.</p><a mat-flat-button routerLink="/profil">Retour au profil</a></section>}`})
export class HistoryComponent implements OnInit {points:HistoryPoint[]=[];constructor(private readonly player:PlayerService){}ngOnInit(){if(this.player.current())this.player.history().subscribe(p=>this.points=p)}hours(s:number){return Math.round(s/360)/10}line(key:'timePlayed'|'winRate'){const v=this.points.map(p=>p[key]);const min=Math.min(...v),max=Math.max(...v);return v.map((n,i)=>`${v.length===1?300:i*600/(v.length-1)},${160-(n-min)*130/Math.max(1,max-min)}`).join(' ')}}

