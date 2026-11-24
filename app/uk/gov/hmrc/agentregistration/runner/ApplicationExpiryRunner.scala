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

package uk.gov.hmrc.agentregistration.runner

import uk.gov.hmrc.agentregistration.services.ApplicationExpiryService
import uk.gov.hmrc.agentregistration.services.email.EmailServiceForExpiredApplications
import uk.gov.hmrc.agentregistration.services.email.EmailServiceForExpiryReminders

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplicationExpiryRunner @Inject() (
  applicationExpiryService: ApplicationExpiryService,
  emailServiceForExpiryReminders: EmailServiceForExpiryReminders,
  emailServiceForExpiredApplications: EmailServiceForExpiredApplications
)(using ec: ExecutionContext):

  val stages: Seq[Stage] = Seq(
    Stage(name = "ApplicationExpiryService.processExpiry", run = applicationExpiryService.processExpiry()),
    Stage(name = "EmailServiceForExpiryReminders.processEmails", run = emailServiceForExpiryReminders.processEmails()),
    Stage(name = "EmailServiceForExpiredApplications.processEmails", run = emailServiceForExpiredApplications.processEmails())
  )

  def run(): Future[Unit] = RunnerHelper.run(runnerName = "ApplicationExpiryRunner", stages = stages)
