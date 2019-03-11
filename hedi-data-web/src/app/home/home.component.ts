import { Component, OnInit } from '@angular/core';
import { RepositoryService } from '../repository.service';
import { Therapist } from '../therapist';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  therapists: Therapist[] = [];

  constructor(private repository: RepositoryService) { }

  ngOnInit() {
    this.repository.therapists().subscribe( therapists => this.therapists = therapists );
  }

}
