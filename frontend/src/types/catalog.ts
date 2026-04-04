export interface Branch {
  id: number;
  name: string;
  address: string;
  active: boolean;
  providerId?: number;
}

export interface Item {
  id: number;
  branchId: number;
  name: string;
  type: 'ROOM' | 'INSTRUMENT' | 'ACCESSORY' | 'OTHER';
  rentalMode: 'TIME_EXCLUSIVE' | 'TIME_QUANTITY';
  basePrice: number;
  active: boolean;
  quantityTotal: number;
}
