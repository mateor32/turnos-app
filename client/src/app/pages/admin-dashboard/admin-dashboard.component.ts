import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, Subscription } from 'rxjs';
import { Turno, TurnoService } from '../../services/turno.service';

const SERVICE_MINUTES = 4;

function formatElapsed(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
  const seconds = (totalSeconds % 60).toString().padStart(2, '0');
  return `${minutes}:${seconds}`;
}

@Component({
  imports: [RouterLink],
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css',
})
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
  );
  protected readonly waitingCount = computed(() => this.pendingQueue().length);
  protected readonly elapsedLabel = computed(() => formatElapsed(this.elapsedSeconds()));

  private timerHandle?: ReturnType<typeof setInterval>;
  private recallHandle?: ReturnType<typeof setTimeout>;
  private request?: Subscription;

  ngOnInit(): void {
    this.loadDashboard();
    this.timerHandle = setInterval(() => {
      if (this.currentTicket()) this.elapsedSeconds.update((seconds) => seconds + 1);
    }, 1000);
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
    });
  }

  protected completeAttention(): void {
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

  private loadDashboard(showLoading = true): void {
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
  }
}
