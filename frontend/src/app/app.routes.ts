import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home.component';
import { ProfileComponent } from './pages/profile.component';
import { HistoryComponent } from './pages/history.component';
import { HelpComponent } from './pages/help.component';
export const routes:Routes=[{path:'',component:HomeComponent},{path:'profil',component:ProfileComponent},{path:'historique',component:HistoryComponent},{path:'aide',component:HelpComponent},{path:'**',redirectTo:''}];

