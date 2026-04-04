import type { Branch } from '../types/catalog';

/**
 * Cruza `branchId` de una reserva con sucursales ya obtenidas del catálogo (mismo gateway).
 * No inventa nombres: si el id no está en el mapa, se indica explícitamente.
 */
export function branchesToMap(branches: Branch[]): Map<number, Branch> {
  return new Map(branches.map((b) => [b.id, b]));
}

export interface BranchDisplay {
  /** Nombre para mostrar */
  primary: string;
  /** Dirección u observación secundaria */
  secondary: string | null;
  /** true si hubo match con el catálogo cargado */
  resolved: boolean;
}

export function resolveBranchDisplay(map: Map<number, Branch>, branchId: number): BranchDisplay {
  const b = map.get(branchId);
  if (b) {
    return { primary: b.name, secondary: b.address || null, resolved: true };
  }
  return {
    primary: `Sucursal no encontrada en catálogo`,
    secondary: `ID ${branchId} (revisá que el catálogo esté cargado o que la sucursal siga existiendo)`,
    resolved: false,
  };
}

/**
 * Prioriza `branchName` del DTO de reserva (Sprint 8); si falta, cruza con el mapa del catálogo.
 */
export function resolveReservationBranchDisplay(
  branchId: number,
  branchNameFromDto: string | null | undefined,
  map: Map<number, Branch>,
  catalogFetched: boolean,
  loadingSecondary: string
): BranchDisplay {
  const trimmed = branchNameFromDto?.trim();
  if (trimmed) {
    return { primary: trimmed, secondary: null, resolved: true };
  }
  if (!catalogFetched) {
    return {
      primary: 'Sucursal',
      secondary: loadingSecondary,
      resolved: false,
    };
  }
  return resolveBranchDisplay(map, branchId);
}
