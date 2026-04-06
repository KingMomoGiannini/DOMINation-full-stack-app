/**
 * Subconjunto estable del JSON de org.springframework.data.domain.Page (Spring Boot).
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty?: boolean;
  numberOfElements?: number;
}
