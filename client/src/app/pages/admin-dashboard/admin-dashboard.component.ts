import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

interface QueueTicket {
  id: number;
  code: string;
  clientName: string;
  service: string;
  priority: boolean;
  priorityLabel?: string;
  requestedAt: number;
}

const SERVICE_MINUTES = 4;

function minutesAgo(minutes: number): number {
  return Date.now() - minutes * 60_000;
}

function formatElapsed(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60)
    .toString()
    .padStart(2, '0');
  const seconds = (totalSeconds % 60).toString().padStart(2, '0');
  return `${minutes}:${seconds}`;
}

function createSeedQueue(): QueueTicket[] {
  return [
    {
      id: 1,
      code: 'A-024',
      clientName: 'Jorge Ramírez Peña',
      service: 'Adulto mayor · 74 años',
      priority: true,
      priorityLabel: 'Prioritario',
      requestedAt: minutesAgo(18),
    },
    {
      id: 2,
      code: 'A-025',
      clientName: 'Valentina Gómez Ruiz',
      service: 'Asesoría crédito hipotecario',
      priority: false,
      requestedAt: minutesAgo(14),
    },
    {
      id: 3,
      code: 'A-026',
      clientName: 'Andrés Felipe Castro',
      service: 'Emisión certificado bancario',
      priority: false,
      requestedAt: minutesAgo(9),
    },
    {
      id: 4,
      code: 'A-027',
      clientName: 'Laura Beltrán Suárez',
      service: 'Actualización de datos',
      priority: false,
      requestedAt: minutesAgo(3),
    },
  ];
}

@Component({
  imports: [RouterLink],
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css',
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  protected readonly pendingQueue = signal<QueueTicket[]>(createSeedQueue());
  protected readonly currentTicket = signal<QueueTicket | null>({
    id: 0,
    code: 'A-023',
    clientName: 'Mariana Restrepo Ortiz',
    service: 'Apertura cuenta nómina',
    priority: false,
    requestedAt: minutesAgo(8),
  });
  protected readonly servedToday = signal(22);
  protected readonly waitTimesMinutes = signal<number[]>([5, 6, 4, 7, 5]);
  protected readonly elapsedSeconds = signal(225);
  protected readonly isRecalling = signal(false);

  protected readonly sortedQueue = computed(() =>
    [...this.pendingQueue()].sort((a, b) => {
      if (a.priority !== b.priority) return a.priority ? -1 : 1;
      return a.requestedAt - b.requestedAt;
    }),
  );

  protected readonly waitingCount = computed(() => this.pendingQueue().length);

  protected readonly averageWaitMinutes = computed(() => {
    const times = this.waitTimesMinutes();
    if (!times.length) return 0;
    return Math.round(times.reduce((total, value) => total + value, 0) / times.length);
  });

  protected readonly elapsedLabel = computed(() => formatElapsed(this.elapsedSeconds()));

  private timerHandle?: ReturnType<typeof setInterval>;
  private recallHandle?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    this.timerHandle = setInterval(() => {
      if (this.currentTicket()) {
        this.elapsedSeconds.update((seconds) => seconds + 1);
      }
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timerHandle) clearInterval(this.timerHandle);
    if (this.recallHandle) clearTimeout(this.recallHandle);
  }

  protected estimatedWait(position: number): number {
    return (position + 1) * SERVICE_MINUTES;
  }

  protected callNext(): void {
    if (this.currentTicket()) {
      this.servedToday.update((count) => count + 1);
    }

    const queue = this.sortedQueue();
    const next = queue[0];

    if (!next) {
      this.currentTicket.set(null);
      return;
    }

    this.pendingQueue.update((tickets) => tickets.filter((ticket) => ticket.id !== next.id));
    const waitMinutes = Math.max(0, Math.round((Date.now() - next.requestedAt) / 60_000));
    this.waitTimesMinutes.update((times) => [...times, waitMinutes]);
    this.currentTicket.set(next);
    this.elapsedSeconds.set(0);
  }

  protected completeAttention(): void {
    if (!this.currentTicket()) return;
    this.servedToday.update((count) => count + 1);
    this.currentTicket.set(null);
    this.elapsedSeconds.set(0);
  }

  protected recall(): void {
    if (!this.currentTicket()) return;
    this.isRecalling.set(true);
    if (this.recallHandle) clearTimeout(this.recallHandle);
    this.recallHandle = setTimeout(() => this.isRecalling.set(false), 1200);
  }
}
