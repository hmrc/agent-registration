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

import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile

extension (agentApplication: AgentApplication)

  def isIncorporated: Boolean =
    agentApplication match
      case _: AgentApplication.IsIncorporated => true
      case _: AgentApplication.IsNotIncorporated => false

extension (agentApplication: AgentApplication.IsIncorporated)

  def getCompanyProfile: CompanyProfile =
    agentApplication match
      case a: AgentApplicationLimitedCompany => a.getBusinessDetails.companyProfile
      case a: AgentApplicationLimitedPartnership => a.getBusinessDetails.companyProfile
      case a: AgentApplicationLlp => a.getBusinessDetails.companyProfile
      case a: AgentApplicationScottishLimitedPartnership => a.getBusinessDetails.companyProfile

  def companyStatusCheckResult: EntityCheckResult =
    agentApplication match
      case a: AgentApplicationLimitedCompany => a.companyStatusCheckResult
      case a: AgentApplicationLimitedPartnership => a.companyStatusCheckResult
      case a: AgentApplicationLlp => a.companyStatusCheckResult
      case a: AgentApplicationScottishLimitedPartnership => a.companyStatusCheckResult
