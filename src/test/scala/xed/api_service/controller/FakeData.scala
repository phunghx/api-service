package xed.api_service.controller

import xed.api_service.domain._
import xed.api_service.domain.design.CardVersion
import xed.api_service.domain.design.v100.{Panel, Text}
import xed.api_service.domain.request._


object FakeData {
  def generateCardRequest(): Seq[CardRequest] = {
    Seq(
      CardRequest(CardVersion.V100,
        design = Some(CardDesign(
          fronts = Seq(Panel(components = Seq(Text("hello")))),
          back = Some(Panel(components = Seq(Text("xin chao"))))
        )))
    )
  }
}