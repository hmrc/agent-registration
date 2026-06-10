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

package uk.gov.hmrc.agentregistration.connectors

import uk.gov.hmrc.agentregistration.util.RequestAwareLogging

trait Connector
extends RequestAwareLogging:

  // java:
  export java.net.URL

  // scala:
  export scala.concurrent.ExecutionContext
  export scala.concurrent.Future

  // play:
  export play.api.http.HeaderNames
  export play.api.http.Status
  export play.api.libs.json.Json
  export play.api.libs.json.JsValue
  export play.api.libs.json.Writes
  export play.api.libs.json.OWrites
  export play.api.libs.json.Reads
  export play.api.libs.json.Format
  export play.api.libs.json.OFormat

  export play.api.libs.json.__
  export play.api.mvc.Result
  export play.api.mvc.Call
  export play.api.mvc.Request
  export play.api.mvc.RequestHeader
  export play.api.mvc.AnyContent
  export play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
  // other libs:
  export sttp.model.Uri

  // bootstrap:
  export uk.gov.hmrc.http.HttpResponse
  export uk.gov.hmrc.http.StringContextOps
  export uk.gov.hmrc.http.HttpReads.Implicits.*
  export uk.gov.hmrc.http.HttpErrorFunctions.is2xx

  // this project:
  export uk.gov.hmrc.agentregistration.util.RequestSupport.given
