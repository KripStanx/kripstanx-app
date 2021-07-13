import { Component } from '@angular/core';

import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'jhi-general-dialog',
  templateUrl: './general-dialog.component.html',
})
export class GeneralDialogComponent {
  title: string;
  body: string[];
  button: string;

  constructor(public activeModal: NgbActiveModal) {}

  onButtonClicked() {
    this.activeModal.close(this.button);
  }
}
