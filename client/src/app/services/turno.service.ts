import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Turno {
  id: number;
  numero: number;
  horaSolicitud: string;
  horaAtencion: string | null;
  estado: 'pendiente' | 'atendido' | 'cancelado';
}

@Injectable({ providedIn: 'root' })
export class TurnoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/turnos';

  listarTodos(): Observable<Turno[]> {
    return this.http.get<Turno[]>(this.apiUrl);
  }

  crear(): Observable<Turno> {
    return this.http.post<Turno>(this.apiUrl, null);
  }

  siguiente(): Observable<Turno> {
    return this.http.get<Turno>(`${this.apiUrl}/siguiente`);
  }

  atender(id: number): Observable<Turno> {
    return this.http.put<Turno>(`${this.apiUrl}/${id}/atender`, null);
  }

  cancelar(id: number): Observable<Turno> {
    return this.http.put<Turno>(`${this.apiUrl}/${id}/cancelar`, null);
  }

  promedioEspera(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/espera-promedio`);
  }

  contarPendientes(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/pendientes/count`);
  }
}
