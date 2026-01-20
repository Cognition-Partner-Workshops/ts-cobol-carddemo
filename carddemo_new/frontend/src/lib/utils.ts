import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
}

export function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export function formatDateTime(dateString: string): string {
  return new Date(dateString).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function maskCardNumber(cardNumber: string): string {
  if (!cardNumber || cardNumber.length < 4) return cardNumber;
  return `**** **** **** ${cardNumber.slice(-4)}`;
}

export function getStatusColor(status: string): string {
  switch (status.toUpperCase()) {
    case 'Y':
    case 'ACTIVE':
    case 'POSTED':
    case 'PROCESSED':
      return 'text-green-600 bg-green-100';
    case 'N':
    case 'INACTIVE':
    case 'CANCELLED':
      return 'text-red-600 bg-red-100';
    case 'PENDING':
      return 'text-yellow-600 bg-yellow-100';
    default:
      return 'text-gray-600 bg-gray-100';
  }
}

export function getStatusLabel(status: string): string {
  switch (status.toUpperCase()) {
    case 'Y':
      return 'Active';
    case 'N':
      return 'Inactive';
    default:
      return status;
  }
}
