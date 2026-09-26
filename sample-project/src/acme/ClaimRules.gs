package acme

uses java.math.BigDecimal

/**
 * Sample file for the plugin: each function below triggers one rule.
 */
class ClaimRules {

  var _threshold : BigDecimal = 10000
  var _legacyLimit : BigDecimal   // UnusedPrivateField

  function isLarge(amount : BigDecimal) : boolean {
    return amount > _threshold
  }

  // UnusedPrivateFunction
  private function oldCheck(amount : BigDecimal) : boolean {
    return amount > 5000
  }

  // EmptyFunction
  function onReopen() {
  }

  // CollapsibleIf and EmptyBlock
  function review(amount : BigDecimal, fraud : boolean) {
    if (amount != null) {
      if (fraud) {
        escalate(amount)
      }
    }
    if (isLarge(amount)) {
    }
  }

  // SwitchWithoutDefault and UnusedLocalVariable
  function route(state : String) : String {
    var unusedNote = "routing"
    switch (state) {
      case "open":
        return "adjuster"
      case "closed":
        return "archive"
    }
    return "unknown"
  }

  // JumpInFinally
  function load() : int {
    try {
      return compute()
    } finally {
      return 0
    }
  }

  // SelfAssignment and IdenticalOperands
  function copy(limit : BigDecimal) : boolean {
    limit = limit
    return limit == limit
  }

  private function escalate(amount : BigDecimal) {
    _threshold = amount
  }

  private function compute() : int {
    return 42
  }
}
