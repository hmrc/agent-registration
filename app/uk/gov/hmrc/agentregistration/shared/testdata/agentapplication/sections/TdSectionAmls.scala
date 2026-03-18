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

package uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.sections

import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.objectstore.client.Path

import java.time.LocalDate
import scala.util.chaining.scalaUtilChainingOps

trait TdSectionAmls {
  dependencies: TdBase & TdUpload =>

  private final def amlsCodeHmrc: AmlsCode = AmlsCode("HMRC")
  private def amlsCodeNonHmrc: AmlsCode = AmlsCode("ATT") /// Association of TaxationTechnicians

  private def amlsRegistrationNumberHmrc = AmlsRegistrationNumber("XAML00000123456")
  private def amlsRegistrationNumberNonHmrc = AmlsRegistrationNumber("NONHMRC-REF-AMLS-NUMBER-00001")

  def amlsExpiryDateValid: LocalDate = dependencies.nowPlus6mAsLocalDateTime.toLocalDate
  def amlsExpiryDateInvalid: LocalDate = dependencies.nowPlus13mAsLocalDateTime.toLocalDate

  class AgentApplicationWithSectionAmls(baseForSectionAmls: AgentApplication):

    /** when the supervisory body is HMRC, the registration number has a different format to non-HMRC bodies and no evidence or expiry date is required to be
      * considered complete
      */
    object sectionAmls:

      object whenSupervisorBodyIsHmrc:

        def amlsCode: AmlsCode = amlsCodeHmrc
        def amlsRegistrationNumber: AmlsRegistrationNumber = amlsRegistrationNumberHmrc

        private object amlsDetailsHelper:

          def afterSupervisoryBodySelected: AmlsDetails = AmlsDetails(
            supervisoryBody = amlsCode,
            amlsRegistrationNumber = None,
            amlsExpiryDate = None,
            amlsEvidence = None
          )

          def afterRegistrationNumberProvided: AmlsDetails = afterSupervisoryBodySelected.copy(
            amlsRegistrationNumber = Some(amlsRegistrationNumber)
          ).tap(x => require(x.isComplete, "when HMRC - no evidence or expiry date is required to be considered complete"))

        def afterSupervisoryBodySelected: AgentApplication = baseForSectionAmls
          .modify(_.amlsDetails)
          .setTo(Some(amlsDetailsHelper.afterSupervisoryBodySelected))

        def afterRegistrationNumberProvided: AgentApplication = baseForSectionAmls
          .modify(_.amlsDetails)
          .setTo(Some(amlsDetailsHelper.afterRegistrationNumberProvided))

        def complete: AgentApplication = afterRegistrationNumberProvided.tap(x => require(x.amlsDetails.exists(_.isComplete), "sanity check"))

      object whenSupervisorBodyIsNonHmrc:

        def amlsCode: AmlsCode = amlsCodeNonHmrc
        def amlsRegistrationNumber: AmlsRegistrationNumber = amlsRegistrationNumberNonHmrc

        private object amlsDetailsHelper:

          def afterSupervisoryBodySelected: AmlsDetails = AmlsDetails(
            supervisoryBody = amlsCode,
            amlsRegistrationNumber = None,
            amlsExpiryDate = None,
            amlsEvidence = None
          )

          def afterRegistrationNumberProvided: AmlsDetails = afterSupervisoryBodySelected.copy(
            amlsRegistrationNumber = Some(amlsRegistrationNumber)
          )

          def afterAmlsExpiryDateProvided: AmlsDetails = afterRegistrationNumberProvided.copy(
            amlsExpiryDate = Some(amlsExpiryDateValid)
          )

          def afterUploadedAmlsEvidence: AmlsDetails = {
            afterAmlsExpiryDateProvided.copy(
              amlsEvidence = Some(AmlsEvidence(
                uploadId = dependencies.uploadId,
                fileName = dependencies.fileName,
                objectStoreLocation = Path.File(dependencies.objectStoreLocation)
              ))
            )
          }

        def afterSupervisoryBodySelected: AgentApplication = baseForSectionAmls
          .modify(_.amlsDetails)
          .setTo(Some(amlsDetailsHelper.afterSupervisoryBodySelected))

        def afterRegistrationNumberProvided: AgentApplication = baseForSectionAmls
          .modify(_.amlsDetails)
          .setTo(Some(amlsDetailsHelper.afterRegistrationNumberProvided))

        def afterAmlsExpiryDateProvided: AgentApplication = baseForSectionAmls
          .modify(_.amlsDetails)
          .setTo(Some(amlsDetailsHelper.afterAmlsExpiryDateProvided))

        def afterUploadSucceeded: AgentApplication = baseForSectionAmls
          .modify(_.amlsDetails)
          .setTo(Some(amlsDetailsHelper.afterUploadedAmlsEvidence))

        def complete: AgentApplication = afterUploadSucceeded.tap(x => require(x.amlsDetails.exists(_.isComplete), "sanity check"))

}
