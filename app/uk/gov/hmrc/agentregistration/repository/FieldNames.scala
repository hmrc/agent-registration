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

object FieldNames:

  val _id: String = "_id"
  val lastUpdated: String = "lastUpdated"

  // agent-application collection
  val applicationState: String = "applicationState"
  val internalUserId: String = "internalUserId"
  val linkId: String = "linkId"
  val applicationReference: String = "applicationReference"

  // individual collection
  val agentApplicationId: String = "agentApplicationId"
  val providedDetailsState: String = "providedDetailsState"
  val personReference: String = "personReference"

  val individuals: String = "individuals"

  // application-scheduler collection
  val applicationReadyToSubmitEmailStatus: String = "applicationReadyToSubmitEmailStatus"

  // aliases for $lookup result arrays from agent-application aggregations
  val applicationSchedulerLookup: String = "applicationSchedulerLookup"
  val applicationSchedulerLookupReadyToSubmitEmailStatus: String = s"$applicationSchedulerLookup.0.$applicationReadyToSubmitEmailStatus"
