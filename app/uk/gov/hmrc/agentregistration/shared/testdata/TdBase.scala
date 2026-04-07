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
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement.Agreed
import uk.gov.hmrc.agentregistration.shared.agentdetails.*
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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth.Provided
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Finished
import uk.gov.hmrc.agentregistration.shared.risking.PersonReference
import uk.gov.hmrc.auth.core.ConfidenceLevel

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID

trait TdBase:

  final val zoneOffset: ZoneOffset = ZoneOffset.UTC
  final val zoneId: ZoneId = ZoneId.of("UTC")

  def dateString: String = "2059-11-25"
  def timeString: String = s"${dateString}T16:33:51.880"
  def randomId: String = UUID.randomUUID().toString

  def nowAsLocalDateTime: LocalDateTime =
    // the frozen time has to be in future otherwise the applications will disappear from mongodb because of expiry index
    LocalDateTime.parse(timeString, DateTimeFormatter.ISO_DATE_TIME)

  def nowPlus6mAsLocalDateTime: LocalDateTime = nowAsLocalDateTime.plus(java.time.Period.ofMonths(6))
  def nowPlus13mAsLocalDateTime: LocalDateTime = nowAsLocalDateTime.plus(java.time.Period.ofMonths(13))
  def newPlus20sAsLocalDateTime: LocalDateTime = nowAsLocalDateTime.plusSeconds(20)

  def nowAsInstant: Instant = nowAsLocalDateTime.toInstant(ZoneOffset.UTC)
  def newInstant: Instant = nowAsInstant.plusSeconds(20) // used when a new application is created from existing one

  final val clock: Clock = Clock.fixed(nowAsInstant, zoneId)

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
  def dateOfIncorporation: LocalDate = LocalDate.now().minusYears(10)
  def personReference: PersonReference = PersonReference("1234567890")
  def applicantName: ApplicantName = ApplicantName(authorisedPersonName)
  def agentBusinessName: AgentBusinessName = AgentBusinessName(agentBusinessName = companyName, otherAgentBusinessName = None)
  def amlsCode: AmlsCode = AmlsCode("HMRC")
  def amlsRegistrationNumber: AmlsRegistrationNumber = AmlsRegistrationNumber("XAML00000123456")
  def vrn = Vrn("123456789")
  def payeRef = PayeRef("123/AB12345")
  def individualDateOfBirth: LocalDate = LocalDate.of(1980, 1, 1)
  def agentTelephoneNumber = AgentTelephoneNumber(agentTelephoneNumber = telephoneNumber.value, otherAgentTelephoneNumber = None)

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
    amlsExpiryDate = None,
    amlsEvidence = None
  )

  def agentApplicationId: AgentApplicationId = AgentApplicationId("agent-application-id-12345")

  def individualProvidedDetailsId: IndividualProvidedDetailsId = IndividualProvidedDetailsId("individual-provided-details-id-12345")
  def individualProvidedDetailsId2: IndividualProvidedDetailsId = IndividualProvidedDetailsId("individual-provided-details-id-22345")
  def individualProvidedDetailsId3: IndividualProvidedDetailsId = IndividualProvidedDetailsId("individual-provided-details-id-32345")
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
    officerRole = Some(LlpMember)
  )
  def businessPartnerRecordResponse: BusinessPartnerRecordResponse = BusinessPartnerRecordResponse(
    organisationName = Some("Test Company Name"),
    individualName = None,
    address = bprRegisteredAddress,
    primaryPhoneNumber = Some(bprPrimaryTelephoneNumber),
    emailAddress = Some(bprEmailAddress)
  )

  def businessPartnerRecordResponseSoleTrader: BusinessPartnerRecordResponse = BusinessPartnerRecordResponse(
    organisationName = None,
    individualName = Some(individualName.value),
    address = bprRegisteredAddress,
    primaryPhoneNumber = Some(bprPrimaryTelephoneNumber),
    emailAddress = Some(bprEmailAddress)
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

  val individualProvidedDetails: IndividualProvidedDetails = IndividualProvidedDetails(
    _id = individualProvidedDetailsId,
    internalUserId = None,
    createdAt = nowAsInstant,
    agentApplicationId = agentApplicationId,
    providedDetailsState = ProvidedDetailsState.Precreated,
    individualName = IndividualName("Test Name"),
    isPersonOfControl = true,
    passedIv = None
  )

  val individualProvidedDetails2: IndividualProvidedDetails = IndividualProvidedDetails(
    _id = individualProvidedDetailsId2,
    internalUserId = None,
    createdAt = nowAsInstant,
    agentApplicationId = agentApplicationId,
    providedDetailsState = ProvidedDetailsState.Precreated,
    individualName = IndividualName("Second Test Name"),
    isPersonOfControl = true,
    passedIv = None
  )

  val individualProvidedDetails3: IndividualProvidedDetails = IndividualProvidedDetails(
    _id = individualProvidedDetailsId3,
    internalUserId = None,
    createdAt = nowAsInstant,
    agentApplicationId = agentApplicationId,
    providedDetailsState = ProvidedDetailsState.Precreated,
    individualName = IndividualName("Third Test Name"),
    isPersonOfControl = true
  )

  val individualProvidedDetailsFinished: IndividualProvidedDetails = IndividualProvidedDetails(
    _id = individualProvidedDetailsId,
    individualName = individualName,
    isPersonOfControl = true,
    internalUserId = Some(internalUserId),
    createdAt = nowAsInstant,
    providedDetailsState = Finished,
    agentApplicationId = agentApplicationId,
    individualDateOfBirth = Some(Provided(individualDateOfBirth)),
    telephoneNumber = Some(telephoneNumber),
    emailAddress = Some(individualVerifiedEmailAddress),
    individualNino = Some(IndividualNino.Provided(nino)),
    individualSaUtr = Some(saUtrProvided),
    hmrcStandardForAgentsAgreed = Agreed,
    hasApprovedApplication = Some(true),
    vrns = Some(List(vrn, vrn)),
    payeRefs = Some(List(payeRef, payeRef)),
    passedIv = Some(true)
  )

  val soleTraderYetToProvideDetails: IndividualProvidedDetails = IndividualProvidedDetails(
    _id = individualProvidedDetailsId,
    internalUserId = None,
    createdAt = nowAsInstant,
    agentApplicationId = agentApplicationId,
    providedDetailsState = ProvidedDetailsState.AccessConfirmed,
    individualName = IndividualName(fullName.toStringFull),
    isPersonOfControl = true,
    telephoneNumber = Some(telephoneNumber),
    emailAddress = Some(IndividualVerifiedEmailAddress(applicantEmailAddress, isVerified = true)),
    hmrcStandardForAgentsAgreed = Agreed,
    hasApprovedApplication = Some(true),
    passedIv = None
  )

  val soleTraderProvidedDetails: IndividualProvidedDetails = IndividualProvidedDetails(
    _id = individualProvidedDetailsId,
    internalUserId = None,
    createdAt = nowAsInstant,
    agentApplicationId = agentApplicationId,
    providedDetailsState = ProvidedDetailsState.AccessConfirmed,
    individualName = IndividualName(fullName.toStringFull),
    isPersonOfControl = true,
    individualDateOfBirth = Some(dateOfBirthFromCitizenDetails),
    telephoneNumber = Some(telephoneNumber),
    emailAddress = Some(individualVerifiedEmailAddress),
    individualNino = Some(ninoFromAuth),
    individualSaUtr = Some(saUtrFromCitizenDetails),
    hmrcStandardForAgentsAgreed = Agreed,
    hasApprovedApplication = Some(true),
    passedIv = None
  )

  val soleTraderFinishedProvideDetails: IndividualProvidedDetails = soleTraderProvidedDetails.copy(
    internalUserId = Some(internalUserId),
    providedDetailsState = Finished,
    passedIv = Some(true)
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
