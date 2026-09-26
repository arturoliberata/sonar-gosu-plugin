package org.sonargosu.plugin.checks;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Flags passwords written in the code: a name ending in password/passwd/pwd/passphrase given a
 * string literal, a "password=..." parameter inside a string (connection strings), and
 * "user:password@" in URLs.
 */
public class HardcodedCredentialCheck extends HardcodedValueCheck {

  private static final List<String> WORDS = List.of("password", "passwd", "pwd", "passphrase", "passcode");

  private static final Pattern PASSWORD_PARAMETER = Pattern.compile("(?i)(?:password|passwd|pwd)\\s*=\\s*([^;&\\s'\"]+)");
  private static final Pattern URL_USER_INFO = Pattern.compile("://[^/\\s:@]+:([^/\\s@]+)@");

  @Override
  protected boolean isSensitiveName(String normalizedName) {
    return WORDS.stream().anyMatch(normalizedName::endsWith);
  }

  @Override
  protected boolean isSensitiveValue(String normalizedName, String value) {
    String normalizedValue = normalize(value);
    return !value.isBlank()
      && !value.matches(".*\\s.*")                        // a label or message: "Enter password"
      && !value.matches("[*xX]+")                         // a mask: "********"
      && !isSensitiveName(normalizedValue)                // a key or field name: "db.password"
      && !normalizedValue.equals(normalizedName);
  }

  @Override
  protected String message(String name) {
    return "Remove this hard-coded password from \"" + name + "\"; load it from a secure configuration instead.";
  }

  @Override
  protected void visitStringLiteral(TerminalNode literal, String value) {
    if (value.contains("${") || value.contains("<%")) {
      return;
    }
    Matcher parameter = PASSWORD_PARAMETER.matcher(value);
    Matcher url = URL_USER_INFO.matcher(value);
    if ((parameter.find() && isRealValue(parameter.group(1))) || (url.find() && isRealValue(url.group(1)))) {
      report(literal, "Remove this hard-coded password from the string; load it from a secure configuration instead.");
    }
  }

  // Placeholders such as "?", "%s", "{0}" or "*****" are not passwords.
  private static boolean isRealValue(String value) {
    return !value.matches("[?*%{$].*|[xX*]+");
  }
}
