/*
 * Copyright 2024 HM Revenue & Customs
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
import upscan.UploadStatus
import upscan.UploadDetails

import java.time.LocalDate

final case class AmlsDetails(
  supervisoryBody: AmlsCode,
  amlsRegistrationNumber: Option[AmlsRegistrationNumber] = None,
  amlsExpiryDate: Option[LocalDate] = None,
  amlsEvidence: Option[UploadDetails] = None
):

  val isHmrc: Boolean = supervisoryBody.value.contains("HMRC")
  val isComplete: Boolean =
    this match {
      case AmlsDetails(
            _,
            Some(_),
            _,
            _
          ) if isHmrc =>
        true
      case AmlsDetails(
            _,
            Some(_),
            Some(_),
            Some(UploadDetails(_, UploadStatus.UploadedSuccessfully(_, _, _, _, _)))
          ) if !isHmrc =>
        true
      case _ => false
    }
  def getAmlsEvidence: UploadDetails = amlsEvidence.getOrElse(throw new RuntimeException("AmlsEvidence missing when required"))
  def getRegistrationNumber: AmlsRegistrationNumber = amlsRegistrationNumber.getOrElse(
    throw new RuntimeException("amlsRegistrationNumber missing when required")
  )
  def getAmlsExpiryDate: LocalDate = amlsExpiryDate.getOrElse(throw new RuntimeException("amlsExpiryDate missing when required"))
  def getAmlsEvidenceName: String =
    getAmlsEvidence.status match
      case s: UploadStatus.UploadedSuccessfully => s.name
      case _ => throw new RuntimeException(s"AmlsEvidence not ready: ${getAmlsEvidence.status}")

object AmlsDetails:
  implicit val format: Format[AmlsDetails] = Json.format[AmlsDetails]
