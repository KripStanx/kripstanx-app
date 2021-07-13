import { Component, Injectable } from '@angular/core';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { filter } from 'rxjs/operators';

import { InProgressModalComponent } from './in-progress-modal.component';
import { GuardsCheckEnd, Router } from '@angular/router';
import { GeneralDialogComponent } from 'app/shared/dialog/general-dialog.component';
import { GeneralErrorDialogComponent } from './general-error-dialog.component';

@Injectable({ providedIn: 'root' })
export class KripstanxDialogService {
  private inProgressDialog;
  private inProgressTimeout: any;
  private activeModal: NgbModalRef = null;
  private currentScreenBaseUrl = '';

  constructor(private router: Router, private modalService: NgbModal) {
    router.events.pipe(filter(e => e instanceof GuardsCheckEnd)).subscribe((event: any) => {
      if (event.shouldActivate) {
        // navigation is allowed
        const baseUrl = this.currentScreenBaseUrl;
        const baseUrlAfterRedirect = this.takeWhile(
          char => !'()/?&'.includes(char),
          event.urlAfterRedirects.substr(1) // '1' = remove the starting '/'
        );
        // if we navigate to a different screen, close any open dialog window
        if (this.activeModal !== null && String(baseUrl) !== String(baseUrlAfterRedirect)) {
          try {
            this.activeModal.dismiss('navigation');
          } catch (e) {
            // ignore, it might be an already removed dialog
          }
          this.activeModal = null;
        }
        this.currentScreenBaseUrl = this.takeWhile(
          char => !'()/?&'.includes(char),
          event.urlAfterRedirects.substr(1) // '1' = remove the starting '/'
        );
      }
    });
  }

  takeWhile(fn, arr) {
    const [x, ...xs] = arr;

    if (arr.length > 0 && fn(x)) {
      return [x, ...this.takeWhile(fn, xs)];
    } else {
      return [];
    }
  }

  showGeneralErrorDialog(errorMsg) {
    let dialogComponent = null;
    if (errorMsg) {
      dialogComponent = GeneralErrorDialogComponent;
    }
    this.activeModal = this.modalService.open(dialogComponent as Component, {
      size: 'lg',
      backdrop: 'static',
    });
    this.activeModal.componentInstance.errorMsg = errorMsg;
    return this.activeModal;
  }

  showGeneralDialog(title: string, body: string[], button: string) {
    const dialogComponent = GeneralDialogComponent;

    this.activeModal = this.modalService.open(dialogComponent as Component, {
      size: 'lg',
      backdrop: 'static',
    });
    this.activeModal.componentInstance.title = title;
    this.activeModal.componentInstance.body = body;
    this.activeModal.componentInstance.button = button;
    return this.activeModal;
  }

  showInProgressModal() {
    this.inProgressTimeout = setTimeout(() => {
      this.inProgressDialog = this.modalService.open(InProgressModalComponent as Component, {
        size: 'lg',
        backdrop: 'static',
      });
    }, 500);
  }

  hideInProgressModal() {
    if (this.inProgressTimeout) {
      clearTimeout(this.inProgressTimeout);
      this.inProgressTimeout = undefined;
    }
    if (this.inProgressDialog) {
      try {
        if (this.inProgressDialog.componentInstance) {
          this.inProgressDialog.componentInstance.stopPolling();
        }
      } catch (e) {
        // 'this.inProgressDialog.componentInstance' can throw an exception, ignore it
      }
      this.inProgressDialog.dismiss();
    }
  }
}
