export function formatDateTime(t: string | null | undefined): string {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}
