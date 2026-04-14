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

package uk.gov.hmrc.agentregistration.testOnly.controllers

import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Sorts
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistration.action.Actions
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.ApplicationState.Started
import uk.gov.hmrc.agentregistration.shared.CheckResult.Pass
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.individual.*
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.risking.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.PersonReferenceGenerator
import uk.gov.hmrc.agentregistration.shared.upload.UploadId
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton()
class TestApplicationController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  agentApplicationRepo: AgentApplicationRepo,
  agentApplicationIdGenerator: AgentApplicationIdGenerator,
  individualProvidedDetailsRepo: IndividualProvidedDetailsRepo,
  individualProvidedDetailsIdGenerator: IndividualProvidedDetailsIdGenerator,
  personReferenceGenerator: PersonReferenceGenerator
)
extends BackendController(cc):

  given ExecutionContext = controllerComponents.executionContext

  private enum SmuScenario:

    case Full
    case Partial

  private object SmuScenario:
    def fromQuery(value: Option[String]): SmuScenario =
      value.map(_.trim.toLowerCase) match
        case Some("partial") => SmuScenario.Partial
        case _ => SmuScenario.Full

  private def isFullScenario(scenario: SmuScenario): Boolean =
    scenario match
      case SmuScenario.Full => true
      case SmuScenario.Partial => false

  def upsertIndividualProvidedDetails: Action[IndividualProvidedDetails] =
    actions
      .default
      .async(parse.json[IndividualProvidedDetails]):
        implicit request =>
          val individualProvidedDetails: IndividualProvidedDetails = request.body
          individualProvidedDetailsRepo
            .upsert(request.body)
            .map(_ => Ok(""))

  def upsertApplication: Action[AgentApplication] =
    actions
      .default
      .async(parse.json[AgentApplication]):
        implicit request =>
          val agentApplication: AgentApplication = request.body
          agentApplicationRepo
            .upsert(request.body)
            .map(_ => Ok(""))

  def recentApplications: Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        agentApplicationRepo
          .collection
          .find()
          .sort(Sorts.descending("createdAt"))
          .limit(15)
          .toFuture()
          .map((recentApplications: Seq[AgentApplication]) => Ok(Json.toJson(recentApplications)))

  def findApplication(agentApplicationId: AgentApplicationId): Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        agentApplicationRepo
          .findById(agentApplicationId)
          .map:
            case Some(agentApplication) => Ok(Json.toJson(agentApplication))
            case None => NoContent

  def findIndividuals(agentApplicationId: AgentApplicationId): Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        individualProvidedDetailsRepo
          .findForApplication(agentApplicationId)
          .map: individuals =>
            Ok(Json.toJson(individuals))

  def findIndividual(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        individualProvidedDetailsRepo
          .findById(individualProvidedDetailsId)
          .map:
            case Some(individualProvidedDetails) => Ok(Json.toJson(individualProvidedDetails))
            case None => NoContent

  def createTestSmuIndividual(scenario: Option[String]): Action[AnyContent] = Action
    .async:
      implicit request =>
        val resolvedScenario = SmuScenario.fromQuery(scenario)
        val agentApplication: AgentApplication = makeApplicationToProvideDetailsFor(resolvedScenario, applicationState = ApplicationState.SentForRisking)
        val individual: IndividualProvidedDetails = makeIndividualProvidedDetailsFor(resolvedScenario, agentApplication.agentApplicationId)
        for
          _ <- agentApplicationRepo.upsert(agentApplication)
          _ <- individualProvidedDetailsRepo.upsert(individual)
        yield Created(Json.obj(
          "scenario" -> resolvedScenario.toString.toLowerCase,
          "personReference" -> individual.personReference.value,
          "individualProvidedDetailsId" -> individual.individualProvidedDetailsId.value
        ))

  def deleteTestSmuIndividual(personReference: PersonReference): Action[AnyContent] = Action
    .async:
      implicit request =>
        for
          maybeIndividual <- individualProvidedDetailsRepo.findByPersonReference(personReference)
          applicationId = maybeIndividual.map(_.agentApplicationId).getOrThrowExpectedDataMissing("agentApplicationId")
          individualId = maybeIndividual.map(_.individualProvidedDetailsId).getOrThrowExpectedDataMissing("individualProvidedDetailsId")
          maybeAaRes <- agentApplicationRepo.removeById(applicationId)
          maybeIpdRes <- individualProvidedDetailsRepo.removeById(individualId)
        yield (maybeAaRes, maybeIpdRes) match
          case (Some(aaRes), Some(ipdRes)) if aaRes.wasAcknowledged() && ipdRes.wasAcknowledged() => NoContent
          case _ => BadRequest("Something went wrong")

  def createTestApplication: Action[AnyContent] = Action
    .async:
      implicit request =>
        val agentApplication: AgentApplication = makeApplicationToProvideDetailsFor(SmuScenario.Full)
        agentApplicationRepo
          .upsert(agentApplication)
          .map(_ => Ok(Json.obj("linkId" -> agentApplication.linkId.value)))

  // TODO: We should revisit the way that we handle the stubbing here after we have brought test data into the shared space
  private def makeApplicationToProvideDetailsFor(
    scenario: SmuScenario,
    applicationState: ApplicationState = Started
  ): AgentApplication = AgentApplicationLlp(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    linkId = LinkId(value = UUID.randomUUID().toString),
    internalUserId = InternalUserId(value = s"test-${UUID.randomUUID().toString}"),
    applicantCredentials = Credentials(
      providerId = s"test-provider-id-${UUID.randomUUID().toString}",
      providerType = s"test-provider-type-${UUID.randomUUID().toString}"
    ),
    groupId = GroupId(value = UUID.randomUUID().toString),
    createdAt = Instant.now(),
    submittedAt = Some(Instant.now()),
    applicationState = applicationState, // Provide details journeys now happen before an application is finished
    userRole = Some(UserRole.Authorised),
    businessDetails = Some(BusinessDetailsLlp(
      safeId = SafeId("safe-id-12345"),
      saUtr = SaUtr("1234567890"),
      companyProfile = CompanyProfile(
        companyNumber = Crn("12345566"),
        companyName = "Test Partnership LLP",
        dateOfIncorporation = Option.when(isFullScenario(scenario))(LocalDate.of(2015, 6, 1)),
        unsanitisedCHROAddress = None
      )
    )),
    applicantContactDetails = Some(ApplicantContactDetails(
      applicantName = ApplicantName("Bob Ross"),
      telephoneNumber = Some(TelephoneNumber("1234658979")),
      applicantEmailAddress = Some(ApplicantEmailAddress(
        emailAddress = EmailAddress("user@test.com"),
        isVerified = true
      ))
    )),
    amlsDetails =
      Option.when(isFullScenario(scenario)) {
        AmlsDetails(
          supervisoryBody = AmlsCode("HMRC"),
          amlsRegistrationNumber = Some(AmlsRegistrationNumber("XAML1234567890")),
          amlsExpiryDate = Some(LocalDate.of(2027, 1, 31)),
          amlsEvidence = Some(AmlsEvidence(
            uploadId = UploadId("smu-test-evidence-123"),
            fileName = "certificate.pdf",
            objectStoreLocation = Path.File("/test/certificate.pdf")
          ))
        )
      },
    agentDetails = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed,
    numberOfIndividuals = None,
    hasOtherRelevantIndividuals = None,
    refusalToDealWithCheckResult = Some(Pass),
    companyStatusCheckResult = Some(Pass),
    vrns = Option.when(isFullScenario(scenario))(List(Vrn("12341234"), Vrn("43214321"))),
    payeRefs = Option.when(isFullScenario(scenario))(List(PayeRef("56785678"), PayeRef("87658765")))
  )
  // TODO: Same as makeApplicationToProvideDetailsFor, we should use test data here or create FF links to populate this data
  private def makeIndividualProvidedDetailsFor(
    scenario: SmuScenario,
    agentApplicationId: AgentApplicationId
  ): IndividualProvidedDetails = IndividualProvidedDetails(
    _id = individualProvidedDetailsIdGenerator.nextIndividualProvidedDetailsId(),
    personReference = personReferenceGenerator.nextPersonReference(),
    individualName = IndividualName("George Smiley"),
    isPersonOfControl = true,
    internalUserId = Some(InternalUserId(value = s"test-${UUID.randomUUID().toString}")),
    createdAt = Instant.now(),
    providedDetailsState = ProvidedDetailsState.Finished,
    agentApplicationId = agentApplicationId,
    individualDateOfBirth = Option.when(isFullScenario(scenario))(IndividualDateOfBirth.Provided(LocalDate.of(1980, 1, 1))),
    telephoneNumber = Some(TelephoneNumber("1234658979")),
    emailAddress = Option.when(isFullScenario(scenario))(IndividualVerifiedEmailAddress(EmailAddress("g.smiley@test.com"), isVerified = true)),
    individualNino = Option.when(isFullScenario(scenario))(IndividualNino.Provided(Nino("AA123456A"))),
    individualSaUtr = Option.when(isFullScenario(scenario))(IndividualSaUtr.Provided(SaUtr("1234567890"))),
    hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed,
    hasApprovedApplication = Some(true),
    vrns = Option.when(isFullScenario(scenario))(List(Vrn("12341234"), Vrn("43214321"))),
    payeRefs = Option.when(isFullScenario(scenario))(List(PayeRef("56785678"), PayeRef("87658765"))),
    passedIv = Option.when(isFullScenario(scenario))(true)
  )
