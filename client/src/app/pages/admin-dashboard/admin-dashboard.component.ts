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

const SERVICE_MINUTES = 4;

function formatElapsed(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60)
    .toString()
    .padStart(2, '0');
  const seconds = (totalSeconds % 60).toString().padStart(2, '0');
  return `${minutes}:${seconds}`;
}

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

@Component({
  imports: [RouterLink],
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css',
})
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
  );

  protected readonly waitingCount = computed(() => this.pendingQueue().length);

  protected readonly averageWaitMinutes = computed(() =>
    Math.round(this.averageWaitFromBackend()),
  );

  protected readonly elapsedLabel = computed(() => formatElapsed(this.elapsedSeconds()));

  private timerHandle?: ReturnType<typeof setInterval>;
  private recallHandle?: ReturnType<typeof setTimeout>;

  constructor() {
    afterNextRender(() => {
      this.reload();
      this.timerHandle = setInterval(() => {
        if (this.currentTicket()) {
          this.elapsedSeconds.update((seconds) => seconds + 1);
        }
      }, 1000);
    });
  }

  ngOnDestroy(): void {
    if (this.timerHandle) clearInterval(this.timerHandle);
    if (this.recallHandle) clearTimeout(this.recallHandle);
  }

  protected estimatedWait(position: number): number {
    return (position + 1) * SERVICE_MINUTES;
  }

  protected callNext(): void {
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
    });
  }

  protected completeAttention(): void {
    if (!this.currentTicket()) return;
    this.currentTicket.set(null);
    this.elapsedSeconds.set(0);
  }

  protected recall(): void {
    if (!this.currentTicket()) return;
    this.isRecalling.set(true);
    if (this.recallHandle) clearTimeout(this.recallHandle);
    this.recallHandle = setTimeout(() => this.isRecalling.set(false), 1200);
  }

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
  }
}
