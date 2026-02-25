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

package uk.gov.hmrc.agentregistration.shared

import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsIncorporated
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsNotIncorporated
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfCompaniesHouseOfficers
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfRequiredKeyIndividuals
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

extension (agentApplication: AgentApplication)

  def hasCheckPassed: Boolean =

    val refusalToDealWithCheckResultPassed: Boolean =
      agentApplication.refusalToDealWithCheckResult === Some(
        CheckResult.Pass
      )

    val deceasedCheckPassed: Boolean =
      agentApplication match
        case a: AgentApplicationSoleTrader => a.deceasedCheckResult === Some(CheckResult.Pass)
        case _ => true // not required so passed

    val companyStatusCheckPassed: Boolean =
      agentApplication match
        case a: IsIncorporated => a.companyStatusCheck === Some(CheckResult.Pass)
        case a: IsNotIncorporated => true // not required so passed

    refusalToDealWithCheckResultPassed
    && deceasedCheckPassed
    && companyStatusCheckPassed

extension (agentApplication: AgentApplication.IsIncorporated)

  def getCompanyProfile: CompanyProfile =
    agentApplication match
      case a: AgentApplicationLimitedCompany => a.getBusinessDetails.companyProfile
      case a: AgentApplicationLimitedPartnership => a.getBusinessDetails.companyProfile
      case a: AgentApplicationLlp => a.getBusinessDetails.companyProfile
      case a: AgentApplicationScottishLimitedPartnership => a.getBusinessDetails.companyProfile

  def companyStatusCheck: Option[CheckResult] =
    agentApplication match
      case a: AgentApplicationLimitedCompany => a.companyStatusCheckResult
      case a: AgentApplicationLlp => a.companyStatusCheckResult
      case a: AgentApplicationLimitedPartnership => a.companyStatusCheckResult
      case a: AgentApplicationScottishLimitedPartnership => a.companyStatusCheckResult

  def getCrn: Crn =
    agentApplication match
      case a: AgentApplicationLimitedCompany => a.getCrn
      case a: AgentApplicationLlp => a.getCrn
      case a: AgentApplicationLimitedPartnership => a.getCrn
      case a: AgentApplicationScottishLimitedPartnership => a.getCrn

  def getNumberOfCompaniesHouseOfficers: Option[NumberOfCompaniesHouseOfficers] =
    agentApplication match
      case a: AgentApplicationLimitedCompany => a.numberOfIndividuals
      case a: AgentApplicationLlp => a.numberOfIndividuals
      case a: AgentApplicationLimitedPartnership => a.numberOfIndividuals
      case a: AgentApplicationScottishLimitedPartnership => a.numberOfIndividuals

extension (agentApplication: AgentApplication.IsAgentApplicationForDeclaringNumberOfKeyIndividuals)

  def getNumberOfRequiredKeyIndividuals: Option[NumberOfRequiredKeyIndividuals] =
    agentApplication match
      case a: AgentApplicationGeneralPartnership => a.numberOfIndividuals
      case a: AgentApplicationScottishPartnership => a.numberOfIndividuals
