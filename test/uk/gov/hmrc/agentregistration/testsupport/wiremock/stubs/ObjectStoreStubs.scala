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

package uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistration.testsupport.wiremock.StubMaker

object ObjectStoreStubs:

  def stubListAmlsEvidence(amlsEvidence: AmlsEvidence): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlEqualTo(s"/object-store/list/agent-registration-frontend/${amlsEvidence.fileUploadReference.value}"),
    responseStatus = Status.OK,
    responseBody =
      Json.obj(
        "objectSummaries" -> Json.arr(
          Json.obj(
            "location" -> s"/object-store/object/agent-registration-frontend/${amlsEvidence.fileUploadReference.value}/${amlsEvidence.fileName}",
            "contentLength" -> 1234,
            "lastModified" -> TdAll.tdAll.nowAsInstant
          )
        )
      ).toString
  )

  def stubListAmlsEvidenceEmpty(amlsEvidence: AmlsEvidence): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlEqualTo(s"/object-store/list/agent-registration-frontend/${amlsEvidence.fileUploadReference.value}"),
    responseStatus = Status.OK,
    responseBody = Json.obj("objectSummaries" -> Json.arr()).toString
  )

  def stubDeleteAmlsEvidence(
    amlsEvidence: AmlsEvidence,
    responseStatus: Int = Status.NO_CONTENT
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.DELETE,
    urlPattern = wm.urlEqualTo(s"/object-store/object/agent-registration-frontend/${amlsEvidence.fileUploadReference.value}/${amlsEvidence.fileName}"),
    responseStatus = responseStatus
  )

  def verifyDeleteAmlsEvidence(
    amlsEvidence: AmlsEvidence,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.DELETE,
    urlPattern = wm.urlEqualTo(s"/object-store/object/agent-registration-frontend/${amlsEvidence.fileUploadReference.value}/${amlsEvidence.fileName}"),
    count = count
  )
