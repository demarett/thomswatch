import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { routes } from '../app.routes';
import { PlayerProfile, RecentProfile } from '../models';
import { PlayerApi } from '../player-api';
import { PlayerStore } from '../player-store';
import { CompareComponent } from './compare.component';

const profile=(battleTag:string,username:string,winRate:number):PlayerProfile=>({battleTag,username,avatar:null,namecard:null,platform:'pc',capturedAt:'2026-08-23T00:00:00Z',timePlayed:3600,gamesPlayed:10,gamesWon:5,gamesLost:5,winRate,ranks:[],heroes:[],globalStats:{},demo:false});
const saved:RecentProfile[]=[
  {battleTag:'Ana#1234',username:'Ana',avatar:null,platform:'pc',lastViewedAt:'2026-08-23T00:00:00Z'},
  {battleTag:'Tracer#5678',username:'Tracer',avatar:null,platform:'pc',lastViewedAt:'2026-08-23T00:00:00Z'}
];

describe('CompareComponent',()=>{
  it('loads and displays two saved profiles',async()=>{
    const api={saved:vi.fn().mockReturnValue(of(saved)),stored:vi.fn((tag:string)=>of(tag==='Ana#1234'?profile(tag,'Ana',55):profile(tag,'Tracer',48)))};
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter(routes),{provide:PlayerApi,useValue:api},{provide:PlayerStore,useFactory:()=>new PlayerStore()}]});
    const harness=await RouterTestingHarness.create();
    await harness.navigateByUrl('/comparaison',CompareComponent);
    harness.detectChanges();

    expect(api.stored).toHaveBeenCalledTimes(2);
    expect(harness.routeNativeElement?.textContent).toContain('Ana');
    expect(harness.routeNativeElement?.textContent).toContain('Tracer');
    expect(harness.routeNativeElement?.textContent).toContain('Taux de victoire');
  });
});
