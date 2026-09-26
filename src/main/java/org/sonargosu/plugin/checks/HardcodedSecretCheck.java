package org.sonargosu.plugin.checks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Flags API keys, tokens and other secrets written in the code:
 * <ul>
 *   <li>a name ending in secret/token/apikey/accesskey/privatekey... given a long, random-looking
 *   string (so {@code tokenHeader = "Authorization"} is fine);</li>
 *   <li>any string with the well-known shape of a real credential (AWS access key id, GitHub or
 *   Slack token, private key block), whatever its name.</li>
 * </ul>
 */
public class HardcodedSecretCheck extends HardcodedValueCheck {

  private static final List<String> WORDS = List.of(
    "secret", "token", "apikey", "accesskey", "privatekey", "secretkey", "authkey", "signingkey", "clientsecret");

  private static final Pattern KNOWN_SECRET = Pattern.compile(
    "AKIA[0-9A-Z]{16}"                              // AWS access key id
      + "|gh[pousr]_[A-Za-z0-9]{36,}"               // GitHub token
      + "|xox[abprs]-[A-Za-z0-9-]{10,}"             // Slack token
      + "|-----BEGIN [A-Z ]*PRIVATE KEY-----");     // PEM private key

  private static final Pattern DOTTED_KEY = Pattern.compile("[A-Za-z][\\w-]*(\\.[\\w-]+)+");

  @Override
  protected boolean isSensitiveName(String normalizedName) {
    return WORDS.stream().anyMatch(normalizedName::endsWith);
  }

  @Override
  protected boolean isSensitiveValue(String normalizedName, String value) {
    return value.length() >= 10
      && !value.matches(".*\\s.*")
      && !DOTTED_KEY.matcher(value).matches()       // a property key: "auth.token.header"
      && !value.startsWith("http")
      && !KNOWN_SECRET.matcher(value).find()        // reported by visitStringLiteral instead
      && entropy(value) >= 3.0;
  }

  @Override
  protected String message(String name) {
    return "Remove this hard-coded secret from \"" + name + "\"; load it from a secure configuration or vault instead.";
  }

  @Override
  protected void visitStringLiteral(TerminalNode literal, String value) {
    if (KNOWN_SECRET.matcher(value).find()) {
      report(literal, "This string contains what looks like a real access key or token. Revoke it and load it from a vault instead.");
    }
  }

  /** Shannon entropy in bits per character: random keys score high, words and repeated text low. */
  static double entropy(String value) {
    Map<Character, Integer> counts = new HashMap<>();
    for (char c : value.toCharArray()) {
      counts.merge(c, 1, Integer::sum);
    }
    double entropy = 0;
    for (int count : counts.values()) {
      double p = (double) count / value.length();
      entropy -= p * Math.log(p) / Math.log(2);
    }
    return entropy;
  }
}
