<<<<<<< HEAD
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type EstadoTurno = 'pendiente' | 'atendido' | 'cancelado';

=======
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

>>>>>>> 4f766af0a4897589f49c4c2c9154d108840e418b
export interface Turno {
  id: number;
  numero: number;
  horaSolicitud: string;
  horaAtencion: string | null;
<<<<<<< HEAD
  estado: EstadoTurno;
=======
  estado: 'pendiente' | 'atendido' | 'cancelado';
>>>>>>> 4f766af0a4897589f49c4c2c9154d108840e418b
}

@Injectable({ providedIn: 'root' })
export class TurnoService {
  private readonly http = inject(HttpClient);
<<<<<<< HEAD
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
=======
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
>>>>>>> 4f766af0a4897589f49c4c2c9154d108840e418b
  }
}
