import { Component, OnInit, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Turno, TurnoService } from '../../services/turno.service';

@Component({
  imports: [RouterLink],
  selector: 'app-client',
  templateUrl: './client.component.html',
  styleUrl: './client.component.css',
})
export class ClientComponent implements OnInit {
  private readonly turnoService = inject(TurnoService);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  protected readonly turno = signal<Turno | null>(null);
  protected readonly position = signal(0);
  protected readonly pendingAhead = signal(0);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const savedId = sessionStorage.getItem('turnoId');
    if (savedId) {
      this.loadTurno(Number(savedId));
      return;
    }

    this.turnoService.crear().subscribe({
      next: (turno) => {
        sessionStorage.setItem('turnoId', String(turno.id));
        this.turno.set(turno);
        this.loadQueue();
      },
      error: () => this.showError('No fue posible solicitar el turno. Verifica que el backend esté disponible.'),
    });
  }

  protected refreshTurn(): void {
    const current = this.turno();
    if (current) this.loadTurno(current.id);
  }

  protected cancelTurn(): void {
    const current = this.turno();
    if (!current) return;

    this.turnoService.cancelar(current.id).subscribe({
      next: () => {
        sessionStorage.removeItem('turnoId');
        void this.router.navigate(['/clientes']);
      },
      error: () => this.showError('No fue posible cancelar el turno.'),
    });
  }

  private loadTurno(id: number): void {
    this.turnoService.listarTodos().subscribe({
      next: (turnos) => {
        const found = turnos.find((turno) => turno.id === id);
        if (!found) {
          sessionStorage.removeItem('turnoId');
          this.showError('El turno ya no está disponible.');
          return;
        }
        this.turno.set(found);
        this.updatePosition(turnos);
      },
      error: () => this.showError('No fue posible consultar el turno.'),
    });
  }

  private loadQueue(): void {
    this.turnoService.listarTodos().subscribe({
      next: (turnos) => this.updatePosition(turnos),
      error: () => this.showError('No fue posible consultar la fila.'),
    });
  }

  private updatePosition(turnos: Turno[]): void {
    const current = this.turno();
    if (!current) return;
    const pending = turnos
      .filter((turno) => turno.estado === 'pendiente')
      .sort((a, b) => a.horaSolicitud.localeCompare(b.horaSolicitud));
    const index = pending.findIndex((turno) => turno.id === current.id);
    this.pendingAhead.set(index < 0 ? 0 : index);
    this.position.set(index < 0 ? 0 : index + 1);
    this.loading.set(false);
  }

  private showError(message: string): void {
    this.error.set(message);
    this.loading.set(false);
  }
}
