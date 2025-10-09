/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.Format
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory

sealed trait ApplicantName

object ApplicantName:
  implicit val format: Format[ApplicantName] = JsonFormatsFactory.makeSealedObjectFormat[ApplicantName]

final case class NameOfMember(
  memberNameQuery: Option[CompaniesHouseNameQuery] = None,
  companiesHouseOfficer: Option[CompaniesHouseOfficer] = None,
  role: ApplicantRoleInLlp = ApplicantRoleInLlp.Member // default value for deserialization compatibility
)
extends ApplicantName

object NameOfMember:
  implicit val format: Format[NameOfMember] = Json.format[NameOfMember]

final case class NameOfAuthorised(
  name: Option[String] = None,
  role: ApplicantRoleInLlp = ApplicantRoleInLlp.Authorised // default value for deserialization compatibility
)
extends ApplicantName

object NameOfAuthorised:
  implicit val format: Format[NameOfAuthorised] = Json.format[NameOfAuthorised]

final case class ApplicantContactDetails(
  applicantName: ApplicantName
):

  def getApplicantRole: ApplicantRoleInLlp =
    applicantName match
      case NameOfMember(_, _, role) => role
      case NameOfAuthorised(_, role) => role

  def getApplicantName: String =
    applicantName match
      case NameOfMember(
            Some(_),
            Some(officer),
            _
          ) =>
        officer.name
      case NameOfAuthorised(Some(name), _) => name
      case _ => throw new RuntimeException("No applicant name found")

  def getMemberNameQuery: CompaniesHouseNameQuery =
    applicantName match
      case NameOfMember(
            Some(nameQuery),
            _,
            _
          ) =>
        nameQuery
      case _ => throw new RuntimeException("No member name query found")

  def readMemberNameQuery: Option[CompaniesHouseNameQuery] =
    applicantName match
      case NameOfMember(
            maybeNameQuery,
            _,
            _
          ) =>
        maybeNameQuery
      case _ => throw new RuntimeException("Member name query is only available for member types")

object ApplicantContactDetails:
  implicit val format: Format[ApplicantContactDetails] = Json.format[ApplicantContactDetails]
