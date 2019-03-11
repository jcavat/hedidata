import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from "rxjs/internal/Observable";
import { Therapist } from "./therapist";

@Injectable({
  providedIn: 'root'
})
export class RepositoryService {

  constructor(private http: HttpClient) { }

  therapists(): Observable<Therapist[]> {
    return this.http.get<Therapist[]>('http://localhost:8080/therapists');
  }
}
