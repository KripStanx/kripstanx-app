import { HttpParams } from '@angular/common/http';

export const createRequestOption = (req?: any): HttpParams => {
  let options: HttpParams = new HttpParams();
  if (req) {
    Object.keys(req).forEach(key => {
      if (key === 'sort') {
        if (Array.isArray(req.sort)) {
          req.sort.forEach(val => {
            options = options.append('sort', val);
          });
        } else {
          options = options.append('sort', req.sort);
        }
      } else if (key === 'quick-search') {
        options = options.set(key, encodeURIComponent(req[key]));
      } else {
        options = options.set(key, req[key]);
      }
    });
  }
  return options;
};
