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

package uk.gov.hmrc.agentregistration.shared.contactdetails

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.shared.ApplicantRoleInLlp
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName.NameOfMember
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig

import scala.annotation.nowarn

sealed trait ApplicantName:

  val role: ApplicantRoleInLlp

  def as[T <: ApplicantName](using ct: reflect.ClassTag[T]): Option[T] =
    this match
      case t: T => Some(t)
      case _ => None

object ApplicantName:

  private val nameRegex = "^[a-zA-Z\\-' ]+$"
  def isValidName(name: String): Boolean = name.matches(nameRegex)

  final case class NameOfMember(
    memberNameQuery: Option[CompaniesHouseNameQuery] = None,
    companiesHouseOfficer: Option[CompaniesHouseOfficer] = None
  )
  extends ApplicantName:
    override val role: ApplicantRoleInLlp.Member.type = ApplicantRoleInLlp.Member

  final case class NameOfAuthorised(
    name: Option[String] = None
  )
  extends ApplicantName:
    override val role: ApplicantRoleInLlp.Authorised.type = ApplicantRoleInLlp.Authorised

  @nowarn
  given format: Format[ApplicantName] =
    given JsonConfiguration = JsonConfig.jsonConfiguration
    given OFormat[NameOfMember] = Json.format[NameOfMember]
    given OFormat[NameOfAuthorised] = Json.format[NameOfAuthorised]
    val dontDeleteMe = """
        |Don't delete me.
        |I will emit a warning so `@nowarn` can be applied to address below
        |`Unreachable case except for null` problem emitted by Play Json macro"""
    Json.format[ApplicantName]
