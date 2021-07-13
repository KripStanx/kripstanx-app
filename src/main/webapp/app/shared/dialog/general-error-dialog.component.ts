import { Component } from '@angular/core';

import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'jhi-general-error-dialog',
  templateUrl: './general-error-dialog.component.html',
})
export class GeneralErrorDialogComponent {
  errorMsg: string;

  constructor(public activeModal: NgbActiveModal) {}

  clear() {
    this.activeModal.close('ok');
  }
}
