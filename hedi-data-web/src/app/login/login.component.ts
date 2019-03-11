import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  login = new FormControl('');
  password = new FormControl('');

  constructor() { }

  ngOnInit() {
  }
  onSubmit() {
    console.log('coucou');
  }

}
