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

package uk.gov.hmrc.agentregistration.shared.testdata

import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.agentdetails.*
import uk.gov.hmrc.agentregistration.shared.amls.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.amls.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.amls.AmlsSupervisoryBodyCode
import uk.gov.hmrc.agentregistration.shared.audit.SessionId
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.businessdetails.FullName
import uk.gov.hmrc.agentregistration.shared.companieshouse.ChroAddress
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseDateOfBirth
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseNameQuery
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficer
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers
import uk.gov.hmrc.agentregistration.shared.individual.*
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficerRole.LlpMember
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.auth.core.ConfidenceLevel

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID

trait TdBase:

  final val zoneOffset: ZoneOffset = ZoneOffset.UTC
  final val zoneId: ZoneId = ZoneId.of("UTC")

  def dateString: String = "2059-11-25"
  def timeString: String = s"${dateString}T16:33:51.880"
  def randomId(): String = UUID.randomUUID().toString

  def nowAsLocalDateTime: LocalDateTime =
    // the frozen time has to be in future otherwise the applications will disappear from mongodb because of expiry index
    LocalDateTime.parse(timeString, DateTimeFormatter.ISO_DATE_TIME)

  def nowPlus6mAsLocalDateTime: LocalDateTime = nowAsLocalDateTime.plus(java.time.Period.ofMonths(6))
  def nowPlus13mAsLocalDateTime: LocalDateTime = nowAsLocalDateTime.plus(java.time.Period.ofMonths(13))
  def newPlus20sAsLocalDateTime: LocalDateTime = nowAsLocalDateTime.plusSeconds(20)

  def nowAsInstant: Instant = nowAsLocalDateTime.toInstant(ZoneOffset.UTC)
  def newInstant: Instant = nowAsInstant.plusSeconds(20) // used when a new application is created from existing one
  def applicationExpiresAtAsInstant: Instant = nowAsInstant.plus(Duration.ofDays(73)) // matches days-to-submit-application config

  final val clock: Clock = Clock.fixed(nowAsInstant, zoneId)

  def cachedSessionId: SessionId = SessionId("session-id-123")
  def saUtr: SaUtr = SaUtr("1234567895")
  def ctUtr: CtUtr = CtUtr("2202108031")
  def internalUserId: InternalUserId = InternalUserId("internal-user-id-12345")
  def linkId: LinkId = LinkId("link-id-12345")
  def groupId: GroupId = GroupId("group-id-12345")
  def credentials: Credentials = Credentials(
    providerId = "cred-id-12345",
    providerType = "GovernmentGateway"
  )
  def confidenceLevel250: ConfidenceLevel = ConfidenceLevel.L250
  def confidenceLevel50: ConfidenceLevel = ConfidenceLevel.L50

  def individualName = IndividualName("Test Name")
  def nino = Nino("AB123456C")
  def ninoFromAuth = IndividualNino.FromAuth(nino)
  def ninoProvided = IndividualNino.Provided(nino)
  def saUtrFromAuth = IndividualSaUtr.FromAuth(saUtr)
  def saUtrFromCitizenDetails = IndividualSaUtr.FromCitizenDetails(saUtr)
  def saUtrProvided = IndividualSaUtr.Provided(saUtr)
  def safeId: SafeId = SafeId("XA0001234512345")
  def dateOfBirth: LocalDate = LocalDate.of(2000, 1, 1)
  def dateOfBirthFromCitizenDetails: IndividualDateOfBirth.FromCitizensDetails = IndividualDateOfBirth.FromCitizensDetails(dateOfBirth)
  def dateOfBirthProvided = IndividualDateOfBirth.Provided(dateOfBirth)
  def fullName: FullName = FullName(firstName = "ST Name", lastName = "ST Lastname")

  def applicantEmailAddress: EmailAddress = EmailAddress("user@test.com")
  def individualEmailAddress: EmailAddress = EmailAddress("member@test.com")

  def telephoneNumber: TelephoneNumber = TelephoneNumber("(+44) 10794554342")
  def crn: Crn = Crn("1234567890")
  def companyName = "Test Company Name"
  def limitedCompanyName = "Test Company Ltd"
  def limitedPartnershipName = "Test Partnership"
  def dateOfIncorporation: LocalDate = LocalDate.now().minusYears(10)
  def applicationReference: ApplicationReference = ApplicationReference("APPREF123")
  def personReference: PersonReference = PersonReference("1234567890")
  def applicantName: ApplicantName = ApplicantName(authorisedPersonName)
  def agentBusinessName: AgentBusinessName = AgentBusinessName(agentBusinessName = companyName, otherAgentBusinessName = None)
  def amlsCode: AmlsSupervisoryBodyCode = AmlsSupervisoryBodyCode("HMRC")
  def amlsRegistrationNumber: AmlsRegistrationNumber = AmlsRegistrationNumber("XAML00000123456")
  def vrn = Vrn("123456789")
  def payeRef = PayeRef("123/AB12345")
  def trn: String = "ST-TRN-987654321"
  def individualDateOfBirth: LocalDate = LocalDate.of(1980, 1, 1)
  def agentTelephoneNumber = AgentTelephoneNumber(agentTelephoneNumber = telephoneNumber.value, otherAgentTelephoneNumber = None)
  def riskingCompletedDate: LocalDate = LocalDate.now().minusDays(1)
  def correctiveActionExpiryDate: LocalDate = LocalDate.now().plusDays(45)

  def companyProfile: CompanyProfile = CompanyProfile(
    companyNumber = crn,
    companyName = companyName,
    dateOfIncorporation = Some(dateOfIncorporation),
    unsanitisedCHROAddress = Some(ChroAddress(
      address_line_1 = Some("23 Great Portland Street"),
      address_line_2 = Some("London"),
      postal_code = Some("W1 8LT"),
      country = Some("GB")
    ))
  )

  def companyProfileLimited: CompanyProfile = companyProfile.copy(companyName = limitedCompanyName)
  def companyProfileLimitedPartnership: CompanyProfile = companyProfile.copy(companyName = limitedPartnershipName)
  def postcode: String = "AA1 1AA"
  def authorisedPersonName: String = "Alice Smith"
  def agentVerifiedEmailAddress = AgentVerifiedEmailAddress(
    emailAddress = AgentEmailAddress(
      agentEmailAddress = applicantEmailAddress.value,
      otherAgentEmailAddress = None
    ),
    isVerified = true
  )
  def individualVerifiedEmailAddress = IndividualVerifiedEmailAddress(individualEmailAddress, isVerified = true)
  def applicantContactDetails: ApplicantContactDetails = ApplicantContactDetails(
    applicantName = applicantName,
    telephoneNumber = Some(telephoneNumber),
    applicantEmailAddress = Some(ApplicantEmailAddress(
      emailAddress = applicantEmailAddress,
      isVerified = true
    ))
  )
  def completeAgentDetails: AgentDetails = AgentDetails(
    agentCorrespondenceAddress = Some(chroAddress),
    telephoneNumber = Some(agentTelephoneNumber),
    agentEmailAddress = Some(agentVerifiedEmailAddress),
    businessName = agentBusinessName
  )

  def completeAmlsDetails: AmlsDetails = AmlsDetails(
    supervisoryBody = amlsCode,
    amlsRegistrationNumber = Some(amlsRegistrationNumber),
    amlsEvidence = None
  )

  def completeAmlsDetailsAtt: AmlsDetails = AmlsDetails(
    supervisoryBody = AmlsSupervisoryBodyCode("ATT"),
    amlsRegistrationNumber = Some(AmlsRegistrationNumber("ATT AML-1-123456")),
    amlsEvidence = Some(uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence(
      uk.gov.hmrc.agentregistration.shared.upload.FileUploadReference("test-file-reference"),
      "evidence.pdf",
      uk.gov.hmrc.objectstore.client.Path.File("agent-registration-frontend/9d5ddeed-d26e-4005-97ca-e40f2466e0a3/evidence.pdf")
    ))
  )

  def agentApplicationId: AgentApplicationId = AgentApplicationId("agent-application-id-12345")

  def individualProvidedDetailsId: IndividualProvidedDetailsId = IndividualProvidedDetailsId("individual-provided-details-id-12345")
  def bprPrimaryTelephoneNumber: String = "(+44) 78714743399"
  def newTelephoneNumber: String = "+44 (0) 7000000000"
  def bprEmailAddress: String = "bpr@example.com"
  def newEmailAddress: String = "new@example.com"
  def chroAddress: AgentCorrespondenceAddress = AgentCorrespondenceAddress(
    addressLine1 = "23 Great Portland Street",
    addressLine2 = Some("London"),
    postalCode = Some("W1 8LT"),
    countryCode = "GB"
  )
  def bprRegisteredAddress: DesBusinessAddress = DesBusinessAddress(
    addressLine1 = "Registered Line 1",
    addressLine2 = Some("Registered Line 2"),
    addressLine3 = None,
    addressLine4 = None,
    postalCode = Some("AB1 2CD"),
    countryCode = "GB"
  )
  def llpNameQuery = CompaniesHouseNameQuery(
    firstName = "Jane",
    lastName = "Leadenhall-Lane"
  )
  def companiesHouseOfficer = CompaniesHouseOfficer(
    name = "Taylor Leadenhall-Lane",
    dateOfBirth = Some(CompaniesHouseDateOfBirth(
      day = Some(12),
      month = 11,
      year = 1990
    )),
    resignedOn = None,
    officerRole = Some(LlpMember),
    identification = None
  )

  def businessPartnerRecordResponse: BusinessPartnerRecordResponse = BusinessPartnerRecordResponse(
    organisationName = Some("Test Company Name"),
    agentReferenceNumber = None,
    individualName = None,
    address = bprRegisteredAddress,
    primaryPhoneNumber = Some(bprPrimaryTelephoneNumber),
    emailAddress = Some(bprEmailAddress),
    isAnAsaAgent = false
  )

  def businessPartnerRecordResponseSoleTrader: BusinessPartnerRecordResponse = BusinessPartnerRecordResponse(
    organisationName = None,
    agentReferenceNumber = None,
    individualName = Some(individualName.value),
    address = bprRegisteredAddress,
    primaryPhoneNumber = Some(bprPrimaryTelephoneNumber),
    emailAddress = Some(bprEmailAddress),
    isAnAsaAgent = false
  )

  def fiveOrFewerKeyIndividuals: FiveOrLess = FiveOrLess(
    numberOfKeyIndividuals = 3
  )

  def fiveOrLessCompaniesHouseOfficers: FiveOrLessOfficers = FiveOrLessOfficers(
    numberOfCompaniesHouseOfficers = 1,
    isCompaniesHouseOfficersListCorrect = true
  )

  def twoCompaniesHouseOfficers: FiveOrLessOfficers = FiveOrLessOfficers(
    numberOfCompaniesHouseOfficers = 2,
    isCompaniesHouseOfficersListCorrect = true
  )

  def sixOrMoreKeyIndividuals: SixOrMore = SixOrMore(
    numberOfKeyIndividualsResponsibleForTaxMatters = 3
  )

  def sixOrMoreCompaniesHouseOfficers: SixOrMoreOfficers = SixOrMoreOfficers(
    numberOfCompaniesHouseOfficers = 6,
    numberOfOfficersResponsibleForTaxMatters = 4
  )

  def sixCompaniesHouseOfficersSelectAll: SixOrMoreOfficers = SixOrMoreOfficers(
    numberOfCompaniesHouseOfficers = 6,
    numberOfOfficersResponsibleForTaxMatters = 6
  )

  /** This is a list of individual names that we currently have stubbed in companies house, We need to use this list for fast forward links to ensure the names
    * match the names we get from companies house stub
    */
  val individualNamesStubbedInCompaniesHouse: List[IndividualName] = List(
    IndividualName("Steve Austin"),
    IndividualName("Beverly Hills"),
    IndividualName("Pauline Austin"),
    IndividualName("Justine Hills"),
    IndividualName("Steve Palmer"),
    IndividualName("Sandra Hills")
  )

  def getIndividualName(
    index: Int
  ): IndividualName = individualNamesStubbedInCompaniesHouse.lift(index).getOrElse(throw new RuntimeException(s"Missing individual name for index $index"))

  def ucrIdentifiers: UcrIdentifiers = UcrIdentifiers(
    vrns = List(vrn),
    payeRefs = List(payeRef)
  )

  def emptyUcrIdentifiers: UcrIdentifiers = UcrIdentifiers(
    vrns = List.empty,
    payeRefs = List.empty
  )

  def arn: Arn = Arn("TARN0000001")

  object riskingOutcomeApplication:

    def approved: RiskingOutcomeApplication.Approved = RiskingOutcomeApplication.Approved(
      actualDecisionDate = riskingCompletedDate
    )
    def failedFixable: RiskingOutcomeApplication.FailedFixable = RiskingOutcomeApplication.FailedFixable(
      actualDecisionDate = riskingCompletedDate,
      correctiveActionExpiryDate = correctiveActionExpiryDate,
      reSubmittedAt = None
    )
    def failedNonFixable: RiskingOutcomeApplication.FailedNonFixable = RiskingOutcomeApplication.FailedNonFixable(
      actualDecisionDate = riskingCompletedDate,
      correctiveActionExpiryDate = correctiveActionExpiryDate
    )

  def riskingOutcomeEntityFixableAmls(failure: EntityFailure.IsAmls): RiskingOutcomeEntity.FailedFixable = RiskingOutcomeEntity.FailedFixable(
    fixes = Seq(
      EntityFix._3.AmlsFix(
        failure = failure,
        isConfirmed = None,
        amlsDetails = Some(completeAmlsDetails)
      ),
      EntityFix._4._4(isConfirmed = None),
      EntityFix._5._4(isConfirmed = None)
    )
  )

  def riskingOutcomeEntityFixableNonHmrcAmls: RiskingOutcomeEntity.FailedFixable = RiskingOutcomeEntity.FailedFixable(
    fixes = Seq(
      EntityFix._3.AmlsFix(
        failure = EntityFailure._3._3,
        isConfirmed = None,
        amlsDetails = Some(completeAmlsDetailsAtt)
      )
    )
  )

  def riskingOutcomeEntityFailedFixable(isFixed: Option[Boolean] = None): RiskingOutcomeEntity.FailedFixable = RiskingOutcomeEntity.FailedFixable(
    fixes = Seq(
      EntityFix._3.AmlsFix(
        failure = EntityFailure._3._5,
        isConfirmed = isFixed,
        amlsDetails = Some(completeAmlsDetails)
      ),
      EntityFix._4._4(isConfirmed = isFixed),
      EntityFix._5._4(isConfirmed = isFixed)
    )
  )

  def riskingOutcomeEntityNewAmlsSupervisor: RiskingOutcomeEntity.FailedFixable = RiskingOutcomeEntity.FailedFixable(
    fixes = Seq(
      EntityFix._3.AmlsFix(
        failure = EntityFailure._3._5,
        isConfirmed = None,
        amlsDetails = Some(completeAmlsDetails.copy(
          supervisoryBody = AmlsSupervisoryBodyCode("ATT"),
          amlsRegistrationNumber = None
        ))
      ),
      EntityFix._4._4(isConfirmed = None),
      EntityFix._5._4(isConfirmed = None)
    )
  )

  val riskingOutcomeEntityFailedFixableAllCodes: RiskingOutcomeEntity.FailedFixable = RiskingOutcomeEntity.FailedFixable(
    fixes = Seq(
      EntityFix._4._1(isConfirmed = None),
      EntityFix._4._2(isConfirmed = None),
      EntityFix._4._3(isConfirmed = None),
      EntityFix._4._4(isConfirmed = None),
      EntityFix._5._1(isConfirmed = None),
      EntityFix._5._2(isConfirmed = None),
      EntityFix._5._3(isConfirmed = None),
      EntityFix._5._4(isConfirmed = None),
      EntityFix._5._5(isConfirmed = None),
      EntityFix._5._6(isConfirmed = None),
      EntityFix._5._7(isConfirmed = None),
      EntityFix._8._5(isConfirmed = None),
      EntityFix._8._7(isConfirmed = None)
    )
  )

  // sole traders will never have 4.2 or 5.2 failures, so we don't include them in the list of fixes for sole traders
  val riskingOutcomeEntityFailedFixableAllSoleTraderCodes: RiskingOutcomeEntity.FailedFixable = RiskingOutcomeEntity.FailedFixable(
    fixes = Seq(
      EntityFix._4._1(isConfirmed = None),
      EntityFix._4._3(isConfirmed = None),
      EntityFix._4._4(isConfirmed = None),
      EntityFix._5._1(isConfirmed = None),
      EntityFix._5._3(isConfirmed = None),
      EntityFix._5._4(isConfirmed = None),
      EntityFix._5._5(isConfirmed = None),
      EntityFix._5._6(isConfirmed = None),
      EntityFix._5._7(isConfirmed = None),
      EntityFix._8._5(isConfirmed = None),
      EntityFix._8._7(isConfirmed = None)
    )
  )

  def riskingOutcomeEntityFailedNonFixable: RiskingOutcomeEntity.FailedNonFixable = RiskingOutcomeEntity.FailedNonFixable(
    failures = Seq(
      EntityFailure._7
    )
  )

  def riskingOutcomeIndividualFixable: RiskingOutcomeIndividual.FailedFixable = RiskingOutcomeIndividual.FailedFixable(
    fixes = Seq(
      IndividualFix._4._1(isConfirmed = None)
    ),
    declarationAgreed = false
  )

  val riskingOutcomeIndividualFailedFixableAllCodes: RiskingOutcomeIndividual.FailedFixable = RiskingOutcomeIndividual.FailedFixable(
    fixes = Seq(
      IndividualFix._4._1(isConfirmed = None),
      IndividualFix._4._3(isConfirmed = None),
      IndividualFix._4._4(isConfirmed = None),
      IndividualFix._5._1(isConfirmed = None),
      IndividualFix._5._3(isConfirmed = None),
      IndividualFix._5._4(isConfirmed = None),
      IndividualFix._5._5(isConfirmed = None),
      IndividualFix._5._6(isConfirmed = None),
      IndividualFix._5._7(isConfirmed = None),
      IndividualFix._8._7(isConfirmed = None)
    ),
    declarationAgreed = false
  )

  val beforeDeclaration: RiskingOutcomeIndividual.FailedFixable = RiskingOutcomeIndividual.FailedFixable(
    fixes = Seq(
      IndividualFix._4._1(isConfirmed = Some(true)),
      IndividualFix._4._3(isConfirmed = Some(true)),
      IndividualFix._4._4(isConfirmed = Some(true)),
      IndividualFix._5._1(isConfirmed = Some(true)),
      IndividualFix._5._3(isConfirmed = Some(true)),
      IndividualFix._5._4(isConfirmed = Some(true)),
      IndividualFix._5._5(isConfirmed = Some(true)),
      IndividualFix._5._6(isConfirmed = Some(true)),
      IndividualFix._5._7(isConfirmed = Some(true)),
      IndividualFix._8._7(isConfirmed = Some(true))
    ),
    declarationAgreed = false
  )

  val riskingOutcomeIndividualDetailsFix: RiskingOutcomeIndividual.FailedFixable = RiskingOutcomeIndividual.FailedFixable(
    fixes = Seq(
      IndividualFix._10.IndividualDetailsFix(
        dateOfBirth = Some(IndividualDateOfBirth.Provided(dateOfBirth)),
        nino = Some(IndividualNino.Provided(nino)),
        saUtr = Some(IndividualSaUtr.Provided(saUtr)),
        isConfirmed = None
      )
    ),
    declarationAgreed = false
  )

  val riskingOutcomeIndividualDetailsFixMissingSaUtr: RiskingOutcomeIndividual.FailedFixable = RiskingOutcomeIndividual.FailedFixable(
    fixes = Seq(
      IndividualFix._10.IndividualDetailsFix(
        dateOfBirth = Some(IndividualDateOfBirth.Provided(dateOfBirth)),
        nino = Some(IndividualNino.Provided(nino)),
        saUtr = None,
        isConfirmed = None
      )
    ),
    declarationAgreed = false
  )
