import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type EstadoTurno = 'pendiente' | 'atendido' | 'cancelado';

export interface Turno {
  id: number;
  numero: number;
  horaSolicitud: string;
  horaAtencion: string | null;
  estado: EstadoTurno;
}

@Injectable({ providedIn: 'root' })
export class TurnoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/turnos';

  listarTodos(): Observable<Turno[]> {
    return this.http.get<Turno[]>(this.baseUrl);
  }

  buscarPorId(id: number): Observable<Turno> {
    return this.http.get<Turno>(`${this.baseUrl}/${id}`);
  }

  filtrarPorEstado(estado: EstadoTurno): Observable<Turno[]> {
    return this.http.get<Turno[]>(`${this.baseUrl}/estado/${estado}`);
  }

  siguientePendiente(): Observable<Turno> {
    return this.http.get<Turno>(`${this.baseUrl}/siguiente`);
  }

  esperaPromedio(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/espera-promedio`);
  }

  contarPendientes(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/pendientes/count`);
  }

  crear(): Observable<Turno> {
    return this.http.post<Turno>(this.baseUrl, null);
  }

  atender(id: number): Observable<Turno> {
    return this.http.put<Turno>(`${this.baseUrl}/${id}/atender`, null);
  }

  cancelar(id: number): Observable<Turno> {
    return this.http.put<Turno>(`${this.baseUrl}/${id}/cancelar`, null);
  }
}
