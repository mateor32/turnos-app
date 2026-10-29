import { Component, afterNextRender, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Turno, TurnoService } from '../../services/turno.service';

const STORAGE_KEY = 'turnofacil.turnoId';
const SERVICE_MINUTES = 4;

@Component({
  imports: [RouterLink],
  selector: 'app-client',
  templateUrl: './client.component.html',
  styleUrl: './client.component.css',
})
export class ClientComponent {
  private readonly turnoService = inject(TurnoService);
  private readonly router = inject(Router);

  protected readonly turno = signal<Turno | null>(null);
  protected readonly position = signal<number>(0);
  protected readonly peopleAhead = signal<number>(0);
  protected readonly loading = signal<boolean>(true);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly code = computed(() => {
    const t = this.turno();
    return t ? `A-${t.numero.toString().padStart(3, '0')}` : '—';
  });

  protected readonly estimatedMinutes = computed(() => this.peopleAhead() * SERVICE_MINUTES);

  protected readonly previousCode = computed(() => {
    const t = this.turno();
    if (!t) return '—';
    return `A-${Math.max(1, t.numero - 1).toString().padStart(3, '0')}`;
  });

  protected readonly inAttentionCode = computed(() => {
    const t = this.turno();
    if (!t) return '—';
    return `A-${Math.max(1, t.numero - 1).toString().padStart(3, '0')}`;
  });

  constructor() {
    afterNextRender(() => this.initTurno());
  }

  protected refreshTurn(): void {
    const current = this.turno();
    if (!current) {
      this.initTurno();
      return;
    }
    this.loadTurno(current.id);
  }

  protected cancelTurn(event: Event): void {
    event.preventDefault();
    const current = this.turno();
    if (!current) {
      this.router.navigateByUrl('/clientes');
      return;
    }
    this.turnoService.cancelar(current.id).subscribe({
      next: () => {
        this.clearStoredId();
        this.router.navigateByUrl('/clientes');
      },
      error: (err: HttpErrorResponse) => {
        this.clearStoredId();
        this.showError(err);
        this.router.navigateByUrl('/clientes');
      },
    });
  }

  private initTurno(): void {
    const storedId = this.getStoredId();
    if (storedId) {
      this.loadTurno(storedId);
    } else {
      this.createTurno();
    }
  }

  private createTurno(): void {
    this.loading.set(true);
    this.turnoService.crear().subscribe({
      next: (turno) => {
        this.setStoredId(turno.id);
        this.turno.set(turno);
        this.updatePosition(turno);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.showError(err);
      },
    });
  }

  private loadTurno(id: number): void {
    this.loading.set(true);
    this.turnoService.buscarPorId(id).subscribe({
      next: (turno) => {
        this.turno.set(turno);
        this.updatePosition(turno);
      },
      error: (err: HttpErrorResponse) => {
        this.clearStoredId();
        this.loading.set(false);
        if (err.status === 404) {
          this.createTurno();
        } else {
          this.showError(err);
        }
      },
    });
  }

  private updatePosition(turno: Turno): void {
    this.turnoService.filtrarPorEstado('pendiente').subscribe({
      next: (pendientes) => {
        const ordered = [...pendientes].sort((a, b) =>
          a.horaSolicitud.localeCompare(b.horaSolicitud),
        );
        const idx = ordered.findIndex((t) => t.id === turno.id);
        if (idx >= 0) {
          this.position.set(idx + 1);
          this.peopleAhead.set(idx);
        } else {
          this.position.set(0);
          this.peopleAhead.set(0);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  private getStoredId(): number | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const parsed = parseInt(raw, 10);
      return Number.isFinite(parsed) ? parsed : null;
    } catch {
      return null;
    }
  }

  private setStoredId(id: number): void {
    try {
      localStorage.setItem(STORAGE_KEY, String(id));
    } catch {
      /* ignore */
    }
  }

  private clearStoredId(): void {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* ignore */
    }
  }

  private showError(err: HttpErrorResponse): void {
    const msg =
      typeof err.error === 'string' && err.error
        ? err.error
        : err.message || 'No se pudo contactar al servidor.';
    this.errorMessage.set(msg);
    setTimeout(() => this.errorMessage.set(null), 4000);
  }
}
