import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { KripstanxSharedModule } from 'app/shared/shared.module';

import { HealthComponent } from './health.component';
import { HealthModalComponent } from './modal/health-modal.component';
import { healthRoute } from './health.route';

@NgModule({
  imports: [KripstanxSharedModule, RouterModule.forChild([healthRoute])],
  declarations: [HealthComponent, HealthModalComponent],
  entryComponents: [HealthModalComponent],
})
export class HealthModule {}
