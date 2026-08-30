import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { HistoryPoint } from '../models';
import { PlayerApi } from '../player-api';
import { fromRouteBattleTag } from '../player-route';

type Metric = 'timePlayed'|'winRate';

@Component({standalone:true, imports:[DatePipe,DecimalPipe,RouterLink,MatButtonModule], template:`
<div class="section-title"><div><span class="eyebrow">CHRONOLOGIE</span><h1>Votre évolution</h1><p>Chaque actualisation ajoute un point. Une nouvelle valeur apparaît uniquement lorsque les statistiques publiques Blizzard changent.</p></div></div>
@if(points().length){
  <section class="charts">
    <article><h2>Temps de jeu</h2><strong>{{hours(points().at(-1)!.timePlayed)}} h</strong><small>{{variation('timePlayed')}}</small><div class="chart"><svg viewBox="0 0 600 180" preserveAspectRatio="none" role="img" aria-label="Évolution du temps de jeu"><path class="chart-grid" d="M0 30H600 M0 90H600 M0 150H600"/><polyline [attr.points]="line('timePlayed')" fill="none" stroke="#f47b20" stroke-width="5" vector-effect="non-scaling-stroke"/>@for(point of coordinates('timePlayed');track $index){<circle [attr.cx]="point.x" [attr.cy]="point.y" r="6" fill="#f47b20" stroke="white" stroke-width="2" vector-effect="non-scaling-stroke"/>}</svg><div class="chart-axis"><span>{{points()[0].capturedAt|date:'dd/MM'}}</span><span>{{points().at(-1)!.capturedAt|date:'dd/MM'}}</span></div></div></article>
    <article><h2>Taux de victoire</h2><strong>{{points().at(-1)!.winRate|number:'1.1-1'}} %</strong><small>{{variation('winRate')}}</small><div class="chart purple"><svg viewBox="0 0 600 180" preserveAspectRatio="none" role="img" aria-label="Évolution du taux de victoire"><path class="chart-grid" d="M0 30H600 M0 90H600 M0 150H600"/><polyline [attr.points]="line('winRate')" fill="none" stroke="#8a72f8" stroke-width="5" vector-effect="non-scaling-stroke"/>@for(point of coordinates('winRate');track $index){<circle [attr.cx]="point.x" [attr.cy]="point.y" r="6" fill="#8a72f8" stroke="white" stroke-width="2" vector-effect="non-scaling-stroke"/>}</svg><div class="chart-axis"><span>{{points()[0].capturedAt|date:'dd/MM'}}</span><span>{{points().at(-1)!.capturedAt|date:'dd/MM'}}</span></div></div></article>
  </section>
  <section class="history-list"><h2>Snapshots enregistrés <small>({{points().length}})</small></h2>@for(p of points().slice().reverse();track p.capturedAt){<div><time>{{p.capturedAt|date:'dd/MM/yyyy HH:mm:ss'}}</time><span>{{hours(p.timePlayed)}} h</span><span>{{p.winRate|number:'1.1-1'}} % victoire</span></div>}</section>
} @else {
  <section class="empty"><h2>Aucune donnée enregistrée</h2><p>Actualisez le profil pour créer le premier snapshot.</p><a mat-flat-button [routerLink]="['/profil',routeTag]">Retour au profil</a></section>
}`})
export class HistoryComponent implements OnInit {
  readonly points=signal<HistoryPoint[]>([]);
  routeTag='';
  constructor(private readonly api:PlayerApi,private readonly route:ActivatedRoute){}
  ngOnInit(){this.routeTag=this.route.snapshot.paramMap.get('battleTag')??'';this.api.history(fromRouteBattleTag(this.routeTag)).subscribe(points=>this.points.set(points))}
  hours(seconds:number){return Math.round(seconds/360)/10}
  coordinates(key:Metric){
    const values=this.points().map(point=>point[key]);
    const min=Math.min(...values),max=Math.max(...values),flat=max===min;
    return values.map((value,index)=>({x:values.length===1?300:index*600/(values.length-1),y:flat?90:160-(value-min)*130/(max-min)}));
  }
  line(key:Metric){return this.coordinates(key).map(point=>`${point.x},${point.y}`).join(' ')}
  variation(key:Metric){
    if(this.points().length<2)return 'Premier snapshot enregistré';
    const delta=this.points().at(-1)![key]-this.points()[0][key];
    if(delta===0)return `Aucune variation sur ${this.points().length} snapshots`;
    if(key==='timePlayed')return `${delta>0?'+':''}${this.hours(delta)} h depuis le premier snapshot`;
    return `${delta>0?'+':''}${new Intl.NumberFormat('fr-FR',{maximumFractionDigits:1}).format(delta)} point(s) depuis le premier snapshot`;
  }
}
