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

package uk.gov.hmrc.agentregistration.repository

import org.bson.BsonDocument
import org.bson.BsonType
import org.bson.BsonValue
import org.bson.conversions.Bson
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.crypto.AgentApplicationMongoFormats
import uk.gov.hmrc.agentregistration.crypto.Hasher
import uk.gov.hmrc.agentregistration.services.email.EmailRecipient
import uk.gov.hmrc.agentregistration.shared.ApplicationState.SentForRisking
import uk.gov.hmrc.agentregistration.shared.ApplicationState.SentToMinerva
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.ExpiryTransformations.*
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationStates

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class AgentApplicationRepoSpec
extends ISpec:

  private lazy val repo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

  private def repoWithClockAt(now: Instant): AgentApplicationRepo =
    new AgentApplicationRepo(
      mongoComponent = mongoComponent,
      agentApplicationMongoFormats = app.injector.instanceOf[AgentApplicationMongoFormats],
      hasher = app.injector.instanceOf[Hasher],
      clock = Clock.fixed(now, ZoneOffset.UTC),
      appConfig = app.injector.instanceOf[AppConfig]
    )

  "updateManyApplicationStateByReference should set the state in all selected applications and leave others untouched" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterDeclarationSubmitted
    val record2: AgentApplication = tdAll.agentApplicationLlp.afterDeclarationSubmitted.copy(
      _id = AgentApplicationId("agent-application-id-23456"),
      applicationReference = ApplicationReference("APPREF234"),
      internalUserId = InternalUserId("internal-user-id-23456"),
      linkId = LinkId("link-id-23456")
    )
    val record3: AgentApplication = tdAll.agentApplicationLlp.afterDeclarationSubmitted.copy(
      _id = AgentApplicationId("agent-application-id-34567"),
      applicationReference = ApplicationReference("APPREF345"),
      internalUserId = InternalUserId("internal-user-id-34567"),
      linkId = LinkId("link-id-34567")
    )
    repo.upsert(record).futureValue
    repo.upsert(record2).futureValue
    repo.upsert(record3).futureValue

    repo.findById(record.agentApplicationId).futureValue.value.applicationState shouldBe SentForRisking withClue "sanity check"

    repo.updateManyApplicationStateByReference(Seq(record.applicationReference, record2.applicationReference), SentToMinerva).futureValue

    val updatedRecord = repo.findByApplicationReference(record.applicationReference).futureValue.value
    updatedRecord.applicationState shouldBe SentToMinerva withClue "application state for record 1 should be updated"
    val updatedRecord2 = repo.findByApplicationReference(record2.applicationReference).futureValue.value
    updatedRecord2.applicationState shouldBe SentToMinerva withClue "application state for record 2 should be updated"
    val updatedRecord3 = repo.findByApplicationReference(record3.applicationReference).futureValue.value
    updatedRecord3 shouldBe record3 withClue "application state for record 3 should be untouched"

  private final case class FindTestCase(
    description: String,
    agentApplication: AgentApplication,
    expectedFound: Boolean
  )

  private final case class UpdateTestCase(
    description: String,
    agentApplication: AgentApplication,
    expectedModifiedCount: Long,
    expectedAgentApplicationAfterRun: AgentApplication
  )

  BusinessType.values.foreach: businessType =>
    lazy val agentApplicationStates: AgentApplicationStates = AgentApplicationStates.forBusinessType(businessType)

    Seq(
      FindTestCase(
        description = "finds an application with a verified email address expiring within the reminder period",
        agentApplication = agentApplicationStates.withVerifiedEmail.makeDueForExpiry,
        expectedFound = true
      ),
      FindTestCase(
        description = "does not find a Started application with no verified email address",
        agentApplication = agentApplicationStates.started.makeDueForExpiry,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a GrsDataReceived application with no verified email address",
        agentApplication = agentApplicationStates.grsDataReceived.makeDueForExpiry,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a SentForRisking application",
        agentApplication = agentApplicationStates.sentForRisking,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a SentToMinerva application",
        agentApplication = agentApplicationStates.sentToMinerva,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a RiskingCompleted application",
        agentApplication = agentApplicationStates.riskingCompleted,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find an Expired application",
        agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpired,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find an application already sent the reminder",
        agentApplication = agentApplicationStates.withVerifiedEmail.makeExpiryReminderEmailSent,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find an application expiring after the reminder period",
        agentApplication = agentApplicationStates.withVerifiedEmail,
        expectedFound = false
      ),
      // TODO: confirm with BA whether a long overdue application should be sent the reminder before it expires
      FindTestCase(
        description = "does not find an application whose expiry date has passed",
        agentApplication = agentApplicationStates.withVerifiedEmail.makeLongOverdue,
        expectedFound = false
      )
    ).foreach: testCase =>
      s"findUnsubmittedApplicationsForExpiryReminderEmail ${testCase.description} ($businessType)" in:
        repo.upsert(testCase.agentApplication).futureValue

        repo.findUnsubmittedApplicationsForExpiryReminderEmail().futureValue.contains(testCase.agentApplication) shouldBe testCase.expectedFound

    s"findUnsubmittedApplicationsForExpiryReminderEmail finds an application only while it has 10 days or less left until its expiry date ($businessType)" in:
      val dueForExpiry: AgentApplication = agentApplicationStates.withVerifiedEmail.makeDueForExpiry
      val expiresAt: Instant = dueForExpiry.applicationExpiresAt.value
      repo.upsert(dueForExpiry).futureValue

      def isReadyForExpiryReminderEmail(now: Instant): Boolean = repoWithClockAt(now)
        .findUnsubmittedApplicationsForExpiryReminderEmail()
        .futureValue
        .contains(dueForExpiry)

      isReadyForExpiryReminderEmail(now = expiresAt.minus(Duration.ofDays(11))) shouldBe false withClue "11 days left, too early for a reminder"
      isReadyForExpiryReminderEmail(now = expiresAt.minus(Duration.ofDays(10))) shouldBe true withClue "10 days left, the reminder period starts"
      isReadyForExpiryReminderEmail(now = expiresAt.minus(Duration.ofDays(3))) shouldBe true withClue "3 days left, inside the reminder period"
      isReadyForExpiryReminderEmail(now = expiresAt) shouldBe true withClue "expiring now, the last moment of the reminder period"
      isReadyForExpiryReminderEmail(now = expiresAt.plusSeconds(1)) shouldBe false withClue "expiry date passed, too late for a reminder"

    Seq(
      UpdateTestCase(
        description = "expires a Started application whose expiry date has passed",
        agentApplication = agentApplicationStates.started.makeOverdue,
        expectedModifiedCount = 1L,
        expectedAgentApplicationAfterRun = agentApplicationStates.started.makeOverdue.makeExpired
      ),
      UpdateTestCase(
        description = "expires a GrsDataReceived application whose expiry date has passed",
        agentApplication = agentApplicationStates.grsDataReceived.makeOverdue,
        expectedModifiedCount = 1L,
        expectedAgentApplicationAfterRun = agentApplicationStates.grsDataReceived.makeOverdue.makeExpired
      ),
      UpdateTestCase(
        description = "does not expire a SentForRisking application",
        agentApplication = agentApplicationStates.sentForRisking,
        expectedModifiedCount = 0L,
        expectedAgentApplicationAfterRun = agentApplicationStates.sentForRisking
      ),
      UpdateTestCase(
        description = "does not expire a SentToMinerva application",
        agentApplication = agentApplicationStates.sentToMinerva,
        expectedModifiedCount = 0L,
        expectedAgentApplicationAfterRun = agentApplicationStates.sentToMinerva
      ),
      UpdateTestCase(
        description = "does not expire a RiskingCompleted application",
        agentApplication = agentApplicationStates.riskingCompleted,
        expectedModifiedCount = 0L,
        expectedAgentApplicationAfterRun = agentApplicationStates.riskingCompleted
      ),
      UpdateTestCase(
        description = "does not change an Expired application",
        agentApplication = agentApplicationStates.expired,
        expectedModifiedCount = 0L,
        expectedAgentApplicationAfterRun = agentApplicationStates.expired
      ),
      UpdateTestCase(
        description = "does not expire a Started application whose expiry date is in the future",
        agentApplication = agentApplicationStates.started,
        expectedModifiedCount = 0L,
        expectedAgentApplicationAfterRun = agentApplicationStates.started
      ),
      UpdateTestCase(
        description = "does not expire a GrsDataReceived application whose expiry date is in the future",
        agentApplication = agentApplicationStates.grsDataReceived,
        expectedModifiedCount = 0L,
        expectedAgentApplicationAfterRun = agentApplicationStates.grsDataReceived
      )
    ).foreach: testCase =>
      s"expireUnsubmittedApplications ${testCase.description} ($businessType)" in:
        repo.upsert(testCase.agentApplication).futureValue

        repo.expireUnsubmittedApplications().futureValue shouldBe testCase.expectedModifiedCount

        repo.findById(testCase.agentApplication.agentApplicationId).futureValue.value shouldBe testCase.expectedAgentApplicationAfterRun

    s"expireUnsubmittedApplications returns the number of applications it expired ($businessType)" in:
      repo.upsert(agentApplicationStates.started.makeOverdue).futureValue

      repo.expireUnsubmittedApplications().futureValue shouldBe 1L
      repo.expireUnsubmittedApplications().futureValue shouldBe 0L withClue
        "a second run has nothing left to expire"

    Seq(
      FindTestCase(
        description = "does not find a Started application",
        agentApplication = agentApplicationStates.started,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a GrsDataReceived application",
        agentApplication = agentApplicationStates.grsDataReceived,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a SentForRisking application",
        agentApplication = agentApplicationStates.sentForRisking,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a SentToMinerva application",
        agentApplication = agentApplicationStates.sentToMinerva,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a RiskingCompleted application",
        agentApplication = agentApplicationStates.riskingCompleted,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find an unsubmitted application with a verified email address",
        agentApplication = agentApplicationStates.withVerifiedEmail,
        expectedFound = false
      ),
      FindTestCase(
        description = "finds an Expired application with a verified email address still in the grace period",
        agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpired,
        expectedFound = true
      ),
      FindTestCase(
        description = "does not find an Expired application with no verified email address",
        agentApplication = agentApplicationStates.expired,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find an Expired application whose grace period has ended",
        agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpired.makeDueForDeletion,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find an Expired application already sent the expired email",
        agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpiredEmailSent,
        expectedFound = false
      )
    ).foreach: testCase =>
      s"findExpiredReadyForExpiredEmail ${testCase.description} ($businessType)" in:
        repo.upsert(testCase.agentApplication).futureValue

        repo.findExpiredReadyForExpiredEmail().futureValue.contains(testCase.agentApplication) shouldBe testCase.expectedFound

    Seq(
      FindTestCase(
        description = "does not find a Started application",
        agentApplication = agentApplicationStates.started,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a GrsDataReceived application",
        agentApplication = agentApplicationStates.grsDataReceived,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a SentForRisking application",
        agentApplication = agentApplicationStates.sentForRisking,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a SentToMinerva application",
        agentApplication = agentApplicationStates.sentToMinerva,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a RiskingCompleted application",
        agentApplication = agentApplicationStates.riskingCompleted,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find an unsubmitted application with a verified email address",
        agentApplication = agentApplicationStates.withVerifiedEmail,
        expectedFound = false
      ),
      FindTestCase(
        description = "finds an Expired application with a verified email address whose grace period has ended",
        agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpired.makeDueForDeletion,
        expectedFound = true
      ),
      FindTestCase(
        description = "does not find an Expired application with no verified email address",
        agentApplication = agentApplicationStates.expired.makeDueForDeletion,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find an Expired application still in the grace period",
        agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpired,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find an Expired application already sent the deletion email",
        agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpiredEmailSent.makeDeletionEmailSent,
        expectedFound = false
      )
    ).foreach: testCase =>
      s"findExpiredReadyForDeletionEmail ${testCase.description} ($businessType)" in:
        repo.upsert(testCase.agentApplication).futureValue

        repo.findExpiredReadyForDeletionEmail().futureValue.contains(testCase.agentApplication) shouldBe testCase.expectedFound

    Seq(
      FindTestCase(
        description = "does not find a Started application",
        agentApplication = agentApplicationStates.started,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a GrsDataReceived application",
        agentApplication = agentApplicationStates.grsDataReceived,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a SentForRisking application",
        agentApplication = agentApplicationStates.sentForRisking,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a SentToMinerva application",
        agentApplication = agentApplicationStates.sentToMinerva,
        expectedFound = false
      ),
      FindTestCase(
        description = "does not find a RiskingCompleted application",
        agentApplication = agentApplicationStates.riskingCompleted,
        expectedFound = false
      ),
      FindTestCase(
        description = "finds an Expired application sent the deletion email",
        agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpiredEmailSent.makeDeletionEmailSent,
        expectedFound = true
      ),
      FindTestCase(
        description = "finds an Expired application with no verified email address whose grace period has ended",
        agentApplication = agentApplicationStates.expired.makeDueForDeletion,
        expectedFound = true
      ),
      FindTestCase(
        description = "finds an Expired application with a verified email address not yet sent the deletion email",
        agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpiredEmailSent.makeDueForDeletion,
        expectedFound = true
      ),
      FindTestCase(
        description = "does not find an Expired application still in the grace period",
        agentApplication = agentApplicationStates.expired,
        expectedFound = false
      )
    ).foreach: testCase =>
      s"findExpiredDueForDeletion ${testCase.description} ($businessType)" in:
        repo.upsert(testCase.agentApplication).futureValue

        repo.findExpiredDueForDeletion().futureValue.contains(testCase.agentApplication) shouldBe testCase.expectedFound

  private final case class VerifiedEmailTestCase(
    description: String,
    agentApplication: AgentApplication,
    hasVerifiedEmailAddress: Boolean
  )

  Seq(
    VerifiedEmailTestCase(
      description = "no applicant contact details",
      agentApplication = tdAll.agentApplicationLlp.afterStarted,
      hasVerifiedEmailAddress = false
    ),
    VerifiedEmailTestCase(
      description = "an unverified applicant email address",
      agentApplication = tdAll.agentApplicationLlp.afterContactDetailsWithUnverifiedEmail,
      hasVerifiedEmailAddress = false
    ),
    VerifiedEmailTestCase(
      description = "a verified applicant email address",
      agentApplication = tdAll.agentApplicationLlp.afterContactDetailsComplete,
      hasVerifiedEmailAddress = true
    ),
    VerifiedEmailTestCase(
      description = "a verified applicant email address and an unverified agent email address",
      agentApplication = tdAll.agentApplicationLlp.afterAgentDetailsWithUnverifiedEmail,
      hasVerifiedEmailAddress = true
    ),
    VerifiedEmailTestCase(
      description = "a verified agent email address",
      agentApplication = tdAll.agentApplicationLlp.afterAgentDetailsComplete,
      hasVerifiedEmailAddress = true
    ),
    VerifiedEmailTestCase(
      description = "a verified agent email address different from the applicant email address",
      agentApplication = tdAll.agentApplicationLlp.afterAgentDetailsCompleteWithOtherAgentEmail,
      hasVerifiedEmailAddress = true
    )
  ).foreach: testCase =>
    s"findExpiredReadyForExpiredEmail agrees with EmailRecipient.resolve for an application with ${testCase.description}" in:
      val expired: AgentApplication = testCase.agentApplication.makeOverdue.makeExpired
      EmailRecipient.resolve(expired).isDefined shouldBe testCase.hasVerifiedEmailAddress
      repo.upsert(expired).futureValue

      repo.findExpiredReadyForExpiredEmail().futureValue.contains(expired) shouldBe testCase.hasVerifiedEmailAddress

  // the fields the Mongo format stores as BSON Date; the records below between them carry all of them
  private val bsonDateFieldNames: Seq[String] = Seq(
    FieldNames.createdAt,
    FieldNames.applicationExpiresAt,
    "submittedAt",
    "riskingOutcomeApplication.actualDecisionDate",
    "riskingOutcomeApplication.reSubmittedAt",
    "riskingOutcomeApplication.correctiveActionExpiryDate",
    FieldNames.postExpiryGracePeriodEndsAt,
    FieldNames.expiredAt,
    FieldNames.expiryReminderEmailSentAt,
    FieldNames.expiredEmailSentAt,
    FieldNames.deletionEmailSentAt
  )

  private val agentApplicationWithEveryEmailSent: AgentApplication =
    tdAll.agentApplicationLlp.afterStarted.makeOverdue.makeExpiredEmailSent.makeDeletionEmailSent

  private val recordsWithDates: Seq[(String, AgentApplication)] = Seq(
    "afterStarted" -> tdAll.agentApplicationLlp.afterStarted,
    "afterResubmitted" -> tdAll.agentApplicationLlp.afterResubmitted,
    "afterExpired" -> tdAll.agentApplicationLlp.afterStarted.makeOverdue.makeExpired,
    "afterDeletionEmailSent" -> agentApplicationWithEveryEmailSent
  )

  private def rawDocument(record: AgentApplication): BsonDocument =
    repo
      .collection
      .withDocumentClass[BsonDocument]()
      .find(Filters.eq(FieldNames._id, record.agentApplicationId.value))
      .headOption()
      .futureValue
      .value

  private def rawValue(
    document: BsonDocument,
    fieldName: String
  ): Option[BsonValue] =
    fieldName.split('.').foldLeft(Option[BsonValue](document)): (value, name) =>
      value.filter(_.isDocument).flatMap(parent => Option(parent.asDocument().get(name)))

  private def storedBsonDateFieldNames(record: AgentApplication): Seq[String] =
    val document: BsonDocument = rawDocument(record)
    bsonDateFieldNames.filter(fieldName => rawValue(document, fieldName).isDefined)

  recordsWithDates.foreach: (name, record) =>
    s"upsert stores every date field of $name as BSON Date, not as a string" in:
      repo.upsert(record).futureValue
      val document: BsonDocument = rawDocument(record)
      val storedFieldNames: Seq[String] = storedBsonDateFieldNames(record)

      storedFieldNames should not be empty
      storedFieldNames.foreach: fieldName =>
        withClue(s"$fieldName: "):
          rawValue(document, fieldName).value.getBsonType shouldBe BsonType.DATE_TIME

  "the records with dates between them store every date field" in:
    // the states share an _id, so each is upserted and inspected in turn
    val storedFieldNames: Set[String] =
      recordsWithDates.flatMap: (_, record) =>
        repo.upsert(record).futureValue
        storedBsonDateFieldNames(record)
      .toSet

    storedFieldNames shouldBe bsonDateFieldNames.toSet

  private final case class DateQueryCase(
    description: String,
    record: AgentApplication,
    fieldName: String,
    storedInstant: Instant
  )

  private val dateQueryCases: Seq[DateQueryCase] = Seq(
    DateQueryCase(
      description = "a started application",
      record = tdAll.agentApplicationLlp.afterStarted,
      fieldName = FieldNames.createdAt,
      storedInstant = tdAll.createdAt
    ),
    DateQueryCase(
      description = "a started application",
      record = tdAll.agentApplicationLlp.afterStarted,
      fieldName = FieldNames.applicationExpiresAt,
      storedInstant = tdAll.applicationExpiresAt
    ),
    DateQueryCase(
      description = "a submitted application",
      record = tdAll.agentApplicationLlp.afterDeclarationSubmitted,
      fieldName = "submittedAt",
      storedInstant = tdAll.submittedAt
    ),
    DateQueryCase(
      description = "an application with a fixable outcome",
      record = tdAll.agentApplicationLlp.afterRiskingCompletedFixable,
      fieldName = "riskingOutcomeApplication.actualDecisionDate",
      storedInstant = tdAll.riskingCompletedDate.atStartOfDay(ZoneOffset.UTC).toInstant
    ),
    DateQueryCase(
      description = "an application with a fixable outcome",
      record = tdAll.agentApplicationLlp.afterRiskingCompletedFixable,
      fieldName = "riskingOutcomeApplication.correctiveActionExpiryDate",
      storedInstant = tdAll.correctiveActionExpiryDate.atStartOfDay(ZoneOffset.UTC).toInstant
    ),
    DateQueryCase(
      description = "a resubmitted application",
      record = tdAll.agentApplicationLlp.afterResubmitted,
      fieldName = "riskingOutcomeApplication.reSubmittedAt",
      storedInstant = tdAll.reSubmittedAt
    ),
    DateQueryCase(
      description = "an expired application",
      record = tdAll.agentApplicationLlp.afterStarted.makeOverdue.makeExpired,
      fieldName = FieldNames.postExpiryGracePeriodEndsAt,
      storedInstant = tdAll.postExpiryGracePeriodEndsAt
    ),
    DateQueryCase(
      description = "an expired application",
      record = tdAll.agentApplicationLlp.afterStarted.makeOverdue.makeExpired,
      fieldName = FieldNames.expiredAt,
      storedInstant = tdAll.expiredAt
    ),
    DateQueryCase(
      description = "an application sent every email",
      record = agentApplicationWithEveryEmailSent,
      fieldName = FieldNames.expiryReminderEmailSentAt,
      storedInstant = agentApplicationWithEveryEmailSent.expiryReminderEmailSentAt.value
    ),
    DateQueryCase(
      description = "an application sent every email",
      record = agentApplicationWithEveryEmailSent,
      fieldName = FieldNames.expiredEmailSentAt,
      storedInstant = agentApplicationWithEveryEmailSent.expiredEmailSentAt.value
    ),
    DateQueryCase(
      description = "an application sent every email",
      record = agentApplicationWithEveryEmailSent,
      fieldName = FieldNames.deletionEmailSentAt,
      storedInstant = agentApplicationWithEveryEmailSent.deletionEmailSentAt.value
    )
  )

  private def countWhere(filter: Bson): Long =
    repo
      .collection
      .countDocuments(filter)
      .toFuture()
      .futureValue

  dateQueryCases.foreach: dateQueryCase =>
    s"${dateQueryCase.fieldName} of ${dateQueryCase.description} can be queried with Mongo date operators" in:
      repo.upsert(dateQueryCase.record).futureValue

      countWhere(Filters.lt(dateQueryCase.fieldName, dateQueryCase.storedInstant.plusMillis(1))) shouldBe 1L withClue "a later date matches"
      countWhere(Filters.lt(dateQueryCase.fieldName, dateQueryCase.storedInstant)) shouldBe 0L withClue "the stored date itself is not less than itself"
