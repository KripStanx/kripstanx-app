import { Component, OnDestroy, OnInit } from '@angular/core';

import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { AuthServerProvider } from 'app/core/auth/auth-jwt.service';

@Component({
  selector: 'jhi-in-progress-modal',
  templateUrl: './in-progress-modal.component.html',
  styleUrls: ['./in-progress-modal.component.scss'],
})
export class InProgressModalComponent implements OnInit, OnDestroy {
  public pollTimerId = null;
  public pollingFrequency = 30000; // 30 sec

  constructor(public activeModal: NgbActiveModal, public authServerProvider: AuthServerProvider) {}

  ngOnInit(): void {
    this.startPolling();
  }

  public stopPolling() {
    clearTimeout(this.pollTimerId);
  }

  public startPolling() {
    clearTimeout(this.pollTimerId);
    this.pollTimerId = setTimeout(() => {
      this.polling();
    }, this.pollingFrequency);
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  private polling() {
    this.authServerProvider.keepAliveSession().subscribe(
      res => {
        this.pollTimerId = setTimeout(() => {
          this.polling();
        }, this.pollingFrequency);
      },
      err => {
        console.log(err);
        this.stopPolling();
      }
    );
  }
}
