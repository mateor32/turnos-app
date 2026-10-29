import { Routes } from '@angular/router';
import { AccessComponent } from './pages/access/access.component';
import { ClientComponent } from './pages/client/client.component';
import { AdminDashboardComponent } from './pages/admin-dashboard/admin-dashboard.component';
import { HomeComponent } from './pages/home/home.component';

export const routes: Routes = [
  { path: '', component: HomeComponent, title: 'TurnoFácil | Gestiona tu turno' },
  {
    path: 'clientes/turno',
    component: ClientComponent,
    title: 'Mi turno | TurnoFácil',
  },
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
  {
    path: 'administradores/panel',
    component: AdminDashboardComponent,
    title: 'Panel del asesor | TurnoFácil',
  },
  { path: '**', redirectTo: '' },
];
