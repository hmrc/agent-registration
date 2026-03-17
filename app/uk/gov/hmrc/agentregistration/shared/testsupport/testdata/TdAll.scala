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

package uk.gov.hmrc.agentregistration.shared.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.Crn
import uk.gov.hmrc.agentregistration.shared.testsupport.testdata.agentapplication.*

trait TestOnlyData
extends TdBase,
  TdGrs,
  TdAgentApplicationLlp,
  TdAgentApplicationGeneralPartnership,
  TdAgentApplicationScottishPartnership,
  TdAgentApplicationSoleTrader,
  TdAgentApplicationLimitedCompany,
  TdAgentApplicationLimitedPartnership,
  TdAgentApplicationScottishLimitedPartnership,
  agentapplication.sections.TdSectionContactDetails,
  agentapplication.sections.TdSectionAgentDetails,
  agentapplication.sections.TdUpload,
  agentapplication.sections.TdSectionAmls,
  providedetails.individual.TdIndividualProvidedDetails

object TestOnlyData
extends TestOnlyData:

  object llp:

    val twoChOfficers: TestOnlyData =
      new TestOnlyData:
        val crnTwoChOfficers = Crn("22222222")
        override def crn: Crn = crnTwoChOfficers

    val sixChOfficers: TestOnlyData =
      new TestOnlyData:
        val crnSixChOfficers = Crn("22222226")
        override def crn: Crn = crnSixChOfficers
