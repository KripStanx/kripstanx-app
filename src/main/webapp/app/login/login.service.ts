import { Injectable } from '@angular/core';
import { AccountService } from 'app/core/auth/account.service';
import { AuthServerProvider } from 'app/core/auth/auth-jwt.service';
import { Login } from './login.model';

import { JhiEventManager } from 'ng-jhipster';

export const enum LogoutReason {
  PASSWORD_RESET = 'PASSWORD_RESET',
  UNAUTHORIZED = 'UNAUTHORIZED',
  LOGOUT_BUTTON = 'LOGOUT_BUTTON',
  SESSION_TIMEOUT = 'SESSION_TIMEOUT',
}

@Injectable({
  providedIn: 'root',
})
export class LoginService {
  constructor(
    private accountService: AccountService,
    private authServerProvider: AuthServerProvider,
    private eventManager: JhiEventManager
  ) {}

  login(credentials: Login, callback?) {
    const cb = callback;

    return new Promise((resolve, reject) => {
      this.authServerProvider.login(credentials).subscribe(
        data => {
          this.accountService
            .identity(true)
            .toPromise()
            .then(account => {
              resolve(data);
            });
          return cb();
        },
        err => {
          this.logout(LogoutReason.UNAUTHORIZED);
          reject(err);
          return cb(err);
        }
      );
    });
  }

  logout(logoutReason: LogoutReason) {
    if (this.accountService.isAuthenticated()) {
      console.log('logout reason:' + logoutReason);
      this.eventManager.broadcast({
        name: 'logout',
        content: {
          reason: logoutReason,
        },
      });

      switch (logoutReason) {
        case LogoutReason.LOGOUT_BUTTON:
        case LogoutReason.SESSION_TIMEOUT:
        case LogoutReason.PASSWORD_RESET:
          this.authServerProvider.sendLogoutNotification().subscribe();
          break;
      }

      this.authServerProvider.logout().subscribe();
      this.accountService.authenticate(null);
    }
  }
}
