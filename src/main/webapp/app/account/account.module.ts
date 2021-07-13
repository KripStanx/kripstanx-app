import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { KripstanxSharedModule } from 'app/shared/shared.module';
import { PasswordStrengthBarComponent } from './password/password-strength-bar/password-strength-bar.component';
import { RegisterComponent } from './register/register.component';
import { ActivateComponent } from './activate/activate.component';
import { PasswordComponent } from './password/password.component';
import { PasswordResetInitComponent } from './password-reset/init/password-reset-init.component';
import { PasswordResetFinishComponent } from './password-reset/finish/password-reset-finish.component';
import { SettingsComponent } from './settings/settings.component';
import { accountState } from './account.route';
import { AngularMaterialModule } from 'app/angular-material.module';
import { MDBBootstrapModule, IconsModule, ButtonsModule, WavesModule, CollapseModule } from 'angular-bootstrap-md';
import { FlexLayoutModule } from '@angular/flex-layout';

@NgModule({
  imports: [
    MDBBootstrapModule.forRoot(),
    IconsModule,
    ButtonsModule,
    WavesModule.forRoot(),
    CollapseModule.forRoot(),
    AngularMaterialModule,
    FlexLayoutModule,
    KripstanxSharedModule,
    RouterModule.forChild(accountState),
  ],
  exports: [AngularMaterialModule],
  declarations: [
    ActivateComponent,
    RegisterComponent,
    PasswordComponent,
    PasswordStrengthBarComponent,
    PasswordResetInitComponent,
    PasswordResetFinishComponent,
    SettingsComponent,
  ],
})
export class AccountModule {}
