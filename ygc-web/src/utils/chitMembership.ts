export type MembershipStatusKey = 'PENDING' | 'ACTIVE' | 'SETTLED' | 'EXITED';

/** API returns string keys; support legacy numeric keys too. */
export function getMyChitStatus(
  map: Record<string, string> | Record<number, string> | undefined,
  chitId: number,
): string | undefined {
  if (!map) return undefined;
  return map[String(chitId) as keyof typeof map] ?? map[chitId as keyof typeof map];
}

export function canJoinChit(status: string | undefined): boolean {
  return !status || status === 'EXITED';
}

export function membershipStatusLabel(status: string): string {
  switch (status) {
    case 'ACTIVE':
      return 'Active Member';
    case 'PENDING':
      return 'Request Pending';
    case 'SETTLED':
      return 'Already Settled';
    case 'EXITED':
      return 'Previously Exited';
    default:
      return status;
  }
}

export function membershipStatusBadgeClass(status: string): string {
  switch (status) {
    case 'ACTIVE':
      return 'bg-success';
    case 'PENDING':
      return 'bg-warning text-dark';
    case 'SETTLED':
      return 'bg-info';
    case 'EXITED':
      return 'bg-secondary';
    default:
      return 'bg-primary';
  }
}
