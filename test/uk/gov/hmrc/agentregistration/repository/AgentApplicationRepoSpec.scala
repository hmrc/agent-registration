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
import uk.gov.hmrc.agentregistration.shared.ApplicationState.Expired
import uk.gov.hmrc.agentregistration.shared.ApplicationState.GrsDataReceived
import uk.gov.hmrc.agentregistration.shared.ApplicationState.RiskingCompleted
import uk.gov.hmrc.agentregistration.shared.ApplicationState.SentForRisking
import uk.gov.hmrc.agentregistration.shared.ApplicationState.SentToMinerva
import uk.gov.hmrc.agentregistration.shared.ApplicationState.Started
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.ExpiryTransformations.*
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationPreconditions.*
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationStates

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class AgentApplicationRepoSpec
extends ISpec:

  private lazy val repo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

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

  BusinessType.values.foreach: businessType =>
    s"for a $businessType application" - {

      lazy val agentApplication: AgentApplicationStates = AgentApplicationStates.forBusinessType(businessType)

      "findReadyForExpiryReminderEmail finds only Started and GrsDataReceived applications expiring within the reminder period" in:
        def isReadyForExpiryReminderEmail(agentApplication: AgentApplication): Boolean =
          dropDatabase()
          repo.upsert(agentApplication).futureValue
          repo.findReadyForExpiryReminderEmail(tdAll.now, expiryReminderPeriodEndsAt).futureValue.contains(agentApplication)

        ApplicationState.values.foreach: state =>
          withClue(s"state=$state: "):
            state match
              case Started => isReadyForExpiryReminderEmail(agentApplication.started.makeDueForExpiry) shouldBe true
              case GrsDataReceived => isReadyForExpiryReminderEmail(agentApplication.grsDataReceived.makeDueForExpiry) shouldBe true
              case SentForRisking => isReadyForExpiryReminderEmail(agentApplication.sentForRisking) shouldBe false
              case SentToMinerva => isReadyForExpiryReminderEmail(agentApplication.sentToMinerva) shouldBe false
              case RiskingCompleted => isReadyForExpiryReminderEmail(agentApplication.riskingCompleted) shouldBe false
              case Expired => isReadyForExpiryReminderEmail(agentApplication.expired) shouldBe false

      "findReadyForExpiryReminderEmail finds an application only while it has 10 days or less left until its expiry date" in:
        agentApplication.havingExpiryDate.foreach: havingExpiryDate =>
          dropDatabase()
          val dueForExpiry: AgentApplication = havingExpiryDate.makeDueForExpiry
          val expiresAt: Instant = dueForExpiry.applicationExpiresAt.value
          repo.upsert(dueForExpiry).futureValue

          def isReadyForExpiryReminderEmail(now: Instant): Boolean = repo.findReadyForExpiryReminderEmail(
            now,
            now.plus(tdAll.expiryReminderPeriod)
          ).futureValue.contains(dueForExpiry)

          withClue(s"state=${havingExpiryDate.applicationState}: "):
            isReadyForExpiryReminderEmail(now = expiresAt.minus(Duration.ofDays(11))) shouldBe false withClue "11 days left, too early for a reminder"
            isReadyForExpiryReminderEmail(now = expiresAt.minus(Duration.ofDays(10))) shouldBe true withClue "10 days left, the reminder period starts"
            isReadyForExpiryReminderEmail(now = expiresAt.minus(Duration.ofDays(3))) shouldBe true withClue "3 days left, inside the reminder period"
            isReadyForExpiryReminderEmail(now = expiresAt) shouldBe true withClue "expiring now, the last moment of the reminder period"
            isReadyForExpiryReminderEmail(now = expiresAt.plusSeconds(1)) shouldBe false withClue "expiry date passed, too late for a reminder"

      "findReadyForExpiryReminderEmail does not find an application already sent the reminder" in:
        agentApplication.havingExpiryDate.foreach: havingExpiryDate =>
          dropDatabase()
          val expiryReminderEmailSent: AgentApplication = havingExpiryDate.makeExpiryReminderEmailSent
          expiryReminderEmailSent.expiryReminderEmailSentAt shouldBe defined withClue "precondition: the reminder must already be recorded"
          expiryReminderEmailSent.shouldHaveExpiryDateWithinReminderPeriod
          repo.upsert(expiryReminderEmailSent).futureValue

          repo.findReadyForExpiryReminderEmail(tdAll.now, expiryReminderPeriodEndsAt).futureValue shouldBe empty withClue
            s"state=${havingExpiryDate.applicationState}"

      "findReadyForExpiryReminderEmail does not find an application expiring after the reminder period" in:
        agentApplication.havingExpiryDate.foreach: havingExpiryDate =>
          dropDatabase()
          havingExpiryDate.shouldHaveExpiryDateAfterReminderPeriod
          repo.upsert(havingExpiryDate).futureValue

          repo.findReadyForExpiryReminderEmail(tdAll.now, expiryReminderPeriodEndsAt).futureValue shouldBe empty withClue
            s"state=${havingExpiryDate.applicationState}"

      "findReadyForExpiryReminderEmail does not find an application whose expiry date has passed" in:
        agentApplication.havingExpiryDate.foreach: havingExpiryDate =>
          dropDatabase()
          val longOverdue: AgentApplication = havingExpiryDate.makeLongOverdue
          longOverdue.shouldHaveExpiryDateInThePast
          longOverdue.expiryReminderEmailSentAt shouldBe None withClue "precondition: the reminder must not be recorded, so only the date keeps it out"
          repo.upsert(longOverdue).futureValue

          repo.findReadyForExpiryReminderEmail(tdAll.now, expiryReminderPeriodEndsAt).futureValue shouldBe empty withClue
            s"state=${havingExpiryDate.applicationState}"

      "updateManyToExpiredForUnsubmitted expires only Started and GrsDataReceived applications whose expiry date has passed" in:
        def afterUpdateManyToExpiredForUnsubmitted(agentApplication: AgentApplication): AgentApplication =
          dropDatabase()
          repo.upsert(agentApplication).futureValue
          repo.updateManyToExpiredForUnsubmitted(tdAll.now, tdAll.postExpiryGracePeriodEndsAt).futureValue
          repo.findById(agentApplication.agentApplicationId).futureValue.value

        val overdueStarted: AgentApplication = agentApplication.started.makeOverdue
        val overdueGrsDataReceived: AgentApplication = agentApplication.grsDataReceived.makeOverdue

        ApplicationState.values.foreach: state =>
          withClue(s"state=$state: "):
            state match
              case Started => afterUpdateManyToExpiredForUnsubmitted(overdueStarted) shouldBe overdueStarted.makeExpired
              case GrsDataReceived => afterUpdateManyToExpiredForUnsubmitted(overdueGrsDataReceived) shouldBe overdueGrsDataReceived.makeExpired
              case SentForRisking => afterUpdateManyToExpiredForUnsubmitted(agentApplication.sentForRisking) shouldBe agentApplication.sentForRisking
              case SentToMinerva => afterUpdateManyToExpiredForUnsubmitted(agentApplication.sentToMinerva) shouldBe agentApplication.sentToMinerva
              case RiskingCompleted => afterUpdateManyToExpiredForUnsubmitted(agentApplication.riskingCompleted) shouldBe agentApplication.riskingCompleted
              case Expired => afterUpdateManyToExpiredForUnsubmitted(agentApplication.expired) shouldBe agentApplication.expired

      "updateManyToExpiredForUnsubmitted does not expire an application whose expiry date is in the future" in:
        agentApplication.havingExpiryDate.foreach: havingExpiryDate =>
          dropDatabase()
          havingExpiryDate.shouldHaveExpiryDateInTheFuture
          repo.upsert(havingExpiryDate).futureValue

          repo.updateManyToExpiredForUnsubmitted(tdAll.now, tdAll.postExpiryGracePeriodEndsAt).futureValue shouldBe 0L withClue
            s"state=${havingExpiryDate.applicationState}"

          repo.findById(havingExpiryDate.agentApplicationId).futureValue.value shouldBe havingExpiryDate

      "updateManyToExpiredForUnsubmitted returns the number of applications it expired" in:
        repo.upsert(agentApplication.started.makeOverdue).futureValue

        repo.updateManyToExpiredForUnsubmitted(tdAll.now, tdAll.postExpiryGracePeriodEndsAt).futureValue shouldBe 1L
        repo.updateManyToExpiredForUnsubmitted(tdAll.now, tdAll.postExpiryGracePeriodEndsAt).futureValue shouldBe 0L withClue
          "a second run has nothing left to expire"

      "findReadyForExpiredEmail finds only Expired applications still in the grace period" in:
        def isReadyForExpiredEmail(agentApplication: AgentApplication): Boolean =
          dropDatabase()
          repo.upsert(agentApplication).futureValue
          repo.findReadyForExpiredEmail(tdAll.now).futureValue.contains(agentApplication)

        ApplicationState.values.foreach: state =>
          withClue(s"state=$state: "):
            state match
              case Started => isReadyForExpiredEmail(agentApplication.started) shouldBe false
              case GrsDataReceived => isReadyForExpiredEmail(agentApplication.grsDataReceived) shouldBe false
              case SentForRisking => isReadyForExpiredEmail(agentApplication.sentForRisking) shouldBe false
              case SentToMinerva => isReadyForExpiredEmail(agentApplication.sentToMinerva) shouldBe false
              case RiskingCompleted => isReadyForExpiredEmail(agentApplication.riskingCompleted) shouldBe false
              case Expired => isReadyForExpiredEmail(agentApplication.expired) shouldBe true

      "findReadyForExpiredEmail does not find an application whose grace period has ended" in:
        val dueForDeletion: AgentApplication = agentApplication.expired.makeDueForDeletion
        dueForDeletion.shouldHaveGracePeriodEndInThePast
        repo.upsert(dueForDeletion).futureValue

        repo.findReadyForExpiredEmail(tdAll.now).futureValue shouldBe empty

      "findReadyForExpiredEmail does not find an application already sent the expired email" in:
        val expiredEmailSent: AgentApplication = agentApplication.started.makeOverdue.makeExpiredEmailSent
        expiredEmailSent.expiredEmailSentAt shouldBe defined withClue "precondition: the expired email must already be recorded"
        expiredEmailSent.shouldHaveGracePeriodEndInTheFuture
        repo.upsert(expiredEmailSent).futureValue

        repo.findReadyForExpiredEmail(tdAll.now).futureValue shouldBe empty

      "findReadyForDeletionEmail finds only Expired applications whose grace period has ended" in:
        def isReadyForDeletionEmail(agentApplication: AgentApplication): Boolean =
          dropDatabase()
          repo.upsert(agentApplication).futureValue
          repo.findReadyForDeletionEmail(tdAll.now).futureValue.contains(agentApplication)

        ApplicationState.values.foreach: state =>
          withClue(s"state=$state: "):
            state match
              case Started => isReadyForDeletionEmail(agentApplication.started) shouldBe false
              case GrsDataReceived => isReadyForDeletionEmail(agentApplication.grsDataReceived) shouldBe false
              case SentForRisking => isReadyForDeletionEmail(agentApplication.sentForRisking) shouldBe false
              case SentToMinerva => isReadyForDeletionEmail(agentApplication.sentToMinerva) shouldBe false
              case RiskingCompleted => isReadyForDeletionEmail(agentApplication.riskingCompleted) shouldBe false
              case Expired => isReadyForDeletionEmail(agentApplication.expired.makeDueForDeletion) shouldBe true

      "findReadyForDeletionEmail does not find an application still in the grace period" in:
        agentApplication.expired.shouldHaveGracePeriodEndInTheFuture
        repo.upsert(agentApplication.expired).futureValue

        repo.findReadyForDeletionEmail(tdAll.now).futureValue shouldBe empty

      "findReadyForDeletionEmail does not find an application already sent the deletion email" in:
        val deletionEmailSent: AgentApplication = agentApplication.expired.makeDeletionEmailSent
        deletionEmailSent.deletionEmailSentAt shouldBe defined withClue "precondition: the deletion email must already be recorded"
        deletionEmailSent.shouldHaveGracePeriodEndInThePast
        repo.upsert(deletionEmailSent).futureValue

        repo.findReadyForDeletionEmail(tdAll.now).futureValue shouldBe empty

      "findDueForDeletion finds only Expired applications whose grace period has ended" in:
        def isDueForDeletion(agentApplication: AgentApplication): Boolean =
          dropDatabase()
          repo.upsert(agentApplication).futureValue
          repo.findDueForDeletion(tdAll.now).futureValue.contains(agentApplication)

        ApplicationState.values.foreach: state =>
          withClue(s"state=$state: "):
            state match
              case Started => isDueForDeletion(agentApplication.started) shouldBe false
              case GrsDataReceived => isDueForDeletion(agentApplication.grsDataReceived) shouldBe false
              case SentForRisking => isDueForDeletion(agentApplication.sentForRisking) shouldBe false
              case SentToMinerva => isDueForDeletion(agentApplication.sentToMinerva) shouldBe false
              case RiskingCompleted => isDueForDeletion(agentApplication.riskingCompleted) shouldBe false
              case Expired => isDueForDeletion(agentApplication.expired.makeDueForDeletion) shouldBe true

      "findDueForDeletion does not find an application still in the grace period" in:
        agentApplication.expired.shouldHaveGracePeriodEndInTheFuture
        repo.upsert(agentApplication.expired).futureValue

        repo.findDueForDeletion(tdAll.now).futureValue shouldBe empty
    }

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

  private def expiryReminderPeriodEndsAt: Instant = tdAll.now.plus(tdAll.expiryReminderPeriod)
