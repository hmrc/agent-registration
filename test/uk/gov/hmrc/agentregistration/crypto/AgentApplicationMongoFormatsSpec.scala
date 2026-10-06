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

package uk.gov.hmrc.agentregistration.crypto

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationFormat
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate

class AgentApplicationMongoFormatsSpec
extends UnitSpec:

  "plaintextFormat" - {

    "serialize and deserialize AgentApplication" in:
      val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterResubmitted
      AgentApplicationMongoFormats.plaintextFormat.writes(agentApplication) shouldBe afterResubmittedJson
      afterResubmittedJson.as[AgentApplication](using AgentApplicationMongoFormats.plaintextFormat) shouldBe agentApplication

    // TODO: remove with the ISO-string fallback in MongoDateFormats once the dates migration has run in every environment
    "deserialize AgentApplication stored before its dates were migrated to BSON dates" in:
      val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterResubmitted
      // before the migration, applications were stored in the REST shape
      val storedBeforeMigration: JsValue = Json.toJson(agentApplication)(using AgentApplicationFormat.restFormat)
      storedBeforeMigration.as[AgentApplication](using AgentApplicationMongoFormats.plaintextFormat) shouldBe agentApplication
  }

  "encryptingFormat" - {
    BusinessType.values.foreach: businessType =>
      s"serialize and deserialize a $businessType application" in:
        val agentApplication: AgentApplication = agentApplicationFor(businessType)
        mongoFormats.encryptingFormat.reads(mongoFormats.encryptingFormat.writes(agentApplication)).get shouldBe agentApplication

      s"store no plaintext PII for a $businessType application" in:
        val agentApplication: AgentApplication = agentApplicationFor(businessType)
        val written: String = mongoFormats.encryptingFormat.writes(agentApplication).toString
        piiStringsFor(agentApplication).foreach: plaintext =>
          withClue(s"plaintext '$plaintext' must not appear as a JSON value in $businessType Mongo JSON: "):
            written should not include s"\"$plaintext\""

    "agentApplicationFor gives each business type an application of that business type" in:
      BusinessType.values.foreach: businessType =>
        agentApplicationFor(businessType).businessType shouldBe businessType
  }

  "encrypt sets ciphertext on every PII field of an LLP application" - {

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

  "encrypt sets ciphertext on subtype-specific PII fields" - {

    "BusinessDetailsPartnership.postcode is encrypted (LimitedPartnership)" in:
      val model = tdAll.agentApplicationLimitedPartnership.afterDeclarationSubmitted
      val encrypted = mongoFormats.encrypt(model)
      encrypted.getBusinessDetails.postcode shouldBe enc(model.getBusinessDetails.postcode)

    "BusinessDetailsPartnership.postcode is encrypted (ScottishLimitedPartnership)" in:
      val model = tdAll.agentApplicationScottishLimitedPartnership.afterDeclarationSubmitted
      val encrypted = mongoFormats.encrypt(model)
      encrypted.getBusinessDetails.postcode shouldBe enc(model.getBusinessDetails.postcode)

    "BusinessDetailsGeneralPartnership.postcode is encrypted" in:
      val model = tdAll.agentApplicationGeneralPartnership.afterDeclarationSubmitted
      val encrypted = mongoFormats.encrypt(model)
      encrypted.getBusinessDetails.postcode shouldBe enc(model.getBusinessDetails.postcode)

    "BusinessDetailsScottishPartnership.postcode is encrypted" in:
      val model = tdAll.agentApplicationScottishPartnership.afterDeclarationSubmitted
      val encrypted = mongoFormats.encrypt(model)
      encrypted.getBusinessDetails.postcode shouldBe enc(model.getBusinessDetails.postcode)

    "BusinessDetailsSoleTrader.trn is encrypted when provided" in:
      val model = tdAll.agentApplicationSoleTrader.soleTraderWithTrn
      val encrypted = mongoFormats.encrypt(model)
      encrypted.getBusinessDetails.trn.value shouldBe enc(tdAll.trn)

    "BusinessDetailsSoleTrader.fullName fields are encrypted" in:
      val model = tdAll.agentApplicationSoleTrader.afterDeclarationSubmitted
      val encrypted = mongoFormats.encrypt(model)
      encrypted.getBusinessDetails.fullName.firstName shouldBe enc(model.getBusinessDetails.fullName.firstName)
      encrypted.getBusinessDetails.fullName.lastName shouldBe enc(model.getBusinessDetails.fullName.lastName)

    "BusinessDetailsSoleTrader.nino is encrypted" in:
      val model = tdAll.agentApplicationSoleTrader.afterDeclarationSubmitted
      val encrypted = mongoFormats.encrypt(model)
      encrypted.getBusinessDetails.nino.value.value shouldBe enc(model.getBusinessDetails.nino.value.value)

    "BusinessDetailsLimitedCompany.ctUtr is encrypted" in:
      val model = tdAll.agentApplicationLimitedCompany.afterDeclarationSubmitted
      val encrypted = mongoFormats.encrypt(model)
      encrypted.getBusinessDetails.ctUtr.value shouldBe enc(model.getBusinessDetails.ctUtr.value)
  }

  "encrypt and decrypt single values" - {
    "encrypt(InternalUserId) -> decrypt round-trips" in:
      mongoFormats.decrypt(mongoFormats.encrypt(tdAll.internalUserId)) shouldBe tdAll.internalUserId

    "encrypt(InternalUserId) produces ciphertext" in:
      mongoFormats.encrypt(tdAll.internalUserId).value shouldBe enc(tdAll.internalUserId.value)

    "encrypt(GroupId) produces ciphertext" in:
      mongoFormats.encrypt(tdAll.groupId).value shouldBe enc(tdAll.groupId.value)

    "encrypt(Vrn) produces ciphertext" in:
      mongoFormats.encrypt(tdAll.vrn).value shouldBe enc(tdAll.vrn.value)

    "encrypt(PayeRef) produces ciphertext" in:
      mongoFormats.encrypt(tdAll.payeRef).value shouldBe enc(tdAll.payeRef.value)
  }

  private lazy val tdAll: TdAll =
    new TdAll:
      // TdBase derives these dates from LocalDate.now(); the document below needs fixed values, so they are taken from the frozen time
      override def dateOfIncorporation: LocalDate = nowAsLocalDateTime.toLocalDate.minusYears(10)
      override def riskingCompletedDate: LocalDate = nowAsLocalDateTime.toLocalDate.minusDays(1)
      override def correctiveActionExpiryDate: LocalDate = nowAsLocalDateTime.toLocalDate.plusDays(45)

  // exhaustive on purpose: a new business type does not compile until it has an application here
  private def agentApplicationFor(businessType: BusinessType): AgentApplication =
    businessType match
      case BusinessType.SoleTrader => tdAll.agentApplicationSoleTrader.soleTraderWithTrn
      case BusinessType.LimitedCompany => tdAll.agentApplicationLimitedCompany.afterDeclarationSubmitted
      case BusinessType.Partnership.GeneralPartnership => tdAll.agentApplicationGeneralPartnership.afterDeclarationSubmitted
      case BusinessType.Partnership.LimitedLiabilityPartnership => tdAll.agentApplicationLlp.afterDeclarationSubmittedWithAllOptionalFields
      case BusinessType.Partnership.LimitedPartnership => tdAll.agentApplicationLimitedPartnership.afterDeclarationSubmitted
      case BusinessType.Partnership.ScottishLimitedPartnership => tdAll.agentApplicationScottishLimitedPartnership.afterDeclarationSubmitted
      case BusinessType.Partnership.ScottishPartnership => tdAll.agentApplicationScottishPartnership.afterDeclarationSubmitted

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
  private val mongoFormats: AgentApplicationMongoFormats = new AgentApplicationMongoFormats(fle)

  private def enc(plain: String): String = fle.encrypt(plain)

  private val llpModel: AgentApplicationLlp = tdAll.agentApplicationLlp.afterAgentDetailsComplete
  private val llpEncrypted: AgentApplicationLlp = mongoFormats.encrypt(llpModel)

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

  // dates as BSON dates
  private val afterResubmittedJson: JsValue = Json.parse(
    // language=JSON
    """{
      |  "_id": "agent-application-id-12345",
      |  "cachedSessionId": "session-id-123",
      |  "applicationReference": "APPREF123",
      |  "internalUserId": "internal-user-id-12345",
      |  "applicantCredentials": {
      |    "providerId": "cred-id-12345",
      |    "providerType": "GovernmentGateway"
      |  },
      |  "linkId": "link-id-12345",
      |  "groupId": "group-id-12345",
      |  "createdAt": {
      |    "$date": {
      |      "$numberLong": "2837003631880"
      |    }
      |  },
      |  "submittedAt": {
      |    "$date": {
      |      "$numberLong": "2837003631880"
      |    }
      |  },
      |  "applicationState": "SentForRisking",
      |  "userRole": "Authorised",
      |  "businessDetails": {
      |    "safeId": "XA0001234512345",
      |    "saUtr": "1234567895",
      |    "companyProfile": {
      |      "companyNumber": "1234567890",
      |      "companyName": "Test Partnership",
      |      "dateOfIncorporation": "2049-11-25",
      |      "unsanitisedCHROAddress": {
      |        "address_line_1": "23 Great Portland Street",
      |        "address_line_2": "London",
      |        "postal_code": "W1 8LT",
      |        "country": "GB"
      |      }
      |    }
      |  },
      |  "applicantContactDetails": {
      |    "applicantName": "Alice Smith",
      |    "telephoneNumber": "(+44) 10794554342",
      |    "applicantEmailAddress": {
      |      "emailAddress": "user@test.com",
      |      "isVerified": true
      |    }
      |  },
      |  "amlsDetails": {
      |    "supervisoryBody": "HMRC",
      |    "amlsRegistrationNumber": "XAML00000123456"
      |  },
      |  "agentDetails": {
      |    "businessName": {
      |      "agentBusinessName": "Test Company Name"
      |    },
      |    "telephoneNumber": {
      |      "agentTelephoneNumber": "(+44) 10794554342"
      |    },
      |    "agentEmailAddress": {
      |      "emailAddress": {
      |        "agentEmailAddress": "user@test.com"
      |      },
      |      "isVerified": true
      |    },
      |    "agentCorrespondenceAddress": {
      |      "addressLine1": "23 Great Portland Street",
      |      "addressLine2": "London",
      |      "postalCode": "W1 8LT",
      |      "countryCode": "GB"
      |    }
      |  },
      |  "refusalToDealWithCheckResult": "Pass",
      |  "globalAsaEnrolmentCheckResult": "Pass",
      |  "hmrcStandardForAgentsAgreed": "Agreed",
      |  "numberOfIndividuals": {
      |    "numberOfCompaniesHouseOfficers": 2,
      |    "isCompaniesHouseOfficersListCorrect": true,
      |    "type": "FiveOrLessOfficers"
      |  },
      |  "hasOtherRelevantIndividuals": false,
      |  "vrns": [
      |    "123456789"
      |  ],
      |  "payeRefs": [
      |    "123/AB12345"
      |  ],
      |  "riskingOutcomeApplication": {
      |    "actualDecisionDate": {
      |      "$date": {
      |        "$numberLong": "2836857600000"
      |      }
      |    },
      |    "correctiveActionExpiryDate": {
      |      "$date": {
      |        "$numberLong": "2840832000000"
      |      }
      |    },
      |    "reSubmittedAt": {
      |      "$date": {
      |        "$numberLong": "2837003631880"
      |      }
      |    },
      |    "outcome": "FailedFixable"
      |  },
      |  "riskingOutcomeEntity": {
      |    "fixes": [
      |      {
      |        "failure": {
      |          "type": "_3._5"
      |        },
      |        "isConfirmed": true,
      |        "amlsDetails": {
      |          "supervisoryBody": "HMRC",
      |          "amlsRegistrationNumber": "XAML00000123456"
      |        },
      |        "type": "EntityFix._3.AmlsFix"
      |      },
      |      {
      |        "isConfirmed": true,
      |        "type": "EntityFix._4._4"
      |      },
      |      {
      |        "isConfirmed": true,
      |        "type": "EntityFix._5._4"
      |      }
      |    ],
      |    "type": "FailedFixable"
      |  },
      |  "type": "AgentApplicationLlp"
      |}""".stripMargin
  )
