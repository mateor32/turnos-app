<<<<<<< HEAD
import {
  Component,
  OnDestroy,
  afterNextRender,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Turno, TurnoService } from '../../services/turno.service';

interface QueueTicket {
  id: number;
  numero: number;
  code: string;
  clientName: string;
  service: string;
  priority: boolean;
  priorityLabel?: string;
  requestedAt: number;
}
=======
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, Subscription } from 'rxjs';
import { Turno, TurnoService } from '../../services/turno.service';
>>>>>>> 4f766af0a4897589f49c4c2c9154d108840e418b

const SERVICE_MINUTES = 4;

function formatElapsed(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
  const seconds = (totalSeconds % 60).toString().padStart(2, '0');
  return `${minutes}:${seconds}`;
}

<<<<<<< HEAD
function parseLocalTime(value: string | null | undefined): number {
  if (!value) return Date.now();
  const [hh, mm, ss] = value.split(':').map((part) => parseInt(part, 10) || 0);
  const now = new Date();
  now.setHours(hh, mm, ss ?? 0, 0);
  return now.getTime();
}

function turnoToTicket(turno: Turno): QueueTicket {
  return {
    id: turno.id,
    numero: turno.numero,
    code: `A-${turno.numero.toString().padStart(3, '0')}`,
    clientName: 'Cliente',
    service: 'Atención general',
    priority: false,
    requestedAt: parseLocalTime(turno.horaSolicitud),
  };
}

=======
>>>>>>> 4f766af0a4897589f49c4c2c9154d108840e418b
@Component({
  imports: [RouterLink],
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css',
})
<<<<<<< HEAD
export class AdminDashboardComponent implements OnDestroy {
  private readonly turnoService = inject(TurnoService);

