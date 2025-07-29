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

package uk.gov.hmrc.agentregistration

/** IMPORTANT: Shared Code Package
  *
  * This package contains models and code that are shared between the agent-registration and agent-registration-frontend microservices. Follow these guidelines:
  *
  *   1. Only place code here that needs to be shared between both services
  *   2. All shared code MUST be defined in the agent-registration service
  *   3. Code is copied from agent-registration to agent-registration-frontend
  *   4. DO NOT modify this code in the frontend service
  *   5. Any changes must be made in agent-registration and then copied to frontend
  *
  * Note: We share code by copying rather than using libraries because creating shared libraries is not permitted in the MDTP platform.
  */
package object shared
