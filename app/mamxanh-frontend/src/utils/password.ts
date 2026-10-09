/**
 * FR-03 password policy, mirroring the backend `@ValidPassword` rule: 8–64 characters, at most
 * 72 UTF-8 bytes, at least one uppercase letter, one lowercase letter and one digit.
 * Returns one message per unmet criterion; the backend remains the authority.
 */
export function passwordProblems(password: string): string[] {
  if (!password) return ['Vui lòng nhập mật khẩu.'];
  const problems: string[] = [];
  const length = [...password].length;
  if (length < 8) problems.push('Mật khẩu cần ít nhất 8 ký tự.');
  if (length > 64) problems.push('Mật khẩu tối đa 64 ký tự.');
  else if (new TextEncoder().encode(password).length > 72) {
    problems.push('Mật khẩu vượt quá 72 byte, ký tự có dấu chiếm nhiều byte hơn, hãy rút ngắn mật khẩu.');
  }
  if (!/\p{Lu}/u.test(password)) problems.push('Mật khẩu cần ít nhất 1 chữ in hoa.');
  if (!/\p{Ll}/u.test(password)) problems.push('Mật khẩu cần ít nhất 1 chữ thường.');
  if (!/[0-9]/.test(password)) problems.push('Mật khẩu cần ít nhất 1 chữ số.');
  return problems;
}
