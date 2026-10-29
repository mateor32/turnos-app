import { Routes } from '@angular/router';
import { AccessComponent } from './pages/access/access.component';
import { HomeComponent } from './pages/home/home.component';

export const routes: Routes = [
  { path: '', component: HomeComponent, title: 'TurnoFácil | Gestiona tu turno' },
  {
    path: 'clientes',
    component: AccessComponent,
    data: { role: 'client' },
    title: 'Acceso clientes | TurnoFácil',
  },
  {
    path: 'administradores',
    component: AccessComponent,
    data: { role: 'admin' },
    title: 'Panel asesor | TurnoFácil',
  },
  { path: '**', redirectTo: '' },
];
