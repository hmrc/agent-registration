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

package uk.gov.hmrc.agentregistration.action

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.Results.*
import play.api.mvc.*
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.controllers.routes
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import uk.gov.hmrc.agentregistration.util.RequestSupport.hc

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AuthorisedRequest[A](
  val internalUserId: InternalUserId,
  val request: Request[A]
)
extends WrappedRequest[A](request)

@Singleton
class AuthorisedAction @Inject() (
  af: AuthorisedFunctions,
  appConfig: AppConfig,
  cc: MessagesControllerComponents
)
extends ActionRefiner[Request, AuthorisedRequest]
with RequestAwareLogging:

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] =
    given r: Request[A] = request

    af.authorised(
      AuthProviders(GovernmentGateway) and AffinityGroup.Agent
    ).retrieve(
      Retrievals.allEnrolments
        and Retrievals.credentialRole
        and Retrievals.internalId
    ).apply:
      case allEnrolments ~ credentialRole ~ maybeInternalId =>
        if isUnsupportedCredentialRole(credentialRole) then
          Future.failed(UnsupportedCredentialRole(s"UnsupportedCredentialRole: $credentialRole"))
        else if isHmrcAsAgentEnrolmentAssignedToUser(allEnrolments) then
          Future.failed(AuthorisationException.fromString(s"Enrolment ${appConfig.hmrcAsAgentEnrolment} is assigned to user"))
        else
          Future.successful(Right(new AuthorisedRequest(
            internalUserId = maybeInternalId
              .map(InternalUserId.apply)
              .getOrElse(throw RuntimeException("Retrievals for internalId is missing")),
            request = request
          )))

  private given ExecutionContext = cc.executionContext
  override protected def executionContext: ExecutionContext = cc.executionContext

  private def isHmrcAsAgentEnrolmentAssignedToUser[A](allEnrolments: Enrolments) = allEnrolments
    .getEnrolment(appConfig.hmrcAsAgentEnrolment.key)
    .exists(_.isActivated)

  private def isUnsupportedCredentialRole[A](maybeCredentialRole: Option[CredentialRole])(using request: RequestHeader) =
    @nowarn
    val supportedCredentialRoles: Set[CredentialRole] = Set(User, Admin)
    val credentialRole: CredentialRole = maybeCredentialRole.getOrElse(throw RuntimeException("Retrievals for CredentialRole is missing"))
    !supportedCredentialRoles.contains(credentialRole)
