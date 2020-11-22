package chatbot

import com.google.inject.Guice
import com.twitter.inject.{Injector, IntegrationTest}
import xed.api_service.Server
import xed.api_service.module._
import xed.api_service.util.Implicits.FutureEnhance
import xed.chatbot.domain.challenge.{ChallengeTemplate, ChallengeType}
import xed.chatbot.service.simulator.ChatFlowSimulator
import xed.chatbot.service.{BotService, ChallengeService, ChallengeTemplateService}
import xed.userprofile.AuthenService

import scala.concurrent.duration.DurationLong

class ChallengeFlowTest extends IntegrationTest {

  override protected def injector: Injector = Injector(Guice.createInjector(
    Server.overrideModule(XedApiModule,
      XedApiModuleTestImpl,
      PublicPathConfigModule,
      BotServiceModule,
      BotProcessorModule,
      ChallengeModule,
      HandlerModule)
  ))

  val username = "up-be198039-0170-4da5-a17f-50a1c05c4d67"

  val templateService = injector.instance[ChallengeTemplateService]
  val challengeService = injector.instance[ChallengeService]

  val chatFlowSimulator = ChatFlowSimulator(
    isDisplayConversation = true,
    botService = injector.instance[BotService],
    authenService = injector.instance[AuthenService]
  )


  val questionIds = Seq(
    "phrasal_verb_all_account_account-for_phrasal-verb",
    "phrasal_verb_all_act_act-as_phrasal-verb",
    "phrasal_verb_all_act_act-on_phrasal-verb",
    "phrasal_verb_all_act_act-up_phrasal-verb",
    "phrasal_verb_all_add_add-to_phrasal-verb",
    "phrasal_verb_all_add_add-up_phrasal-verb",
    "phrasal_verb_all_add_add-up-to_phrasal-verb",
    "phrasal_verb_all_aim_aim-at_phrasal-verb",
    "phrasal_verb_all_allow_allow-for_phrasal-verb",
    "phrasal_verb_all_amount_amount-to_phrasal-verb",
    "phrasal_verb_all_answer_answer-back_phrasal-verb",
    "phrasal_verb_all_appeal_appeal-for_phrasal-verb",
    "phrasal_verb_all_appeal_appeal-to_phrasal-verb",
    "phrasal_verb_all_apply_apply-to_phrasal-verb",
    "phrasal_verb_all_arrive_arrive-at_phrasal-verb",
    "phrasal_verb_all_ask_ask-after_phrasal-verb",
    "phrasal_verb_all_ask_ask-for_phrasal-verb",
    "phrasal_verb_all_ask_ask-out_phrasal-verb",
    "phrasal_verb_all_ask_ask-over_phrasal-verb",
    "phrasal_verb_all_attach_attach-to_phrasal-verb",
    "phrasal_verb_all_attend_attend-to_phrasal-verb",
    "phrasal_verb_all_average_average-out_phrasal-verb"
  )

  test("Create Challenge Phrasal Verb A") {
    val templateId = "phrasal_verb_a_streaking_7days"

    val questionListId = challengeService.setQuestionList(templateId, questionIds).sync()

    val template = ChallengeTemplate(
      templateId,
      challengeType = ChallengeType.STREAKING,
      name = "Phrasal Verb Master: A 7Days From Now",
      description = Some("Check your knowledge about Phrasal Verb: A"),
      canExpired = true,
      questionListId = questionListId,
      duration = Some(7.day.toMillis)
    )

    val challengeTemplate = templateService.addTemplate(template).sync()
    val templates = templateService.searchChallengeTemplates(0,500).sync()

    println(challengeTemplate)
    println(s"Total templates: ${templates.total}")
    templates.records.foreach(template =>{
      println(template)
    })

  }

  test("Join Challenge") {

  }


}
