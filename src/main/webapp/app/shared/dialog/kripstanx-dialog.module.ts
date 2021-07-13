import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { KripstanxSharedModule } from '../../shared/shared.module';
import { GeneralDialogComponent } from '../../shared/dialog/general-dialog.component';
import { InProgressModalComponent } from '../../shared/dialog/in-progress-modal.component';
import { GeneralErrorDialogComponent } from './general-error-dialog.component';

@NgModule({
  imports: [FormsModule, KripstanxSharedModule],
  declarations: [InProgressModalComponent, GeneralDialogComponent, GeneralErrorDialogComponent],
  entryComponents: [InProgressModalComponent, GeneralDialogComponent, GeneralErrorDialogComponent],
})
export class KripstanxEntityDialogModule {}
