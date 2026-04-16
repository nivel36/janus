import { AsyncResult } from './async.type';

export type SearchMethod<T> = (query: string) => AsyncResult<T[]>;