  protected readonly pendingQueue = signal<QueueTicket[]>([]);
  protected readonly currentTicket = signal<QueueTicket | null>(null);
  protected readonly servedToday = signal(0);
  protected readonly averageWaitFromBackend = signal(0);
  protected readonly elapsedSeconds = signal(0);
  protected readonly isRecalling = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly sortedQueue = computed(() =>
    [...this.pendingQueue()].sort((a, b) => a.requestedAt - b.requestedAt),
=======
export class AdminDashboardComponent implements OnInit, OnDestroy {
  private readonly turnoService = inject(TurnoService);
  protected readonly pendingQueue = signal<Turno[]>([]);
  protected readonly currentTicket = signal<Turno | null>(null);
  protected readonly servedToday = signal(0);
  protected readonly averageWaitMinutes = signal(0);
  protected readonly elapsedSeconds = signal(0);
  protected readonly isRecalling = signal(false);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  protected readonly sortedQueue = computed(() =>
    [...this.pendingQueue()].sort((a, b) => a.horaSolicitud.localeCompare(b.horaSolicitud)),
>>>>>>> 4f766af0a4897589f49c4c2c9154d108840e418b
  );
  protected readonly waitingCount = computed(() => this.pendingQueue().length);
<<<<<<< HEAD

  protected readonly averageWaitMinutes = computed(() =>
    Math.round(this.averageWaitFromBackend()),
  );

=======
>>>>>>> 4f766af0a4897589f49c4c2c9154d108840e418b
  protected readonly elapsedLabel = computed(() => formatElapsed(this.elapsedSeconds()));

  private timerHandle?: ReturnType<typeof setInterval>;
  private recallHandle?: ReturnType<typeof setTimeout>;
  private request?: Subscription;

<<<<<<< HEAD
  constructor() {
    afterNextRender(() => {
      this.reload();
      this.timerHandle = setInterval(() => {
        if (this.currentTicket()) {
          this.elapsedSeconds.update((seconds) => seconds + 1);
        }
      }, 1000);
    });
=======
  ngOnInit(): void {
    this.loadDashboard();
    this.timerHandle = setInterval(() => {
      if (this.currentTicket()) this.elapsedSeconds.update((seconds) => seconds + 1);
    }, 1000);
>>>>>>> 4f766af0a4897589f49c4c2c9154d108840e418b
  }

  ngOnDestroy(): void {
    if (this.timerHandle) clearInterval(this.timerHandle);
    if (this.recallHandle) clearTimeout(this.recallHandle);
    this.request?.unsubscribe();
  }

  protected estimatedWait(position: number): number {
    return (position + 1) * SERVICE_MINUTES;
  }

  protected callNext(): void {
<<<<<<< HEAD
    this.turnoService.siguientePendiente().subscribe({
      next: (turno) => {
        if (!turno) return;
        this.turnoService.atender(turno.id).subscribe({
          next: (atendido) => {
            this.currentTicket.set(turnoToTicket(atendido));
            this.elapsedSeconds.set(0);
            this.servedToday.update((n) => n + 1);
            this.reload();
          },
          error: (err: HttpErrorResponse) => this.showError(err),
        });
      },
      error: () => {
        this.errorMessage.set('No hay turnos pendientes.');
      },
=======
    this.turnoService.siguiente().subscribe({
      next: (turno) => this.turnoService.atender(turno.id).subscribe({
        next: (atendido) => {
          this.currentTicket.set(atendido);
          this.elapsedSeconds.set(0);
          this.loadDashboard(false);
        },
        error: () => this.showError('No fue posible llamar al siguiente turno.'),
      }),
      error: () => this.showError('No hay turnos pendientes para llamar.'),
>>>>>>> 4f766af0a4897589f49c4c2c9154d108840e418b
    });
  }

  protected completeAttention(): void {
<<<<<<< HEAD
    if (!this.currentTicket()) return;
=======
>>>>>>> 4f766af0a4897589f49c4c2c9154d108840e418b
    this.currentTicket.set(null);
    this.elapsedSeconds.set(0);
    this.loadDashboard(false);
  }

  protected recall(): void {
    if (!this.currentTicket()) return;
    this.isRecalling.set(true);
    if (this.recallHandle) clearTimeout(this.recallHandle);
    this.recallHandle = setTimeout(() => this.isRecalling.set(false), 1200);
  }

<<<<<<< HEAD
  private reload(): void {
    this.turnoService.filtrarPorEstado('pendiente').subscribe({
      next: (turnos) => this.pendingQueue.set(turnos.map(turnoToTicket)),
      error: (err: HttpErrorResponse) => this.showError(err),
    });

    this.turnoService.esperaPromedio().subscribe({
      next: (promedio) => this.averageWaitFromBackend.set(promedio),
      error: () => this.averageWaitFromBackend.set(0),
    });

    this.turnoService.filtrarPorEstado('atendido').subscribe({
      next: (turnos) => this.servedToday.set(turnos.length),
      error: () => {},
    });
  }

  private showError(err: HttpErrorResponse): void {
    const msg =
      typeof err.error === 'string' && err.error
        ? err.error
        : err.message || 'Error al comunicarse con el servidor.';
    this.errorMessage.set(msg);
    setTimeout(() => this.errorMessage.set(null), 4000);
=======
  protected loadDashboard(showLoading = true): void {
    if (showLoading) this.loading.set(true);
    this.request?.unsubscribe();
    this.request = forkJoin({
      turnos: this.turnoService.listarTodos(),
      promedio: this.turnoService.promedioEspera(),
    }).subscribe({
      next: ({ turnos, promedio }) => {
        this.pendingQueue.set(turnos.filter((turno) => turno.estado === 'pendiente'));
        this.servedToday.set(turnos.filter((turno) => turno.estado === 'atendido').length);
        this.averageWaitMinutes.set(Math.round(promedio));
        this.loading.set(false);
      },
      error: () => this.showError('No fue posible cargar la fila. Verifica que el backend esté disponible.'),
    });
  }

  private showError(message: string): void {
    this.error.set(message);
    this.loading.set(false);
>>>>>>> 4f766af0a4897589f49c4c2c9154d108840e418b
  }
}
