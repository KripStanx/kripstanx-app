import { Component, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { ModalDirective } from 'angular-bootstrap-md';

import { EMAIL_ALREADY_USED_TYPE, LOGIN_ALREADY_USED_TYPE } from 'app/config/error.constants';
import { RegisterService } from './register.service';
import { MainComponent } from 'app/layouts/main/main.component';

@Component({
  selector: 'jhi-register',
  templateUrl: './register.component.html',
})
export class RegisterComponent implements AfterViewInit {
  @ViewChild('successModal', { static: true }) successModal: ModalDirective;
  @ViewChild('username', { static: false })
  username?: ElementRef;

  error = false;
  errorEmailExists = false;
  errorUserExists = false;
  success = false;

  registerForm = this.formBuilder.group({
    firstName: ['', [Validators.required, Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.maxLength(50)]],
    username: [
      '',
      [
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(50),
        Validators.pattern('^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$'),
      ],
    ],
    email: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(254), Validators.email]],
    password: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]],
  });

  constructor(
    private translateService: TranslateService,
    private registerService: RegisterService,
    private router: Router,
    private formBuilder: FormBuilder,
    public spin: MainComponent
  ) {}

  ngAfterViewInit(): void {
    if (this.username) {
      // this.username.nativeElement.focus();
    }
  }

  register(): void {
    this.error = false;
    this.errorEmailExists = false;
    this.errorUserExists = false;

    const firstName = this.registerForm.get(['firstName'])!.value;
    const lastName = this.registerForm.get(['lastName'])!.value;
    const username = this.registerForm.get(['username'])!.value;
    const password = this.registerForm.get(['password'])!.value;
    const email = this.registerForm.get(['email'])!.value;

    this.registerService.save({ firstName, lastName, username, email, password, langKey: 'en' }).subscribe(
      () => (this.success = true),
      response => this.processError(response)
    );
  }

  routeToLogin() {
    this.successModal.hide();
    this.spin.showSpin();

    setTimeout(() => {
      this.router.navigate(['login']);
      this.spin.hideSpin();
    }, 3000);
  }

  private processError(response: HttpErrorResponse): void {
    if (response.status === 400 && response.error.type === LOGIN_ALREADY_USED_TYPE) {
      this.errorUserExists = true;
    } else if (response.status === 400 && response.error.type === EMAIL_ALREADY_USED_TYPE) {
      this.errorEmailExists = true;
    } else {
      this.error = true;
    }
  }
}
