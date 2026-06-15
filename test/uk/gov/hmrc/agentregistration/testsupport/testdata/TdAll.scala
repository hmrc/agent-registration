/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.testdata.TestOnlyData

import java.time.Instant

object TdAll:

  def apply(): TdAll = new TdAll {}

  val tdAll: TdAll = new TdAll {}

  /** A `TdAll` with every domain identifier suffixed by `seed`. Allows priming a single Mongo DB with multiple scenarios that would otherwise collide on unique
    * indexes — each scenario calls `TdAll.make(this.toString)` (or similar) and downstream fixtures (`agentApplicationLlp.afterX`, `providedDetails.afterX`,
    * `applicationScheduler.notProcessed`) automatically pick up the seeded identifiers via the dependencies cake.
    */
  def make(seed: String): TdAll =
    new TdAll:
      override def agentApplicationId: AgentApplicationId = AgentApplicationId(s"agent-application-id-12345-$seed")
      override def internalUserId: InternalUserId = InternalUserId(s"internal-user-id-12345-$seed")
      override def linkId: LinkId = LinkId(s"link-id-12345-$seed")
      override def applicationReference: ApplicationReference = ApplicationReference(s"APPREF123-$seed")
      override def personReference: PersonReference = PersonReference(s"1234567890-$seed")
      override def individualProvidedDetailsId: IndividualProvidedDetailsId = IndividualProvidedDetailsId(s"individual-provided-details-id-12345-$seed")

  /** A `TdAll` whose `agentApplicationId` matches a parent application but whose individual-level identifiers are uniquified by `individualSeed`. Used to
    * derive distinct `IndividualProvidedDetails` rows that all link back to the same parent `AgentApplication` — eliminates manual `.copy(_id = …,
    * personReference = …, internalUserId = …)` patching at the call site.
    */
  def makeForIndividual(
    parentSeed: String,
    individualSeed: String
  ): TdAll =
    new TdAll:
      override def agentApplicationId: AgentApplicationId = AgentApplicationId(s"agent-application-id-12345-$parentSeed")
      override def internalUserId: InternalUserId = InternalUserId(s"internal-user-id-12345-$parentSeed-$individualSeed")
      override def personReference: PersonReference = PersonReference(s"1234567890-$parentSeed-$individualSeed")
      override def individualProvidedDetailsId: IndividualProvidedDetailsId = IndividualProvidedDetailsId(
        s"individual-provided-details-id-12345-$parentSeed-$individualSeed"
      )

/** TestData (Td), All instances. Extends shared TestOnlyData (source of truth) plus agent-registration-specific traits.
  */
trait TdAll
extends TestOnlyData
with TdRequest
with TdEmail
with TdApplicationScheduler:

  // Backward-compatible aliases for properties renamed/removed in shared
  lazy val instant: Instant = nowAsInstant
  lazy val utr: Utr = Utr(saUtr.value)
  lazy val email: String = "test@example.com"
