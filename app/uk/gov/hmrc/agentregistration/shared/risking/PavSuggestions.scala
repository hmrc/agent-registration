package uk.gov.hmrc.agentregistration.shared.risking

sealed trait EntityFailure

object EntityFailure:

  sealed trait Fixable
  extends EntityFailure

  sealed trait NonFixable
  extends EntityFailure

  /** Check 3: AMLS */
  object _3:

    /** 3.1: Entity claims AMLS with HMRC but their registration number cannot be found in HMRC's AMLS register */
    object _1
    extends EntityFailure.Fixable

    /** 3.2: Entity claims AMLS with a professional body but their registration number cannot be found in that professional body's AMLS register */
    object _2
    extends EntityFailure.Fixable

    /** 3.3: No proof or evidence of AMLS coverage (file upload) */
    object _3
    extends EntityFailure.Fixable

    /** 3.4: Professional body not on approved list */
    object _4
    extends EntityFailure.Fixable

    /** 3.5: Student membership */
    object _5
    extends EntityFailure.Fixable

  /** Check 4: Overdue returns */
  object _4:

    /** 4.1: One or more overdue SA returns */
    object _1
    extends EntityFailure.Fixable

    /** 4.2: One or more overdue CoTax returns */
    object _2
    extends EntityFailure.Fixable

    /** 4.3: One or more overdue VAT returns */
    object _3
    extends EntityFailure.Fixable

    /** 4.4: One or more overdue PAYE returns */
    object _4
    extends EntityFailure.Fixable

  /** Check 5: Overdue liabilities */
  object _5:

    /** 5.1: One or more overdue SA liabilities */
    case class _1(value: String)
    extends EntityFailure.Fixable

    /** 5.2: One or more overdue CoTax liabilities */
    case class _2(value: String)
    extends EntityFailure.Fixable

    /** 5.3: One or more overdue VAT liabilities */
    case class _3(value: String)
    extends EntityFailure.Fixable

    /** 5.4: One or more overdue PAYE liabilities */
    case class _4(value: String)
    extends EntityFailure.Fixable

    /** 5.5: One or more overdue civil penalties */
    case class _5(value: String)
    extends EntityFailure.Fixable

    /** 5.6: One or more overdue Stamp Duty liabilities */
    case class _6(value: String)
    extends EntityFailure.Fixable

    /** 5.7: One or more overdue Capital Gains Tax liabilities */
    case class _7(value: String)
    extends EntityFailure.Fixable

  /** 7: Insolvent */
  object _7
  extends EntityFailure.NonFixable

  /** Check 8: Anti-avoidance measures or penalties */
  object _8:

    /** 8.1: Measure - Published Tax Avoidance promoters, enablers and suppliers */
    case object _1
    extends EntityFailure.NonFixable

    /** 8.4: POTAS penalty - within 12 months */
    case object _4
    extends EntityFailure.NonFixable

    /** 8.5: POTAS penalty - more than 12 months, not paid */
    case object _5
    extends EntityFailure.Fixable

    /** 8.6: Enablers Penalty - within 12 months */
    case object _6
    extends EntityFailure.NonFixable

    /** 8.7: Enablers Penalty - more than 12 months, not paid */
    case object _7
    extends EntityFailure.Fixable

sealed trait IndividualFailure

object IndividualFailure:

  sealed trait Fixable
  extends IndividualFailure

  sealed trait NonFixable
  extends IndividualFailure

  /** Check 4: Overdue returns */
  object _4:

    /** 4.1: One or more overdue SA returns */
    object _1
    extends IndividualFailure.Fixable

    /** 4.3: One or more overdue VAT returns */
    object _3
    extends IndividualFailure.Fixable

    /** 4.4: One or more overdue PAYE returns */
    object _4
    extends IndividualFailure.Fixable

  /** Check 5: Overdue liabilities */
  object _5:

    /** 5.1: One or more overdue SA liabilities */
    case class _1(value: String)
    extends IndividualFailure.Fixable

    /** 5.3: One or more overdue VAT liabilities */
    case class _3(value: String)
    extends IndividualFailure.Fixable

    /** 5.4: One or more overdue PAYE liabilities */
    case class _4(value: String)
    extends IndividualFailure.Fixable

    /** 5.5: One or more overdue civil penalties */
    case class _5(value: String)
    extends IndividualFailure.Fixable

    /** 5.6: One or more overdue Stamp Duty liabilities */
    case class _6(value: String)
    extends IndividualFailure.Fixable

    /** 5.7: One or more overdue Capital Gains Tax liabilities */
    case class _7(value: String)
    extends IndividualFailure.Fixable

  /** 6: Disqualified as a director on Companies House */
  object _6
  extends IndividualFailure.NonFixable

  /** 7: Insolvent */
  object _7
  extends IndividualFailure.NonFixable

  /** Check 8: Anti-avoidance measures or penalties */
  object _8:

    /** 8.1: Measure - Published Tax Avoidance promoters, enablers and suppliers */
    case object _1
    extends IndividualFailure.NonFixable

    /** 8.6: Enablers Penalty - within 12 months */
    case object _6
    extends IndividualFailure.NonFixable

    /** 8.7: Enablers Penalty - more than 12 months, not paid */
    case object _7
    extends IndividualFailure.Fixable

  /** 9: Relevant criminal convictions */
  object _9
  extends IndividualFailure.NonFixable

  /** Check 10: Cannot verify the individual's information */
  object _10:

    /** 10.1: Unable to match Name and DOB against references */
    object _1
    extends IndividualFailure.Fixable

    /** 10.2: Unable to match Name and DOB against references and Missing SA-UTR */
    object _2
    extends IndividualFailure.Fixable
