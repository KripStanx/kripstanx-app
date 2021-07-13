import { Component, OnInit, Renderer2, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { JhiEventManager } from 'ng-jhipster';
import { AuditsService } from 'app/admin/audits/audits.service';
import { KripstanxDialogService } from 'app/shared/dialog/kripstanx-dialog.service';
import { LoginService } from './login.service';
import { AccountService } from 'app/core/auth/account.service';

declare const particlesJS: any;

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.scss'],
})
export class LoginComponent implements OnInit {
  username: any;
  password: any;

  authenticationError: boolean;
  credentials: any;
  errorMsg1: string;
  errorMsg2: string;
  isLogin: boolean;
  capsLockIsOn = false;
  @ViewChild('editForm') editForm: HTMLFormElement;

  constructor(
    private eventManager: JhiEventManager,
    private loginService: LoginService,
    private router: Router,
    private accountService: AccountService,
    private auditsService: AuditsService,
    private renderer: Renderer2,
    private route: ActivatedRoute,
    private kripstanxDialogService: KripstanxDialogService
  ) {
    this.credentials = {};
  }

  ngOnInit() {
    this.isLogin = false;
    this.renderer.selectRootElement('#username').focus();
    if (this.accountService.isAuthenticated()) {
      this.router.navigate(['']);
    }
    this.route.queryParams.subscribe(params => {
      if (params['passwordResetKeyError']) {
        const dialog = this.kripstanxDialogService.showGeneralErrorDialog('The password link is expired');
        dialog.result.then(result => {
          this.router.navigate(['/login']);
        });
      }
    });
  }

  cancel() {
    this.isLogin = false;
    this.credentials = {
      username: null,
      password: null,
    };
    this.authenticationError = false;
  }

  onkeyDown(event) {
    if (this.capsLockIsOn && event.key === 'CapsLock') {
      this.capsLockIsOn = false;
    } else {
      this.capsLockIsOn = event.getModifierState && event.getModifierState('CapsLock');
    }
  }

  login() {
    if (!this.isLogin) {
      this.isLogin = true;
      this.loginService
        .login({
          username: this.username,
          password: this.password,
        })
        .then(() => {
          this.editForm.reset();
          this.isLogin = false;
          this.auditsService.clearLastSessionInfo();
          this.authenticationError = false;
          if (this.router.url === '/register' || /^\/activate\//.test(this.router.url) || /^\/reset\//.test(this.router.url)) {
            this.router.navigate(['']);
          }

          this.eventManager.broadcast({
            name: 'authenticationSuccess',
            content: 'Sending Authentication Success',
          });

          if (this.router.url === '/login') {
            this.router.navigate(['']);
          }
        })
        .catch(resp => {
          this.isLogin = false;
          this.auditsService.clearLastSessionInfo();
          if (resp.status === 409) {
            // 409 is HTTP status code for 'Conflict', in our case it means password is expired
            const resetScreenUrl = resp.headers.get('Location');
            const resetKey = resp.headers.get('kripstanx-reset-key');
            this.router.navigate([resetScreenUrl], { queryParams: { key: resetKey } });
          } else if (resp.status === 412) {
            this.errorMsg1 = resp.headers.get('message');
            this.errorMsg2 = 'Please contact your Administrator.';
            this.authenticationError = true;
            this.onOpenModal();
          } else if (resp.status === 423) {
            this.errorMsg1 = resp.headers.get('message');
            this.errorMsg2 = 'Please try to login later.';
            this.authenticationError = true;
            this.onOpenModal();
          } else {
            this.errorMsg1 = 'Your user credentials are incorrect.';
            this.errorMsg2 = 'Please review your username and password.';
            this.authenticationError = true;
            this.onOpenModal();
          }
        });
    }
  }

  onOpenModal() {
    const container = document.getElementById('particles-js');
    const button = document.createElement('button');
    button.type = 'button';
    button.style.display = 'none';
    button.setAttribute('data-bs-toggle', 'modal');
    button.setAttribute('data-bs-target', '#errorModal');

    container?.appendChild(button);
    button.click();
  }

  ngAfterViewInit() {
    particlesJS(
      'particles-js',

      {
        particles: {
          number: {
            value: 80,
            density: {
              enable: true,
              value_area: 800,
            },
          },
          color: {
            value: '#ffffff',
          },
          shape: {
            type: 'circle',
            stroke: {
              width: 0,
              color: '#000000',
            },
            polygon: {
              nb_sides: 5,
            },
            image: {
              src: 'img/github.svg',
              width: 100,
              height: 100,
            },
          },
          opacity: {
            value: 0.5,
            random: false,
            anim: {
              enable: false,
              speed: 1,
              opacity_min: 0.1,
              sync: false,
            },
          },
          size: {
            value: 3,
            random: true,
            anim: {
              enable: false,
              speed: 40,
              size_min: 0.1,
              sync: false,
            },
          },
          line_linked: {
            enable: false,
            distance: 150,
            color: '#ffffff',
            opacity: 0.4,
            width: 1,
          },
          move: {
            enable: true,
            speed: 6,
            direction: 'bottom',
            random: false,
            straight: false,
            out_mode: 'out',
            attract: {
              enable: false,
              rotateX: 600,
              rotateY: 1200,
            },
          },
        },
        interactivity: {
          detect_on: 'canvas',
          events: {
            onhover: {
              enable: false,
              mode: 'repulse',
            },
            onclick: {
              enable: false,
              mode: 'push',
            },
            resize: true,
          },
          modes: {
            grab: {
              distance: 400,
              line_linked: {
                opacity: 1,
              },
            },
            bubble: {
              distance: 400,
              size: 40,
              duration: 2,
              opacity: 8,
              speed: 3,
            },
            repulse: {
              distance: 200,
            },
            push: {
              particles_nb: 4,
            },
            remove: {
              particles_nb: 2,
            },
          },
        },
        retina_detect: true,
        config_demo: {
          hide_card: false,
          background_color: '#b61924',
          background_image: '',
          background_position: '50% 50%',
          background_repeat: 'no-repeat',
          background_size: 'cover',
        },
      }
    );
  }

  goToRegister() {
    this.router.navigate(['register']);
  }
}
