/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentregistration.services

import com.softwaremill.quicklens.*
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

class RiskingOutcomeIndividualHelperSpec
extends UnitSpec:

  private val td: TdAll = TdAll.tdAll

  /** IPD with all three details as user-provided (Provided). Baseline fixture for the `_10` cases below. */
  private val ipdAllProvided: IndividualProvidedDetails = td.providedDetails.afterFinished

  /** IPD variant where every detail is system-derived (FromCitizensDetails / FromAuth / FromCitizenDetails). Used to prove the fix carries `None` in that
    * situation — the FE fix page must not offer system-derived values for editing.
    */
  private val ipdAllSystemDerived: IndividualProvidedDetails = ipdAllProvided
    .modify(_.individualDateOfBirth).setTo(Some(td.dateOfBirthFromCitizenDetails))
    .modify(_.individualNino).setTo(Some(td.ninoFromAuth))
    .modify(_.individualSaUtr).setTo(Some(td.saUtrFromCitizenDetails))

  /** IPD variant where every detail is user-provided as `NotProvided`. Used to prove the fix carries `Some(NotProvided)` — the FE fix page must render the "not
    * provided" state so the user can fill it in.
    */
  private val ipdAllNotProvided: IndividualProvidedDetails = ipdAllProvided
    .modify(_.individualNino).setTo(Some(IndividualNino.NotProvided))
    .modify(_.individualSaUtr).setTo(Some(IndividualSaUtr.NotProvided))

  private case class TestCase(
    description: String,
    riskingOutcome: RiskingOutcome,
    individualFailures: Seq[IndividualFailure],
    individualProvidedDetails: IndividualProvidedDetails,
    expected: RiskingOutcomeIndividual
  )

  "RiskingOutcomeIndividualHelper.riskingOutcomeIndividual(riskingOutcome, individualFailures, individualProvidedDetails)" - {

    List(
      TestCase(
        description = "Approved riskingOutcome with empty individual failures yields RiskingOutcomeIndividual.Approved",
        riskingOutcome = RiskingOutcome.Approved,
        individualFailures = Seq.empty,
        individualProvidedDetails = ipdAllProvided,
        expected = RiskingOutcomeIndividual.Approved
      ),
      TestCase(
        description = "FailedFixable riskingOutcome with a single fixable individual failure yields FailedFixable with the corresponding fix",
        riskingOutcome = RiskingOutcome.FailedFixable,
        individualFailures = Seq(IndividualFailure._4._1),
        individualProvidedDetails = ipdAllProvided,
        expected = RiskingOutcomeIndividual.FailedFixable(
          fixes = Seq(IndividualFix._4._1(isConfirmed = None)),
          declarationAgreed = false
        )
      ),
      TestCase(
        description = "FailedFixable riskingOutcome with multiple non-_10 fixable individual failures yields FailedFixable with one fix per failure",
        riskingOutcome = RiskingOutcome.FailedFixable,
        individualFailures = Seq(
          IndividualFailure._4._1,
          IndividualFailure._5._3,
          IndividualFailure._8._7
        ),
        individualProvidedDetails = ipdAllProvided,
        expected = RiskingOutcomeIndividual.FailedFixable(
          fixes = Seq(
            IndividualFix._4._1(isConfirmed = None),
            IndividualFix._5._3(isConfirmed = None),
            IndividualFix._8._7(isConfirmed = None)
          ),
          declarationAgreed = false
        )
      ),
      TestCase(
        description =
          "FailedFixable _10 individual failure pre-populates IndividualDetailsFix with the individual's user-provided DoB, NINO and SAUTR (Provided variants)",
        riskingOutcome = RiskingOutcome.FailedFixable,
        individualFailures = Seq(IndividualFailure._10._1),
        individualProvidedDetails = ipdAllProvided,
        expected = RiskingOutcomeIndividual.FailedFixable(
          fixes = Seq(
            IndividualFix._10.IndividualDetailsFix(
              dateOfBirth = Some(td.dateOfBirthProvided),
              saUtr = Some(td.saUtrProvided),
              nino = Some(td.ninoProvided),
              isConfirmed = None
            )
          ),
          declarationAgreed = false
        )
      ),
      TestCase(
        description =
          "FailedFixable _10 individual failure with system-derived details (FromCitizensDetails DoB, FromAuth NINO, FromCitizenDetails SAUTR) sets the corresponding fix fields to None so the FE fix pages do not offer them for editing",
        riskingOutcome = RiskingOutcome.FailedFixable,
        individualFailures = Seq(IndividualFailure._10._1),
        individualProvidedDetails = ipdAllSystemDerived,
        expected = RiskingOutcomeIndividual.FailedFixable(
          fixes = Seq(
            IndividualFix._10.IndividualDetailsFix(
              dateOfBirth = None,
              saUtr = None,
              nino = None,
              isConfirmed = None
            )
          ),
          declarationAgreed = false
        )
      ),
      TestCase(
        description =
          "FailedFixable _10 individual failure with user-provided NotProvided values for NINO and SAUTR carries them into the fix as Some(NotProvided) so the FE can render the not-provided state for the user to complete",
        riskingOutcome = RiskingOutcome.FailedFixable,
        individualFailures = Seq(IndividualFailure._10._1),
        individualProvidedDetails = ipdAllNotProvided,
        expected = RiskingOutcomeIndividual.FailedFixable(
          fixes = Seq(
            IndividualFix._10.IndividualDetailsFix(
              dateOfBirth = Some(td.dateOfBirthProvided),
              saUtr = Some(IndividualSaUtr.NotProvided),
              nino = Some(IndividualNino.NotProvided),
              isConfirmed = None
            )
          ),
          declarationAgreed = false
        )
      ),
      TestCase(
        description = "FailedFixable riskingOutcome with both _10 individual failures collapses to a single IndividualDetailsFix after distinct",
        riskingOutcome = RiskingOutcome.FailedFixable,
        individualFailures = Seq(IndividualFailure._10._1, IndividualFailure._10._2),
        individualProvidedDetails = ipdAllProvided,
        expected = RiskingOutcomeIndividual.FailedFixable(
          fixes = Seq(
            IndividualFix._10.IndividualDetailsFix(
              dateOfBirth = Some(td.dateOfBirthProvided),
              saUtr = Some(td.saUtrProvided),
              nino = Some(td.ninoProvided),
              isConfirmed = None
            )
          ),
          declarationAgreed = false
        )
      ),
      TestCase(
        description = "FailedFixable riskingOutcome with duplicate fixable individual failures yields a single fix after distinct",
        riskingOutcome = RiskingOutcome.FailedFixable,
        individualFailures = Seq(IndividualFailure._5._1, IndividualFailure._5._1),
        individualProvidedDetails = ipdAllProvided,
        expected = RiskingOutcomeIndividual.FailedFixable(
          fixes = Seq(IndividualFix._5._1(isConfirmed = None)),
          declarationAgreed = false
        )
      ),
      TestCase(
        description = "FailedNonFixable riskingOutcome with a single non-fixable individual failure yields FailedNonFixable with the failure",
        riskingOutcome = RiskingOutcome.FailedNonFixable,
        individualFailures = Seq(IndividualFailure._6),
        individualProvidedDetails = ipdAllProvided,
        expected = RiskingOutcomeIndividual.FailedNonFixable(failures = Seq(IndividualFailure._6))
      ),
      TestCase(
        description = "FailedNonFixable riskingOutcome with multiple non-fixable individual failures yields FailedNonFixable with all failures",
        riskingOutcome = RiskingOutcome.FailedNonFixable,
        individualFailures = Seq(IndividualFailure._6, IndividualFailure._9),
        individualProvidedDetails = ipdAllProvided,
        expected = RiskingOutcomeIndividual.FailedNonFixable(failures = Seq(IndividualFailure._6, IndividualFailure._9))
      ),
      TestCase(
        description =
          "FailedNonFixable riskingOutcome with a mix of fixable and non-fixable individual failures yields FailedNonFixable carrying ALL failures in original order",
        riskingOutcome = RiskingOutcome.FailedNonFixable,
        individualFailures = Seq(IndividualFailure._4._1, IndividualFailure._6),
        individualProvidedDetails = ipdAllProvided,
        expected = RiskingOutcomeIndividual.FailedNonFixable(failures = Seq(IndividualFailure._4._1, IndividualFailure._6))
      )
    ).foreach: tc =>
      tc.description in:
        RiskingOutcomeIndividualHelper.riskingOutcomeIndividual(
          tc.riskingOutcome,
          tc.individualFailures,
          tc.individualProvidedDetails
        ) shouldBe tc.expected
  }
