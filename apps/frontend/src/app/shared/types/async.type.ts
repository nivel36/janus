import { Observable } from 'rxjs';

export type AsyncResult<T> = Observable<T> | Promise<T> | T;
