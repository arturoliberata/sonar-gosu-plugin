package acme

uses java.util.List
uses java.util.Map
uses java.util.List

/**
 * Sample file used to try the plugin; every rule has at least one issue here.
 */
class PolicyUtil {

  // TODO replace with a real rating engine
  static function rate(a : int, b : int, c : int, d : int, e : int, f : int, g : int, h : int) : int {
    return a + b + c + d + e + f + g + h
  }

  static function safeParse(s : String) : int {
    try {
      return Integer.parseInt(s)
    } catch (ex : NumberFormatException) {
    }
    print("Could not parse " + s + ", falling back to zero because the value provided by the caller was invalid or empty or otherwise unusable")
    return 0
  }
}
