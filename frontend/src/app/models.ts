export interface Rank { role:string; division:string; tier:number|null; score:number|null }
export interface Hero { key:string; name:string; timePlayed:number; gamesPlayed:number; gamesWon:number; winRate:number; stats:Record<string,unknown> }
export interface HeroPortrait { key:string; name:string; portrait:string }
export interface PlayerProfile { battleTag:string; username:string; avatar:string|null; namecard:string|null; platform:string|null; capturedAt:string; timePlayed:number; gamesPlayed:number; gamesWon:number; gamesLost:number; winRate:number; ranks:Rank[]; heroes:Hero[]; globalStats:Record<string,unknown>; demo:boolean }
export interface HistoryPoint { capturedAt:string; timePlayed:number; winRate:number; tankRank:number|null; damageRank:number|null; supportRank:number|null }
export interface RecentProfile { battleTag:string; username:string; avatar:string|null; platform:string|null; lastViewedAt:string }
export interface ApiError { code:string; message:string }
