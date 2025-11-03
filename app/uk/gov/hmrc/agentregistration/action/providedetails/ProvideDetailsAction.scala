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

package uk.gov.hmrc.agentregistration.action.providedetails

import com.google.inject.{Inject, Singleton}
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.util.RequestAwareLogging
import uk.gov.hmrc.agentregistration.util.RequestSupport.hc
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import scala.concurrent.{ExecutionContext, Future}

class ProvideDetailsRequest[A](
  val internalUserId: InternalUserId,
  val request: Request[A]
)
extends WrappedRequest[A](request)

@Singleton
class ProvideDetailsAction @Inject() (
  af: AuthorisedFunctions,
  appConfig: AppConfig,
  cc: MessagesControllerComponents
)
extends ActionRefiner[Request, ProvideDetailsRequest]
with RequestAwareLogging:

  override protected def refine[A](request: Request[A]): Future[Either[Result, ProvideDetailsRequest[A]]] =
    given r: Request[A] = request

    af.authorised(
      AuthProviders(GovernmentGateway) and AffinityGroup.Individual
    ).retrieve(
      Retrievals.allEnrolments
        and Retrievals.affinityGroup
        and Retrievals.internalId
    ).apply:
      case allEnrolments ~ affinityGroup ~ maybeInternalId =>
        if isUnsupportedAffinityGroup(affinityGroup) then
          Future.failed(UnsupportedAffinityGroup(s"UnsupportedAffinityGroup: $affinityGroup"))
        else
          Future.successful(Right(new ProvideDetailsRequest(
            internalUserId = maybeInternalId
              .map(InternalUserId.apply)
              .getOrElse(throw RuntimeException("Retrievals for internalId is missing")),
            request = request
          )))

  private given ExecutionContext = cc.executionContext
  override protected def executionContext: ExecutionContext = cc.executionContext

  private def isUnsupportedAffinityGroup[A](maybeAffinityGroup: Option[AffinityGroup])(using request: RequestHeader) =
    val supportedAffinityGroups: Set[AffinityGroup] = Set(AffinityGroup.Individual)
    val affinityGroup: AffinityGroup = maybeAffinityGroup.getOrElse(throw RuntimeException("Retrievals for AffinityGroup is missing"))
    !supportedAffinityGroups.contains(affinityGroup)

