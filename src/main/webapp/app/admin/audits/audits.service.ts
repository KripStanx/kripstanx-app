import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { createRequestOption } from 'app/core/util/request-util';
import { SERVER_API_URL } from 'app/app.constants';
import { Audit } from './audit.model';
import { AccountService } from 'app/core/auth/account.service';

@Injectable({ providedIn: 'root' })
export class AuditsService {
  private lastSessionInfo: any;

  constructor(private http: HttpClient, private accountService: AccountService) {}

  query(req: any): Observable<HttpResponse<Audit[]>> {
    const params: HttpParams = createRequestOption(req);
    params.set('fromDate', req.fromDate);
    params.set('toDate', req.toDate);

    const requestURL = SERVER_API_URL + 'management/audits';

    return this.http.get<Audit[]>(requestURL, {
      params,
      observe: 'response',
    });
  }

  queryForCurrentUer(login: string): Observable<HttpResponse<Audit[]>> {
    const params: HttpParams = createRequestOption({});

    const requestURL = SERVER_API_URL + '/api/audits/' + login;

    return this.http.get<Audit[]>(requestURL, {
      params,
      observe: 'response',
    });
  }

  getLastSessionInfo() {
    if (!this.lastSessionInfo) {
      // first call
      this.lastSessionInfo = {};
      this.accountService
        .identity()
        .toPromise()
        .then(account => {
          this.queryForCurrentUer(account.login).subscribe(res => {
            res.body.shift(); // first element is the current login attempt, should be ignored
            this.lastSessionInfo = this.createLastSessionInfo(res.body);
          });
        });
      return this.lastSessionInfo;
    } else {
      return this.lastSessionInfo;
    }
  }

  createLastSessionInfo(sessionInfos) {
    const lastSuccessfulLogin = sessionInfos.find(sessionInfo => sessionInfo.type === 'AUTHENTICATION_SUCCESS');
    const lastSessionInfo = {};
    if (lastSuccessfulLogin) {
      lastSessionInfo['remoteAddress'] = lastSuccessfulLogin.data.remoteAddress;
      lastSessionInfo['time'] = new Date(lastSuccessfulLogin.timestamp);
      lastSessionInfo['amountOfFailedLoginsSinceLastSuccessfulAttempt'] = sessionInfos.indexOf(lastSuccessfulLogin);
    }
    return lastSessionInfo;
  }

  clearLastSessionInfo() {
    this.lastSessionInfo = null;
  }
}
