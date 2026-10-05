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

package uk.gov.hmrc.agentregistration.crypto

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AgentApplicationEncryptionSpec
extends UnitSpec:

  private val tdAll: TdAll = TdAll()

  private val configuration: Configuration = Configuration(ConfigFactory.parseString(
    """
      |appName = "agent-registration"
      |microservice.services.des.host = "localhost"
      |microservice.services.des.port = 1234
      |microservice.services.des.protocol = "http"
      |microservice.services.des.environment = "test"
      |microservice.services.des.authorization-token = "test-token"
      |microservice.services.hip.host = "localhost"
      |microservice.services.hip.port = 1234
      |microservice.services.hip.protocol = "http"
      |microservice.services.hip.authorization-token = "test-token"
      |field-level-encryption.enabled = true
      |field-level-encryption.key = "HIvqb3uQRW8oryUZ3jEQPgMQsvgBSgl71ygWJk6VIdc="
      |field-level-encryption.previousKeys = []
      |""".stripMargin
  ))
  private val appConfig: AppConfig = new AppConfig(new ServicesConfig(configuration), configuration)
  private val fle: FieldLevelEncryption = new FieldLevelEncryption(appConfig)
  private val service: AgentApplicationEncryption = new AgentApplicationEncryption(fle)

  private def enc(plain: String): String = fle.encrypt(plain)

  private val llpModel: AgentApplicationLlp = tdAll.agentApplicationLlp.afterAgentDetailsComplete
  private val llpEncrypted: AgentApplicationLlp = service.encrypt(llpModel)

  "AgentApplicationEncryption.encrypt sets ciphertext on every PII field (LLP)" - {

    "internalUserId is encrypted" in:
      llpEncrypted.internalUserId.value shouldBe enc(llpModel.internalUserId.value)

    "groupId is encrypted" in:
      llpEncrypted.groupId.value shouldBe enc(llpModel.groupId.value)

    "applicantCredentials.providerId is encrypted" in:
      llpEncrypted.applicantCredentials.providerId shouldBe enc(llpModel.applicantCredentials.providerId)

    "applicantCredentials.providerType stays plaintext" in:
      llpEncrypted.applicantCredentials.providerType shouldBe llpModel.applicantCredentials.providerType

    "businessDetails.saUtr is encrypted" in:
      llpEncrypted.getBusinessDetails.saUtr.value shouldBe enc(llpModel.getBusinessDetails.saUtr.value)

    "businessDetails.companyProfile.companyNumber is encrypted" in:
      llpEncrypted.getBusinessDetails.companyProfile.companyNumber.value shouldBe enc(llpModel.getBusinessDetails.companyProfile.companyNumber.value)

    "businessDetails.companyProfile.companyName is encrypted" in:
      llpEncrypted.getBusinessDetails.companyProfile.companyName shouldBe enc(llpModel.getBusinessDetails.companyProfile.companyName)

    "businessDetails.companyProfile.unsanitisedCHROAddress.address_line_1 is encrypted" in:
      llpEncrypted.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value.address_line_1.value shouldBe
        enc(llpModel.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value.address_line_1.value)

    "businessDetails.companyProfile.unsanitisedCHROAddress.address_line_2 is encrypted" in:
      llpEncrypted.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value.address_line_2.value shouldBe
        enc(llpModel.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value.address_line_2.value)

    "businessDetails.companyProfile.unsanitisedCHROAddress.postal_code is encrypted" in:
      llpEncrypted.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value.postal_code.value shouldBe
        enc(llpModel.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value.postal_code.value)

    "businessDetails.companyProfile.unsanitisedCHROAddress.country stays plaintext" in:
      llpEncrypted.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value.country shouldBe
        llpModel.getBusinessDetails.companyProfile.unsanitisedCHROAddress.value.country

    "applicantContactDetails.applicantName is encrypted" in:
      llpEncrypted.getApplicantContactDetails.applicantName.value shouldBe enc(llpModel.getApplicantContactDetails.applicantName.value)

    "applicantContactDetails.telephoneNumber is encrypted" in:
      llpEncrypted.getApplicantContactDetails.getTelephoneNumber.value shouldBe enc(llpModel.getApplicantContactDetails.getTelephoneNumber.value)

    "applicantContactDetails.applicantEmailAddress.emailAddress is encrypted" in:
      llpEncrypted.getApplicantContactDetails.getApplicantEmailAddress.emailAddress.value shouldBe
        enc(llpModel.getApplicantContactDetails.getApplicantEmailAddress.emailAddress.value)

    "agentDetails.businessName.agentBusinessName is encrypted" in:
      llpEncrypted.getAgentDetails.businessName.agentBusinessName shouldBe enc(llpModel.getAgentDetails.businessName.agentBusinessName)

    "agentDetails.telephoneNumber.agentTelephoneNumber is encrypted" in:
      llpEncrypted.getAgentDetails.getTelephoneNumber.agentTelephoneNumber shouldBe enc(llpModel.getAgentDetails.getTelephoneNumber.agentTelephoneNumber)

    "agentDetails.agentEmailAddress.emailAddress.agentEmailAddress is encrypted" in:
      llpEncrypted.getAgentDetails.getAgentEmailAddress.emailAddress.agentEmailAddress.value shouldBe
        enc(llpModel.getAgentDetails.getAgentEmailAddress.emailAddress.agentEmailAddress.value)

    "agentDetails.agentCorrespondenceAddress.addressLine1 is encrypted" in:
      llpEncrypted.getAgentDetails.getAgentCorrespondenceAddress.addressLine1 shouldBe enc(llpModel.getAgentDetails.getAgentCorrespondenceAddress.addressLine1)

    "agentDetails.agentCorrespondenceAddress.addressLine2 is encrypted" in:
      llpEncrypted.getAgentDetails.getAgentCorrespondenceAddress.addressLine2.value shouldBe
        enc(llpModel.getAgentDetails.getAgentCorrespondenceAddress.addressLine2.value)

    "agentDetails.agentCorrespondenceAddress.postalCode is encrypted" in:
      llpEncrypted.getAgentDetails.getAgentCorrespondenceAddress.postalCode.value shouldBe
        enc(llpModel.getAgentDetails.getAgentCorrespondenceAddress.postalCode.value)

    "agentDetails.agentCorrespondenceAddress.countryCode stays plaintext" in:
      llpEncrypted.getAgentDetails.getAgentCorrespondenceAddress.countryCode shouldBe
        llpModel.getAgentDetails.getAgentCorrespondenceAddress.countryCode

    "vrns are encrypted element-wise" in:
      llpEncrypted.vrns.value.map(_.value) shouldBe llpModel.vrns.value.map(v => enc(v.value))

    "payeRefs are encrypted element-wise" in:
      llpEncrypted.payeRefs.value.map(_.value) shouldBe llpModel.payeRefs.value.map(p => enc(p.value))

    "applicationReference stays plaintext (search key, not PII)" in:
      llpEncrypted.applicationReference.value shouldBe llpModel.applicationReference.value

    "linkId stays plaintext" in:
      llpEncrypted.linkId.value shouldBe llpModel.linkId.value
  }

  "AgentApplicationEncryption encrypts subtype-specific PII fields" - {

    "BusinessDetailsPartnership.postcode is encrypted (LimitedPartnership)" in:
      val model = tdAll.agentApplicationLimitedPartnership.afterDeclarationSubmitted
      val encrypted = service.encrypt(model)
      encrypted.getBusinessDetails.postcode shouldBe enc(model.getBusinessDetails.postcode)

    "BusinessDetailsPartnership.postcode is encrypted (ScottishLimitedPartnership)" in:
      val model = tdAll.agentApplicationScottishLimitedPartnership.afterDeclarationSubmitted
      val encrypted = service.encrypt(model)
      encrypted.getBusinessDetails.postcode shouldBe enc(model.getBusinessDetails.postcode)

    "BusinessDetailsGeneralPartnership.postcode is encrypted" in:
      val model = tdAll.agentApplicationGeneralPartnership.afterDeclarationSubmitted
      val encrypted = service.encrypt(model)
      encrypted.getBusinessDetails.postcode shouldBe enc(model.getBusinessDetails.postcode)

    "BusinessDetailsScottishPartnership.postcode is encrypted" in:
      val model = tdAll.agentApplicationScottishPartnership.afterDeclarationSubmitted
      val encrypted = service.encrypt(model)
      encrypted.getBusinessDetails.postcode shouldBe enc(model.getBusinessDetails.postcode)

    "BusinessDetailsSoleTrader.trn is encrypted when provided" in:
      val model = tdAll.agentApplicationSoleTrader.soleTraderWithTrn
      val encrypted = service.encrypt(model)
      encrypted.getBusinessDetails.trn.value shouldBe enc(tdAll.trn)

    "BusinessDetailsSoleTrader.fullName fields are encrypted" in:
      val model = tdAll.agentApplicationSoleTrader.afterDeclarationSubmitted
      val encrypted = service.encrypt(model)
      encrypted.getBusinessDetails.fullName.firstName shouldBe enc(model.getBusinessDetails.fullName.firstName)
      encrypted.getBusinessDetails.fullName.lastName shouldBe enc(model.getBusinessDetails.fullName.lastName)

    "BusinessDetailsSoleTrader.nino is encrypted" in:
      val model = tdAll.agentApplicationSoleTrader.afterDeclarationSubmitted
      val encrypted = service.encrypt(model)
      encrypted.getBusinessDetails.nino.value.value shouldBe enc(model.getBusinessDetails.nino.value.value)

    "BusinessDetailsLimitedCompany.ctUtr is encrypted" in:
      val model = tdAll.agentApplicationLimitedCompany.afterDeclarationSubmitted
      val encrypted = service.encrypt(model)
      encrypted.getBusinessDetails.ctUtr.value shouldBe enc(model.getBusinessDetails.ctUtr.value)
  }

  "AgentApplicationEncryption single-value helpers" - {
    "encrypt(InternalUserId) -> decrypt round-trips" in:
      service.decrypt(service.encrypt(tdAll.internalUserId)) shouldBe tdAll.internalUserId

    "encrypt(InternalUserId) produces ciphertext" in:
      service.encrypt(tdAll.internalUserId).value shouldBe enc(tdAll.internalUserId.value)

    "encrypt(GroupId) produces ciphertext" in:
      service.encrypt(tdAll.groupId).value shouldBe enc(tdAll.groupId.value)

    "encrypt(Vrn) produces ciphertext" in:
      service.encrypt(tdAll.vrn).value shouldBe enc(tdAll.vrn.value)

    "encrypt(PayeRef) produces ciphertext" in:
      service.encrypt(tdAll.payeRef).value shouldBe enc(tdAll.payeRef.value)
  }

  "AgentApplicationEncryption round-trips every subtype and does not leak plaintext PII" - {
    val subtypes: Seq[(String, AgentApplication)] = Seq(
      "AgentApplicationLlp" -> tdAll.agentApplicationLlp.afterDeclarationSubmittedWithAllOptionalFields,
      "AgentApplicationSoleTrader" -> tdAll.agentApplicationSoleTrader.soleTraderWithTrn,
      "AgentApplicationLimitedCompany" -> tdAll.agentApplicationLimitedCompany.afterDeclarationSubmitted,
      "AgentApplicationGeneralPartnership" -> tdAll.agentApplicationGeneralPartnership.afterDeclarationSubmitted,
      "AgentApplicationLimitedPartnership" -> tdAll.agentApplicationLimitedPartnership.afterDeclarationSubmitted,
      "AgentApplicationScottishLimitedPartnership" -> tdAll.agentApplicationScottishLimitedPartnership.afterDeclarationSubmitted,
      "AgentApplicationScottishPartnership" -> tdAll.agentApplicationScottishPartnership.afterDeclarationSubmitted
    )

    subtypes.foreach { case (name, model) =>
      s"$name encrypt then decrypt is identity" in:
        service.decrypt(service.encrypt(model)) shouldBe model

      s"$name rendered JSON of the encrypted model contains no plaintext PII" in:
        val rendered = Json.toJson[AgentApplication](service.encrypt(model))(using AgentApplication.format).toString
        piiStringsFor(model).foreach { plaintext =>
          withClue(s"plaintext '$plaintext' must not appear as a JSON value in $name encrypted JSON: ") {
            rendered should not include s"\"$plaintext\""
          }
        }

      s"$name Mongo formats write then read is identity" in:
        service.formats.reads(service.formats.writes(model)).get shouldBe model

      s"$name JSON written by the Mongo formats contains no plaintext PII" in:
        val written: String = service.formats.writes(model).toString
        piiStringsFor(model).foreach { plaintext =>
          withClue(s"plaintext '$plaintext' must not appear as a JSON value in $name Mongo JSON: ") {
            written should not include s"\"$plaintext\""
          }
        }
    }
  }

  "JSON shape of Instant fields in the REST format and in the Mongo format" - {
    val model: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    val restJson: JsValue = Json.toJson[AgentApplication](model)(using AgentApplication.format)
    val mongoJson: JsValue = service.formats.writes(model)

    "REST format writes createdAt and applicationExpiresAt as ISO strings" in:
      (restJson \ "createdAt").get shouldBe JsString("2059-11-25T16:33:51.880Z")
      (restJson \ "applicationExpiresAt").get shouldBe JsString("2060-02-06T16:33:51.880Z")

    "Mongo format writes createdAt and applicationExpiresAt as BSON Date" in:
      (mongoJson \ "createdAt").get shouldBe Json.parse("""{ "$date": { "$numberLong": "2837003631880" } }""")
      (mongoJson \ "applicationExpiresAt").get shouldBe Json.parse("""{ "$date": { "$numberLong": "2843310831880" } }""")

    "REST format writes submittedAt as an ISO string and Mongo format writes it as a BSON Date" in:
      val submitted: AgentApplication = tdAll.agentApplicationLlp.afterSentForRisking
      val submittedAt: Instant = submitted.submittedAt.value
      (Json.toJson[AgentApplication](submitted)(using AgentApplication.format) \ "submittedAt").get shouldBe JsString(submittedAt.toString)
      (service.formats.writes(submitted) \ "submittedAt").get shouldBe
        Json.obj("$date" -> Json.obj("$numberLong" -> submittedAt.toEpochMilli.toString))

    "Mongo format reads back what it wrote" in:
      service.formats.reads(mongoJson).get shouldBe model

    "Mongo format reads createdAt and applicationExpiresAt stored as legacy ISO strings" in:
      val legacyMongoJson: JsObject =
        mongoJson.as[JsObject] ++ Json.obj(
          "createdAt" -> "2059-11-25T16:33:51.880Z",
          "applicationExpiresAt" -> "2060-02-06T16:33:51.880Z"
        )
      service.formats.reads(legacyMongoJson).get shouldBe model

    "REST format writes riskingOutcomeApplication.reSubmittedAt as an ISO string and Mongo format writes it as a BSON Date" in:
      val resubmitted: AgentApplication = tdAll.agentApplicationLlp.afterResubmitted
      (Json.toJson[AgentApplication](resubmitted)(using AgentApplication.format) \ "riskingOutcomeApplication" \ "reSubmittedAt").get shouldBe
        JsString("2059-11-25T16:33:51.880Z")
      (service.formats.writes(resubmitted) \ "riskingOutcomeApplication" \ "reSubmittedAt").get shouldBe
        Json.parse("""{ "$date": { "$numberLong": "2837003631880" } }""")

    "Mongo format reads riskingOutcomeApplication.reSubmittedAt stored as a BSON Date or as a legacy ISO string" in:
      val resubmitted: AgentApplication = tdAll.agentApplicationLlp.afterResubmitted
      val resubmittedMongoJson: JsObject = service.formats.writes(resubmitted)
      val legacyMongoJson: JsObject = resubmittedMongoJson.deepMerge(
        Json.obj("riskingOutcomeApplication" -> Json.obj("reSubmittedAt" -> "2059-11-25T16:33:51.880Z"))
      )
      service.formats.reads(resubmittedMongoJson).get shouldBe resubmitted
      service.formats.reads(legacyMongoJson).get shouldBe resubmitted

    "REST format writes correctiveActionExpiryDate as an ISO date string and Mongo format writes it as a BSON Date at midnight UTC" in:
      val fixable: AgentApplication = tdAll.agentApplicationLlp.afterRiskingCompletedFixable
      val correctiveActionExpiryDate: LocalDate = tdAll.riskingOutcomeApplication.failedFixable.correctiveActionExpiryDate
      val midnightUtcMillis: Long = correctiveActionExpiryDate.atStartOfDay(ZoneOffset.UTC).toInstant.toEpochMilli
      (Json.toJson[AgentApplication](fixable)(using AgentApplication.format) \ "riskingOutcomeApplication" \ "correctiveActionExpiryDate").get shouldBe
        JsString(correctiveActionExpiryDate.toString)
      (service.formats.writes(fixable) \ "riskingOutcomeApplication" \ "correctiveActionExpiryDate").get shouldBe
        Json.obj("$date" -> Json.obj("$numberLong" -> midnightUtcMillis.toString))

    "Mongo format keeps actualDecisionDate as an ISO date string" in:
      val fixable: AgentApplication = tdAll.agentApplicationLlp.afterRiskingCompletedFixable
      (service.formats.writes(fixable) \ "riskingOutcomeApplication" \ "actualDecisionDate").get shouldBe
        JsString(tdAll.riskingOutcomeApplication.failedFixable.actualDecisionDate.toString)

    "Mongo format reads correctiveActionExpiryDate stored as a BSON Date or as a legacy ISO date string" in:
      Seq(
        tdAll.agentApplicationLlp.afterRiskingCompletedFixable,
        tdAll.agentApplicationLlp.afterRiskingCompletedNonFixable
      ).foreach: application =>
        val applicationMongoJson: JsObject = service.formats.writes(application)
        val correctiveActionExpiryDate: String =
          (Json.toJson[AgentApplication](application)(using AgentApplication.format) \ "riskingOutcomeApplication" \ "correctiveActionExpiryDate").as[String]
        val legacyMongoJson: JsObject = applicationMongoJson.deepMerge(
          Json.obj("riskingOutcomeApplication" -> Json.obj("correctiveActionExpiryDate" -> correctiveActionExpiryDate))
        )
        service.formats.reads(applicationMongoJson).get shouldBe application
        service.formats.reads(legacyMongoJson).get shouldBe application
  }

  private def companyProfilePiiStrings(cp: CompanyProfile): List[String] =
    List(cp.companyNumber.value, cp.companyName) ++
      cp.unsanitisedCHROAddress.toList.flatMap(a =>
        List(
          a.address_line_1,
          a.address_line_2,
          a.postal_code
        ).flatten
      )

  private def piiStringsFor(application: AgentApplication): List[String] = {
    val common: List[String] =
      List(
        application.internalUserId.value,
        application.groupId.value,
        application.applicantCredentials.providerId
      ) ++
        application.applicantContactDetails.toList.flatMap { c =>
          List(c.applicantName.value) ++
            c.telephoneNumber.toList.map(_.value) ++
            c.applicantEmailAddress.toList.map(_.emailAddress.value)
        } ++
        application.agentDetails.toList.flatMap { a =>
          List(a.businessName.agentBusinessName) ++
            a.businessName.otherAgentBusinessName.toList ++
            a.telephoneNumber.toList.flatMap(t => List(t.agentTelephoneNumber) ++ t.otherAgentTelephoneNumber.toList) ++
            a.agentEmailAddress.toList.flatMap(e =>
              List(e.emailAddress.agentEmailAddress.value) ++ e.emailAddress.otherAgentEmailAddress.toList.map(_.value)
            ) ++
            a.agentCorrespondenceAddress.toList.flatMap(addr => List(addr.addressLine1) ++ addr.addressLine2.toList ++ addr.postalCode.toList)
        } ++
        application.vrns.toList.flatten.map(_.value) ++
        application.payeRefs.toList.flatten.map(_.value)

    val businessSpecific: List[String] =
      application match
        case llp: AgentApplicationLlp =>
          val bd = llp.getBusinessDetails
          bd.saUtr.value +: companyProfilePiiStrings(bd.companyProfile)
        case ltd: AgentApplicationLimitedCompany =>
          val bd = ltd.getBusinessDetails
          bd.ctUtr.value +: companyProfilePiiStrings(bd.companyProfile)
        case lp: AgentApplicationLimitedPartnership =>
          val bd = lp.getBusinessDetails
          List(bd.saUtr.value, bd.postcode) ++ companyProfilePiiStrings(bd.companyProfile)
        case slp: AgentApplicationScottishLimitedPartnership =>
          val bd = slp.getBusinessDetails
          List(bd.saUtr.value, bd.postcode) ++ companyProfilePiiStrings(bd.companyProfile)
        case gp: AgentApplicationGeneralPartnership =>
          val bd = gp.getBusinessDetails
          List(bd.saUtr.value, bd.postcode)
        case sp: AgentApplicationScottishPartnership =>
          val bd = sp.getBusinessDetails
          List(bd.saUtr.value, bd.postcode)
        case st: AgentApplicationSoleTrader =>
          val bd = st.getBusinessDetails
          List(
            bd.saUtr.value,
            bd.fullName.firstName,
            bd.fullName.lastName
          ) ++
            bd.nino.map(_.value).toList ++
            bd.trn.toList

    common ++ businessSpecific
  }
