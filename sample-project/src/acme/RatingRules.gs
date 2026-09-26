package acme

uses java.math.BigDecimal
uses java.text.SimpleDateFormat

/**
 * Sample file for the plugin: each function below triggers one of the rules added in the second batch.
 */
class RatingRules {

  // HardcodedCredential
  var _dbPassword = "Summer2024!"

  // HardcodedSecret
  var _ratingApiKey = "k3J9x7Qm2pL8vT4nR6wZ"

  function connect() : String {
    return _dbPassword + _ratingApiKey
  }

  // BigDecimalFromDouble
  function taxRate() : BigDecimal {
    return new BigDecimal(0.07)
  }

  // WeekYearInDatePattern
  function formatDate(date : java.util.Date) : String {
    return new SimpleDateFormat("YYYY-MM-dd").format(date)
  }

  // ExceptionNotThrown
  function validate(amount : BigDecimal) {
    if (amount == null) {
      new IllegalArgumentException("amount is required")
    }
  }

  // IndexOfPositive
  function isAuto(policyNumber : String) : boolean {
    return policyNumber.indexOf("AUTO") > 0
  }

  // DuplicateCondition
  function tier(amount : int) : String {
    if (amount > 100000) {
      return "senior"
    } else if (amount > 10000) {
      return "standard"
    } else if (amount > 100000) {
      return "escalated"
    }
    return "junior"
  }

  // DuplicateBranch
  function route(status : String) {
    if (status == "draft") {
      validate(1bd)
      save()
    } else if (status == "quoted") {
      validate(1bd)
      save()
    } else {
      archive()
    }
  }

  // AllBranchesIdentical
  function fee(expedited : boolean) : BigDecimal {
    return expedited ? 50bd : 50bd
  }

  // CognitiveComplexity (19)
  function surcharge(drivers : List<Integer>, claims : List<Integer>) : int {
    var total = 0
    if (drivers != null and claims != null) {
      for (age in drivers) {
        if (age < 25) {
          for (count in claims) {
            if (count > 0 and age < 21) {
              total += 3
            } else if (count > 0) {
              total += 2
            } else {
              total += 1
            }
          }
        }
      }
    }
    return total
  }

  var _calls = 0

  private function save() {
    _calls++
  }

  private function archive() {
    _calls--
  }
}
