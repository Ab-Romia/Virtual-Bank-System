// Minimal className joiner. No dependency needed for the small set of conditional
// classes this app uses.
export function cn(...parts: Array<string | false | null | undefined>): string {
  return parts.filter(Boolean).join(' ');
}
