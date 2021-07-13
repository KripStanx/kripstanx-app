import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { KripstanxSharedModule } from 'app/shared/shared.module';
import { loginRoute } from './login.route';
import { LoginComponent } from './login.component';
import { AngularMaterialModule } from 'app/angular-material.module';
import { FlexLayoutModule } from '@angular/flex-layout';

@NgModule({
  imports: [FlexLayoutModule, AngularMaterialModule, KripstanxSharedModule, RouterModule.forChild([loginRoute])],
  exports: [AngularMaterialModule],
  declarations: [LoginComponent],
})
export class LoginModule {}
