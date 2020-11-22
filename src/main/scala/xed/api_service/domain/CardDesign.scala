package xed.api_service.domain

import xed.api_service.domain.design.Container

case class CardDesign(fronts: Seq[Container],
                      back: Option[Container]) {
  def isQuestionAnswerCard() = {
    Option(fronts).getOrElse(Seq.empty)
      .filter(_.hasAction())
      .nonEmpty
  }
}
