import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { JhiParseLinks, JhiAlertService } from 'ng-jhipster';

import { BACKEND_DATE_FORMAT, ITEMS_PER_PAGE } from 'app/shared';
import { Audit } from './audit.model';
import { AuditsService } from './audits.service';
import { Moment } from 'moment';
import * as moment from 'moment';

@Component({
  selector: 'jhi-audit',
  templateUrl: './audits.component.html',
})
export class AuditsComponent implements OnInit, OnDestroy {
  audits: Audit[];
  itemsPerPage: any;
  links: any;
  queryCount: number;
  page: number;
  routeData: any;
  previousPage: any;
  reverse: boolean;
  totalItems: number;
  fromDateMoment: Moment;
  toDateMoment: Moment;

  constructor(
    private auditsService: AuditsService,
    private alertService: JhiAlertService,
    private parseLinks: JhiParseLinks,
    private activatedRoute: ActivatedRoute,
    private datePipe: DatePipe,
    private router: Router
  ) {
    this.itemsPerPage = ITEMS_PER_PAGE;
    this.routeData = this.activatedRoute.data.subscribe(data => {
      this.page = data['pagingParams'].page;
      this.previousPage = data['pagingParams'].page;
      this.reverse = data['pagingParams'].ascending;
    });
  }

  ngOnInit() {
    this.fromDateMoment = moment().subtract(1, 'months').hour(0).minute(0).second(0);
    this.toDateMoment = moment().hour(23).minute(59).second(59);
    this.loadAll();
  }

  ngOnDestroy() {
    this.routeData.unsubscribe();
  }

  loadAll() {
    this.auditsService
      .query({
        page: this.page - 1,
        size: this.itemsPerPage,
        sort: ['auditEventDate'],
        fromDate: this.fromDateMoment.format(BACKEND_DATE_FORMAT),
        toDate: this.toDateMoment.format(BACKEND_DATE_FORMAT),
      })
      .subscribe(
        (res: HttpResponse<Audit[]>) => this.onSuccess(res.body, res.headers),
        (res: HttpResponse<any>) => this.onError(res.body)
      );
  }

  loadPage(page: number) {
    if (page !== this.previousPage) {
      this.previousPage = page;
      this.transition();
    }
  }

  transition() {
    this.router.navigate(['/admin/audits'], {
      queryParams: {
        page: this.page,
        sort: 'auditEventDate,asc',
      },
    });
    this.loadAll();
  }

  private onSuccess(data, headers) {
    this.links = this.parseLinks.parse(headers.get('link'));
    this.totalItems = headers.get('X-Total-Count');
    this.queryCount = this.totalItems;
    this.audits = data;
  }

  private onError(error) {
    this.alertService.error(error.error, error.message, null);
  }
}
